package com.alarme.state.impl;

import com.alarme.core.conf.DataRepository;
import com.alarme.core.io.ELed;
import com.alarme.state.AbstractState;
import com.alarme.state.factory.EState;

/**
 * 
 * @author TAD
 *
 */
public class StateTest extends AbstractState {

	@Override
	public void init() {
		super.init();
		getIoManager().setLed(true, ELed.GREEN);
		getIoManager().setLed(true, ELed.ORANGE);
		getIoManager().setLed(true, ELed.RED);
	}


	@Override
	public void stop() {
		super.stop();
		getIoManager().setLed(false, ELed.GREEN);
		getIoManager().setLed(false, ELed.ORANGE);
		getIoManager().setLed(false, ELed.RED);
	}
	
	
	@Override
	public void run() {
		//
		if (getTime() > 3000) {
			DataRepository data = DataRepository.getInstance();
			EState startState = (data.isAlarmEnabled()) ? EState.RUN : EState.WAIT;
			switchStateTo(startState);
		}
	}

}
