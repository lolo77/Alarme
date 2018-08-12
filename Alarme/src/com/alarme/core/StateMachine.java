package com.alarme.core;

import com.alarme.core.conf.ConfigRepository;
import com.alarme.core.io.IIoManager;
import com.alarme.core.io.NetWatchDog;
import com.alarme.core.io.impl.IoManagerEmul;
import com.alarme.service.MessageQueue;
import com.alarme.service.MessageQueue.EMedia;
import com.alarme.state.IState;
import com.alarme.state.factory.EState;
import com.alarme.state.factory.StateFactory;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * 
 * @author ffradet
 * 
 */
public class StateMachine implements IStateMachine {

	private static final Logger log = Logger.getLogger(StateMachine.class);
	
	private IIoManager		ioManager       = new IoManagerEmul();
	
	private List<IState>	lstStates		= new ArrayList<>();
	private IState			currentState;
	private Boolean			oldConnected	= null;

	private NetWatchDog		watchDog 		= new NetWatchDog();
	private Thread		    watchDogThread 	= null;

	/**
	 * Call init()
	 * 
	 */
	public StateMachine() {
		init();
	}


	@Override
	public void init() {
		createState(EState.TEST);
		watchDogThread = new Thread(watchDog);
		watchDogThread.start();
	}


	@Override
	public void stop() {
		ioManager.stop();
		watchDogThread.interrupt();
	}

	
	@Override
	public boolean isConnected() {
		return watchDog.isConnected();
	}
	

	@Override
	public void run() {
		long timeBegin = System.currentTimeMillis();

//		boolean bOldAlimSecteur = ioManager.isAlimSecteur();
		boolean bConnected = watchDog.isConnected();

		ioManager.refreshInputs();
		
		// If no wire is connected -> terminate main thread by removing the only state.
		if (isState(EState.TEST)) {
			//
			if (ioManager.isAllDown()) {
				// Terminate thread
				removeState(EState.TEST);
			}
		}

		int _hash = lstStates.hashCode();
		
		// To allow add / remove while iterating over the collection (changes
		// will take effect at the next run cycle)
		List<IState> backList = new ArrayList<>(lstStates.size());
		backList.addAll(lstStates);
		//
		for (IState state : backList) {
			currentState = state;
			state.run();
		}

		//
//		if ((bOldAlimSecteur) && (!ioManager.isAlimSecteur())) {
//			MessageQueue.getInstance().createAndPushMessage(
//					"Panne energie electrique", "", EMedia.BOTH);
//		}
//		//
//		if ((!bOldAlimSecteur) && (ioManager.isAlimSecteur())) {
//			MessageQueue.getInstance().createAndPushMessage(
//					"Retour energie electrique", "", EMedia.BOTH);
//		}
		//
		if (oldConnected != null) {
			//
			if ((oldConnected) && (!bConnected)) {
				MessageQueue.getInstance().createAndPushMessage(ConfigRepository.getInstance().getRecipients(),
						"Panne reseau Internet", "", EMedia.BOTH);
			}
			//
			if ((!oldConnected) && (bConnected)) {
				// IP address may have changed
				MessageQueue.getInstance().detectIpAddress();
				MessageQueue.getInstance().createAndPushMessage(ConfigRepository.getInstance().getRecipients(),
						"Retour reseau Internet", "", EMedia.BOTH);
			}
		}

		oldConnected = bConnected;

		int hash = lstStates.hashCode();
		//
		if (hash != _hash) {
			log.debug(lstStates.toString());
		}

		long timeEnd = System.currentTimeMillis();
		long timeDelta = timeEnd - timeBegin;
		//
		if (timeDelta < ioManager.getRunPeriodDuration()) {
			// Adjust loop duration to RUN_TICK_DURATION according to state
			// management real duration
			try {
				Thread.sleep(ioManager.getRunPeriodDuration() - timeDelta);
			}
			catch (InterruptedException e) {
				//
			}
		}
	}


	@Override
	public void createState(EState state) {
		log.debug("createState : " + state);
		IState stateInst = StateFactory.getInstance().buildState(state);
		//
		if (stateInst != null) {
			log.debug("State instance created : " + stateInst);
			// Add once
			if (!lstStates.contains(stateInst)) {
				log.debug("Adding state instance");
				lstStates.add(stateInst);
				stateInst.setStateMachine(this);
				log.debug("Init state instance");
				stateInst.init();
			}
		}
		log.debug("createState END");
	}


	@Override
	public void removeState(EState state) {
		log.debug("removeState : " + state);
		IState stateInst = StateFactory.getInstance().buildState(state);
		//
		if (stateInst != null) {
			log.debug("removeState : instance created for removal : " + stateInst);
			// find task
			int index = lstStates.indexOf(stateInst);
			if (index >= 0) {
				log.debug("State found for removal");
				stateInst = lstStates.get(index);
				log.debug("Removing state");
				removeStateInstance(stateInst);
			}
		}
		log.debug("removeState END");
	}


	@Override
	public void removeStateInstance(IState state) {
		log.debug("removeStateInstance : " + state);
		state.stop();
		log.debug("lstStates.remove");
		lstStates.remove(state);
		log.debug("removeStateInstance END");
	}


	@Override
	public void switchCurrentStateTo(EState state) {
		log.debug("switchCurrentStateTo : " + state);
		removeStateInstance(currentState);
		createState(state);
	}


	@Override
	public boolean isTerminated() {
		return (lstStates.isEmpty()) || (ioManager.getKeyboardInput().equals("q"));
	}
	
	
	@Override
	public boolean isState(EState state) {
		//
		for (IState item : lstStates) {
				//
				if (item.getClass().equals(state.getStateClass())) {
					return true;
				}
		}
		return false;
	}


	@Override
	public IIoManager getIoManager() {
		return ioManager;
	}

}
