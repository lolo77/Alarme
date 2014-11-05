package com.alarme.state.factory;

import com.alarme.state.IState;


/**
 * 
 * @author ffradet
 * 
 */
public interface IStateFactory {

	/**
	 * 
	 * @param state
	 * @return
	 */
	public IState buildState(EState state);

}
