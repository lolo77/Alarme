package com.alarme.state.factory;

import com.alarme.state.IState;


/**
 * 
 * @author ffradet
 * 
 */
public class StateFactory implements IStateFactory {

	private static IStateFactory	instance	= null;


	/**
	 * 
	 * @return
	 */
	public static IStateFactory getInstance() {
		//
		if (instance == null) {
			instance = new StateFactory();
		}

		return instance;
	}


	@Override
	public IState buildState(EState state) {
		try {
			return state.getStateClass().newInstance();
		}
		catch (Exception e) {}

		return null;
	}
}
