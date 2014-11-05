package com.alarme.state.impl;

import com.alarme.state.AbstractState;


/**
 * 
 * @author ffradet
 * 
 */
public class StateCapture extends AbstractState {

	private static final long	STATE_DURATION	= 5 * 60 * 1000; // 5min in ms
															// private static final long FPS = 5; // Frames per second (hz)


	@Override
	public void run() {
		//
		if (getTime() > STATE_DURATION) {
			endState();
		}
	}
}
