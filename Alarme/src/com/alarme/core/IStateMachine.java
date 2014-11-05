package com.alarme.core;

import com.alarme.core.io.IIoManager;
import com.alarme.state.IState;
import com.alarme.state.factory.EState;

/**
 * 
 * @author ffradet
 * 
 */
public interface IStateMachine extends Runnable, IStoppable {

	/**
	 * 
	 */
	public void init();

	/**
	 * 
	 * @param state
	 */
	public void createState(EState state);

	/**
	 * 
	 * @param state
	 */
	public void removeState(EState state);

	/**
	 * 
	 * @param state
	 */
	public void removeStateInstance(IState state);

	/**
	 * 
	 * @param state
	 */
	public void switchCurrentStateTo(EState state);

	/**
	 * 
	 * @return
	 */
	public IIoManager getIoManager();

	/**
	 * 
	 * @return
	 */
	public boolean isTerminated();

	/**
	 * 
	 * @param state
	 * @return
	 */
	public boolean isState(EState state);

	/**
	 * 
	 * @return
	 */
	public boolean isConnected();
}
