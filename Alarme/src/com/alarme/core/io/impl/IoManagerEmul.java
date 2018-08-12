package com.alarme.core.io.impl;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.alarme.core.conf.RecipientInfo;
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
	private String				keyboardBuffer		= "";
	private char				key					= 0;
    private boolean             bAllDown            = false;
	private boolean             bAlim               = true;
	private boolean             bOpenDoor           = false;
	private boolean             bOpenWin            = false;


	private List<ELed>			outputs				= new ArrayList<ELed>();
	private boolean				alarm				= false;

	private int _hash = 0;

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
	 * @return
	 */
	private boolean managePasswordRequestedCode() {

		if ((passwordRequestedCode != null)
				&& (keyboardInput.equals(passwordRequestedCode))) {
			Signal.BIPBIP_1320.start();
			Properties props = ConfigRepository.getInstance().getProperties();
			MessageQueue.getInstance().createAndPushMessageTo(
					Arrays.asList(new RecipientInfo(passwordRequestedEmail, null, null)), "RE : PWD",
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
		return bAllDown;
	}


	@Override
	public boolean isOpenDoor() {
		return bOpenDoor;
	}


	@Override
	public boolean isOpenWin() {
		return bOpenWin;
	}


	@Override
	public boolean isInputCode() {
		DataRepository data = DataRepository.getInstance();
		return keyboardInput.equals(data.getCode());
	}


	@Override
	public boolean isAlimSecteur() {
		return bAlim;
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
		key = 0;
		//
		try {
			int nb = System.in.available();
			//
			if (nb > 0) {
				byte[] buf = new byte[nb + 10];
				nb = System.in.read(buf);

				String s = new String(buf, Charset.forName("UTF-8"));
				// Take only the first char
				// User must hit "Enter" key after each char entered
				if (nb >= 2) {
					key = s.charAt(0);
				}
			}
		}
		catch (Exception e) {
			//
		}

		if (key != 0) {

            /*
              Emul special commands
            */

            if (key == 's') {
                bAlim = !bAlim;
            }
            else
            if (key == 'a') {
                bAllDown = !bAllDown;
            }
            else
            if (key == 'd') {
                bOpenDoor = !bOpenDoor;
            }
            else
            if (key == 'w') {
                bOpenWin = !bOpenWin;
            }
            else {
                keyboardBuffer += key;
                boolean invalidKey = DataRepository.getInstance().isAlarmEnabled();
                invalidKey &= ((key == '*') || (key == '#'));
                // "*" and "#" keys are disabled when alarm mode is enabled
                if (!invalidKey) {
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
                } else {
                    keyboardBuffer = "";
                }
            }
		}

		if (managePasswordRequestedCode()) {
			keyboardInput = "";
		}
		int hash = outputs.hashCode() + Boolean.valueOf(alarm).hashCode() + Character.valueOf(key).hashCode() + keyboardInput.hashCode() + keyboardBuffer.hashCode();
		if (hash != _hash) {
			_hash = hash;
			log.debug("leds : " + outputs + " ; flags : [" + (alarm ? "B" : "b") + (bAllDown ? "A" : "a") + (bAlim ? "S" : "s") + (bOpenDoor ? "D" : "d") + (bOpenWin ? "W" : "w") + " ; key = " + key + " ; keyboardInput = " + keyboardInput + " ; keyboardBuffer = " + keyboardBuffer);
		}
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
