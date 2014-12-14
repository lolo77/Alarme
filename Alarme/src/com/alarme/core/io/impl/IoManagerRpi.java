package com.alarme.core.io.impl;

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.alarme.core.conf.ConfigRepository;
import com.alarme.core.conf.DataRepository;
import com.alarme.core.conf.Sensor;
import com.alarme.core.conf.Sensor.EType;
import com.alarme.core.conf.SensorRepository;
import com.alarme.core.io.ELed;
import com.alarme.core.io.IIoManager;
import com.alarme.service.MessageQueue;
import com.alarme.service.Signal;
import com.alarme.service.MessageQueue.EMedia;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;

/**
 * Raspberry Pi IoManager implementation
 * 
 * @author ffradet
 * 
 */
public class IoManagerRpi implements IIoManager {

	private static final Logger log = Logger.getLogger(IoManagerRpi.class);

	public static final int RUN_FREQ = 20; // hz
	public static final int RUN_PERIOD_DURATION = 1000 / RUN_FREQ; // ms per
																	// cycle

	private static final long DELAY_READ = 1; // millisec
	private static final char KEY_NONE = '.';

	private GpioPinDigitalInput in[] = new GpioPinDigitalInput[4];
	private GpioPinDigitalOutput out[] = new GpioPinDigitalOutput[9];

	private char key = KEY_NONE;
	private char oldKey = KEY_NONE;
	private char oldKey2 = KEY_NONE;
	private String keyboardInput = "";
	private String keyboardBuffer = "";

	private boolean bAlimSecteur;
	private boolean bAlarm = false;
	private boolean sensor[] = new boolean[12];
	private boolean led[] = new boolean[3];

	private String passwordRequestedEmail = null;
	private String passwordRequestedCode = null;

	/**
	 * 
	 */
	public IoManagerRpi() {
		final GpioController gpio = GpioFactory.getInstance();
		// @formatter:off
		in[0] = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00); // /I0 -> Pin
																	// 11
		in[1] = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02); // /I1 -> Pin
																	// 13
		in[2] = gpio.provisionDigitalInputPin(RaspiPin.GPIO_03); // /I2 -> Pin
																	// 15
		in[3] = gpio.provisionDigitalInputPin(RaspiPin.GPIO_05); // ALIM -> Pin
																	// 18

		gpio.setPullResistance(PinPullResistance.PULL_UP, in); // I0..2 are down
																// when a key is
																// pressed or a
																// sensor
																// detects an
																// opening (due
																// to demux
																// 74HC138)
		gpio.setPullResistance(PinPullResistance.PULL_DOWN, in[3]); // ALIM is
																	// up when
																	// ok

		out[0] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_08); // A0 -> Pin
																	// 3
		out[1] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_09); // A1 -> Pin
																	// 5
		out[2] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07); // A2 -> Pin
																	// 7
		out[3] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04); // ACK -> Pin
																	// 16
		out[4] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06); // LED0 ->
																	// Pin 22
		out[5] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_10); // LED1 ->
																	// Pin 24
		out[6] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_11); // LED2 ->
																	// Pin 26
		out[7] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01); // ALARM ->
																	// Pin 12
		out[8] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_12); // AMPLI ->
																	// Pin 19
		// @formatter:on
		gpio.setState(false, out[3]);
		//
		for (int i = 0; i < led.length; i++) {
			led[i] = false;
		}
		// First init
		refreshInputs();
	}

	@Override
	public String computePasswordRequestedCode(String sender) {
		passwordRequestedEmail = sender;
		passwordRequestedCode = "*"
				+ String.valueOf((int) (Math.random() * 9000 + 1000)) + "#";
		log.debug("computePasswordRequestedCode : " + passwordRequestedCode
				+ " from " + passwordRequestedEmail);
		return passwordRequestedCode;
	}

	/**
	 * 
	 * @param input
	 * @return
	 */
	private boolean managePasswordRequestedCode() {

		if ((passwordRequestedCode != null)
				&& (keyboardInput.equals(passwordRequestedCode))) {
			Signal.BIPBIP_1320.start();
			Properties props = ConfigRepository.getInstance().getProperties();
			MessageQueue.getInstance().createAndPushMessageTo(
					passwordRequestedEmail, "RE : PWD",
					props.getProperty(ConfigRepository.KEY_MAIL_PASS),
					EMedia.EMAIL);
			passwordRequestedCode = null;
			passwordRequestedEmail = null;
			return true;
		}

		return false;
	}

	@Override
	public boolean isAllDown() {
		return ((!bAlimSecteur) && (isOpenDoor()) && (isOpenWin()));
	}

	/**
	 * Acknowledge demux 74HC138
	 * 
	 * @param enable
	 */
	private void ack(boolean enable) {
		final GpioController gpio = GpioFactory.getInstance();

		// Enable/Disable DEMUX
		gpio.setState(enable, out[3]);

		try {
			Thread.sleep(DELAY_READ);
		} catch (InterruptedException e) {
		}

	}

	@Override
	public void refreshInputs() {
		final GpioController gpio = GpioFactory.getInstance();

		// Input alim source
		bAlimSecteur = gpio.getState(in[3]).isHigh();

		// Reset keyboard
		oldKey2 = oldKey;
		oldKey = key;
		key = KEY_NONE;
		keyboardInput = "";

		// Scan DEMUX 74HC138 (3 bits to 8 pins)
		for (int adr = 0; adr <= 7; adr++) {

			// Disable DEMUX
			ack(false);

			// Output DEMUX Address lines
			gpio.setState((adr & 1) != 0, out[0]);
			gpio.setState((adr & 2) != 0, out[1]);
			gpio.setState((adr & 4) != 0, out[2]);

			// Enable DEMUX
			ack(true);

			boolean bI0 = false;
			boolean bI1 = false;
			boolean bI2 = false;

			int iInputs = -1;
			int iFlags = 0;
			// Read stabilized inputs to avoid any electric interference
			// (induced currents on thunderstorm weather)
			while (iInputs != iFlags) {
				iInputs = iFlags;
				bI0 = gpio.getState(in[0]).isLow();
				bI1 = gpio.getState(in[1]).isLow();
				bI2 = gpio.getState(in[2]).isLow();

				iFlags = (bI0 ? 1 : 0) + (bI1 ? 2 : 0) + (bI2 ? 4 : 0);
				try {
					Thread.sleep(DELAY_READ);
				} catch (InterruptedException e) {
				}
			}

			switch (adr) {
			case 0:
				if (bI0) {
					key = '1';
				}
				if (bI1) {
					key = '2';
				}
				if (bI2) {
					key = '3';
				}
				break;
			case 1:
				if (bI0) {
					key = '4';
				}
				if (bI1) {
					key = '5';
				}
				if (bI2) {
					key = '6';
				}
				break;
			case 2:
				if (bI0) {
					key = '7';
				}
				if (bI1) {
					key = '8';
				}
				if (bI2) {
					key = '9';
				}
				break;
			case 3:
				if (bI0) {
					key = '*';
				}
				if (bI1) {
					key = '0';
				}
				if (bI2) {
					key = '#';
				}
				break;
			case 4:
				sensor[0] = bI0;
				sensor[1] = bI1;
				sensor[2] = bI2;
				break;
			case 5:
				sensor[3] = bI0;
				sensor[4] = bI1;
				sensor[5] = bI2;
				break;
			case 6:
				sensor[6] = bI0;
				sensor[7] = bI1;
				sensor[8] = bI2;
				break;
			case 7:
				sensor[9] = bI0;
				sensor[10] = bI1;
				sensor[11] = bI2;
				break;
			}
		}

		// log.debug("key = " + key + " oldKey = " + oldKey + " oldKey2 = " +
		// oldKey2);

		// Read keyboard twice before taking data in account to avoid false
		// positives.
		if ((oldKey2 == KEY_NONE) && (oldKey == key) && (key != KEY_NONE)) {

			Signal.BIP_KB.start();

			keyboardBuffer += key;
			// '#' terminates a user input string
			if (key == '#') {
				keyboardInput = keyboardBuffer;
				keyboardBuffer = "";
			} else if (key == '*') { // '*' erases the user input string
				keyboardBuffer = "" + key;
			} else {
				// The secret code is entered with no leading '*' and no
				// trailing '#'
				if (keyboardBuffer.charAt(0) != '*') {
					String code = DataRepository.getInstance().getCode();
					//
					if (code.equals(keyboardBuffer)) {
						keyboardInput = keyboardBuffer;
					} else {
						// Wrong sequence, empty buffer to allow the user to
						// input again
						if (!code.startsWith(keyboardBuffer)) {
							keyboardBuffer = "";
						}
					}
				}
			}

			log.debug("keyboardBuffer = " + keyboardBuffer
					+ ", keyboardInput = " + keyboardInput);

			if (managePasswordRequestedCode()) {
				keyboardInput = "";
			}
		}

	}

	@Override
	public boolean isAlimSecteur() {
		return bAlimSecteur;
	}

	@Override
	public int getSensorCount() {
		return 12;
	}

	@Override
	public boolean getSensor(int index) {
		//
		if ((index < 0) || (index >= sensor.length)) {
			return false;
		}
		return !sensor[index];
	}

	@Override
	public void stop() {

	}

	@Override
	public boolean isOpenDoor() {

		SensorRepository repo = SensorRepository.getInstance();
		List<Sensor> lst = repo.getByType(EType.ENTRY);

		boolean res = false;
		//
		for (Sensor s : lst) {
			res |= getSensor(s.getPort());
		}

		return res;
	}

	@Override
	public boolean isOpenWin() {
		SensorRepository repo = SensorRepository.getInstance();
		List<Sensor> lst = repo.getByType(EType.WINDOW);

		boolean res = false;
		//
		for (Sensor s : lst) {
			res |= getSensor(s.getPort());
		}

		return res;
	}

	@Override
	public boolean isInputCode() {
		String code = DataRepository.getInstance().getCode();
		return keyboardInput.equals(code);
	}

	@Override
	public boolean changeCode(String newCode) {
		DataRepository data = DataRepository.getInstance();
		data.setCode(newCode);
		return data.save();
	}

	@Override
	public String getKeyboardInput() {
		return keyboardInput;
	}

	@Override
	public void setLed(boolean status, ELed idx) {
		int index = idx.getIndex();
		//
		if ((index < 0) || (index >= led.length)) {
			return;
		}
		led[index] = status;

		final GpioController gpio = GpioFactory.getInstance();

		// Output LED
		gpio.setState(led[index], out[index + 4]);
	}

	@Override
	public void setAlarm(boolean enable) {
		bAlarm = enable;
		log.info("Alarm enable : " + enable);
		final GpioController gpio = GpioFactory.getInstance();

		// Output Alarm signal (direct)
		gpio.setState(bAlarm, out[7]);
	}

	@Override
	public void setAudioAmpli(boolean enable) {
		final GpioController gpio = GpioFactory.getInstance();
		gpio.setState(enable, out[8]);
	}

	@Override
	public long getRunPeriodDuration() {
		return RUN_PERIOD_DURATION;
	}

}
