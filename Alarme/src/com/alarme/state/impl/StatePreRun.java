package com.alarme.state.impl;

import org.apache.log4j.Logger;

import com.alarme.core.conf.DataRepository;
import com.alarme.core.io.ELed;
import com.alarme.core.io.IIoManager;
import com.alarme.service.Signal;
import com.alarme.state.AbstractState;
import com.alarme.state.factory.EState;


/**
 * 
 * @author ffradet
 * 
 */
public class StatePreRun extends AbstractState {

	private static final Logger log = Logger.getLogger(StatePreRun.class);
	
	private static final long	STATE_DURATION 			= 5 * 60 * 1000; // 5min in ms
//	private static final long	STATE_DURATION			= 15 * 1000; // 15s in ms (dev)
	private static final long	BLINK_HIGH_DURATION		= 500;		// ms
	private static final long	BLINK_PERIOD_DURATION	= 1000;	// ms

	private boolean openDoor = false;
	private boolean openWin = false;
	private Signal openWinSignal;

	@Override
	public void init() {
		super.init();

		DataRepository.getInstance().setAlarmEnabled(true);
		
		openWinSignal = Signal.BIPBIP_1320;
		
		addState(EState.DISPLAY_OPEN);
	}


	@Override
	public void stop() {
		super.stop();
		openWinSignal.stop();
		getIoManager().setLed(false, ELed.GREEN);
		
		removeState(EState.DISPLAY_OPEN);
	}


	@Override
	public void run() {
		IIoManager ioManager = getIoManager();
		//
		if (ioManager.isInputCode()) {
			switchStateTo(EState.WAIT);
		} else {
			long timeInPeriod = System.currentTimeMillis() % BLINK_PERIOD_DURATION;
			boolean bLed = (timeInPeriod < BLINK_HIGH_DURATION);
			// blink green light
			ioManager.setLed(bLed, ELed.GREEN);
			//
			if ((ioManager.isOpenWin()) && (!openWin)) {
				openWin = true;
				openWinSignal.loop();
				log.debug("A window is open");
			}
			if ((!ioManager.isOpenWin()) && (openWin)) {
				openWin = false;
				openWinSignal.stop();
				log.debug("All windows are closed");
			}
			//
			if ((ioManager.isOpenDoor()) && (!openDoor)) {
				openDoor = true;
				log.debug("A door is open");
			}
			//
			if ((!ioManager.isOpenDoor()) && (openDoor)) {
				openDoor = false;
				log.debug("All doors are closed");
			}
			//
			if ((ioManager.isOpenDoor()) || (ioManager.isOpenWin())) {
				resetTime();
			}
			else
			if (getTime() >= STATE_DURATION) {
				switchStateTo(EState.RUN);
			}
		}
	}

}
