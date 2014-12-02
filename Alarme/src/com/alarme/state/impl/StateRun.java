package com.alarme.state.impl;

import org.apache.log4j.Logger;

import com.alarme.core.conf.DataRepository;
import com.alarme.core.io.ELed;
import com.alarme.core.io.IIoManager;
import com.alarme.state.AbstractState;
import com.alarme.state.factory.EState;


/**
 * 
 * @author ffradet
 * 
 */
public class StateRun extends AbstractState {

	private static final Logger log = Logger.getLogger(StateRun.class);
	
	private static final int	STATE_DURATION			= 20 * 1000;	// 20s in ms to enter the secret code before the alarm beeps
	private static final int	BLINK_HIGH_DURATION		= 500;			// ms
	private static final int	BLINK_PERIOD_DURATION	= 1000;		// ms

	private boolean				bOpenDoorOld;
	private boolean				bOpenDoor;


	@Override
	public void init() {
		super.init();
		bOpenDoorOld = false;
		bOpenDoor = false;
		IIoManager ioManager = getIoManager();
		ioManager.setLed(true, ELed.RED);

		DataRepository.getInstance().setAlarmEnabled(true);
	}


	@Override
	public void stop() {
		super.stop();
		getIoManager().setLed(false, ELed.RED);
		signal(false);
	}


	private void signal(boolean enable) {
		log.debug("signal : " + enable);
		
		// Départ / Arrêt signal sonore 10%
		if (enable) {
//			Signal.BIP.setVolume(0.1);
//			Signal.BIP.loop();
		}
		else {
//			Signal.BIP.stopThread();
		}
	}


	@Override
	public void run() {
		IIoManager ioManager = getIoManager();

		bOpenDoorOld = bOpenDoor;
		//
		if (ioManager.isOpenDoor()) {
			bOpenDoor = true;
		}

		//
		if (ioManager.isInputCode()) {
			//
			if (bOpenDoor) {
				// Disabled by entering person : capture video
				addState(EState.CAPTURE);
			}
			switchStateTo(EState.WAIT);
		} else {
			// We should not enter by the window !
			if (ioManager.isOpenWin()) {
				switchStateTo(EState.ALARM);
			} else {
				//
				if (!bOpenDoor) {
					resetTime();
				} else {
					// Front montant
					if (!bOpenDoorOld) {
						signal(true);
					}
					//
					if (getTime() < STATE_DURATION) {
						long timeInPeriod = getTime() % BLINK_PERIOD_DURATION;
						boolean bLed = (timeInPeriod < BLINK_HIGH_DURATION);
						// blink red light
						ioManager.setLed(bLed, ELed.RED);
					} else {
						switchStateTo(EState.ALARM);
					}
				}
			}
		}
	}
}
