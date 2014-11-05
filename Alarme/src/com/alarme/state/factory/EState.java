package com.alarme.state.factory;

import com.alarme.state.IState;
import com.alarme.state.impl.StateAlarm;
import com.alarme.state.impl.StateCapture;
import com.alarme.state.impl.StateDisplayOpen;
import com.alarme.state.impl.StatePreRun;
import com.alarme.state.impl.StateRun;
import com.alarme.state.impl.StateTest;
import com.alarme.state.impl.StateWait;

/**
 * 
 * @author ffradet
 * 
 */
public enum EState {
	PRERUN(StatePreRun.class), //
	RUN(StateRun.class), //
	ALARM(StateAlarm.class), //
	WAIT(StateWait.class), //
	CAPTURE(StateCapture.class), //
	DISPLAY_OPEN(StateDisplayOpen.class), //
	TEST(StateTest.class); //

	private Class<? extends IState> stateClass;

	/**
	 * 
	 * @param stateClass
	 */
	private EState(Class<? extends IState> stateClass) {
		this.stateClass = stateClass;
	}

	/**
	 * 
	 * @return
	 */
	public Class<? extends IState> getStateClass() {
		return stateClass;
	}

}
