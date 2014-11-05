package com.alarme.core.io.impl;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.alarme.core.conf.ConfigRepository;
import com.alarme.core.conf.DataRepository;
import com.alarme.core.io.ELed;
import com.alarme.core.io.IIoManager;
import com.alarme.service.MessageQueue;
import com.alarme.service.Signal;
import com.alarme.service.MessageQueue.EMedia;


/**
 * Development IoManager implementation (uses System.in as keyboard input)
 * 
 * @author ffradet
 * 
 */
public class IoManagerEmul implements IIoManager {

	private static final Logger log = Logger.getLogger(IoManagerEmul.class);
	
	public static final int	RUN_FREQ			= 4;				// hz
	public static final int	RUN_PERIOD_DURATION	= 1000 / RUN_FREQ;	// ms per cycle
	
	private String				keyboardInput		= "";

	private List<ELed>			outputs				= new ArrayList<ELed>();
	private boolean				alarm				= false;

	private String					passwordRequestedEmail = null;
	private String					passwordRequestedCode = null;
	
	@Override
	public String computePasswordRequestedCode(String sender) {
		passwordRequestedEmail = sender;
		passwordRequestedCode = "*" + String
				.valueOf((int) (Math.random() * 9000 + 1000)) + "#";
		log.debug("computePasswordRequestedCode : " + passwordRequestedCode + " from " + passwordRequestedEmail);
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
		return false;
	}
	
	
	@Override
	public boolean isOpenDoor() {
		return keyboardInput.equals("d");
	}


	@Override
	public boolean isOpenWin() {
		return keyboardInput.equals("w");
	}


	@Override
	public boolean isInputCode() {
		DataRepository data = DataRepository.getInstance();
		return keyboardInput.equals(data.getCode());
	}


	@Override
	public boolean isAlimSecteur() {
		return !keyboardInput.equals("a");
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
	public void setLed(boolean enable, ELed led) {
		//
		if (enable) {
			if (!outputs.contains(led)) {
				outputs.add(led);
			}
		} else {
			outputs.remove(led);
		}
	}


	@Override
	public void setAlarm(boolean enable) {
		DataRepository data = DataRepository.getInstance();
		data.setAlarmEnabled(enable);
		alarm = enable;
	}


	@Override
	public void refreshInputs() {
		keyboardInput = "";
		//
		try {
			int nb = System.in.available();
			//
			if (nb > 0) {
				byte[] buf = new byte[nb + 10];
				nb = System.in.read(buf);

				keyboardInput = new String(buf, Charset.forName("UTF-8"));
				// Remove 0x0D 0x0A
				if (nb >= 2) {
					keyboardInput = keyboardInput.substring(0, nb - 2);
				}

				log.debug("keyboardInput = " + keyboardInput);
			}
		}
		catch (Exception e) {
			//
		}
		if (managePasswordRequestedCode()) {
			keyboardInput = "";
		}
		log.debug("leds : " + outputs + " ; alarm : " + alarm);
	}


	@Override
	public void stop() {

	}


	@Override
	public int getSensorCount() {
		return 4;
	}


	/**
	 * 
	 * @param index
	 * @return
	 */
	@Override
	public boolean getSensor(int index) {
		return true;
	}


	@Override
	public void setAudioAmpli(boolean enable) {
		log.debug("Audio ampli set to " + enable);
	}


	@Override
	public long getRunPeriodDuration() {
		return RUN_PERIOD_DURATION;
	}
}
