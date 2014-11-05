package com.alarme.state;

import org.apache.log4j.Logger;

import com.alarme.core.IStateMachine;
import com.alarme.core.io.IIoManager;
import com.alarme.state.factory.EState;


/**
 * 
 * @author ffradet
 * 
 */
public abstract class AbstractState implements IState {

	private static final Logger log = Logger.getLogger(AbstractState.class);
	
	private long			startTime;
	private IStateMachine	parent;


	/**
	 * 
	 */
	protected void resetTime() {
		startTime = System.currentTimeMillis();
	}


	@Override
	public void init() {
		log.debug("init : " + this);
		resetTime();
	}


	@Override
	public void stop() {
		log.debug("stop : " + this);
		// Idle
	}


	@Override
	public void setStateMachine(IStateMachine parent) {
		this.parent = parent;

	}


	/**
	 * 
	 * @return the elapsed time in ms since the call to init() of this state
	 */
	public long getTime() {
		long curTime = System.currentTimeMillis();
		return curTime - startTime;
	}


	/**
	 * 
	 * @return
	 */
	protected IStateMachine getStateMachine() {
		return parent;
	}


	/**
	 * 
	 * @return
	 */
	protected IIoManager getIoManager() {
		return parent.getIoManager();
	}


	/**
	 * 
	 * @param state
	 */
	protected void switchStateTo(EState state) {
		getStateMachine().switchCurrentStateTo(state);
	}


	/**
	 * 
	 * @param state
	 */
	protected void addState(EState state) {
		getStateMachine().createState(state);
	}


	/**
	 * 
	 * @param state
	 */
	protected void removeState(EState state) {
		getStateMachine().removeState(state);
	}


	/**
	 * 
	 */
	protected void endState() {
		getStateMachine().removeStateInstance(this);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AbstractState [");
		builder.append(getClass().getName());
		builder.append("]");
		return builder.toString();
	}


	@Override
	public boolean equals(IState other) {
		return (other != null) && (other.getClass().getName().equals(getClass().getName()));
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof AbstractState)) {
			return false;
		}
		AbstractState other = (AbstractState) obj;
		if (!other.getClass().getName().equals(getClass().getName())) {
			return false;
		}
		return true;
	}
}
