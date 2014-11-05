package com.alarme.state;

import com.alarme.core.IStateMachine;
import com.alarme.core.IStoppable;


/**
 * 
 * @author ffradet
 * 
 */
public interface IState extends Runnable, IStoppable {

	/**
	 * 
	 */
	public void init();


	/**
	 * 
	 * @param parent
	 */
	public void setStateMachine(IStateMachine parent);


	/**
	 * 
	 * @param other
	 * @return
	 */
	public boolean equals(IState other);
}
