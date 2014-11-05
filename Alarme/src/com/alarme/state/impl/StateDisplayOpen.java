package com.alarme.state.impl;

import com.alarme.core.io.ELed;
import com.alarme.core.io.IIoManager;
import com.alarme.state.AbstractState;


/**
 * 
 * @author ffradet
 * 
 */
public class StateDisplayOpen extends AbstractState {

	@Override
	public void stop() {
		super.stop();
		getIoManager().setLed(false, ELed.ORANGE);
	}


	@Override
	public void run() {
		IIoManager ioManager = getIoManager();

		// Orange light : something is open
		boolean bOpen = ioManager.isOpenDoor() || ioManager.isOpenWin();
		ioManager.setLed(bOpen, ELed.ORANGE);
	}

}
