package com.alarme.service;

import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;

import org.apache.log4j.Logger;

import com.alarme.core.io.IIoManager;


/**
 * 
 * @author ffradet
 * 
 */
public class SignalManager implements LineListener {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(SignalManager.class);
	
	private static SignalManager	instance				= null;

	private int						iActiveSignalCounter	= 0;
	private IIoManager				ioManager				= null;


	/**
	 * 
	 */
	private SignalManager() {
		iActiveSignalCounter = 0;
	}


	/**
	 * 
	 * @return
	 */
	public static SignalManager getInstance() {
		//
		if (instance == null) {
			instance = new SignalManager();
		}

		return instance;
	}


	/**
	 * 
	 * @param ioManager
	 */
	public void bindIoManager(IIoManager ioManager) {
		this.ioManager = ioManager;
	}


	@Override
	public void update(LineEvent event) {
		//
		if (event.getType().equals(Type.START)) {
			iActiveSignalCounter++;
//			log.debug("START iActiveSignalCounter = " + iActiveSignalCounter);
		}
		//
		if (event.getType().equals(Type.STOP)) {
			iActiveSignalCounter--;
//			log.debug("STOP iActiveSignalCounter = " + iActiveSignalCounter);
		}
		//
		if (ioManager != null) {
			//
			if (iActiveSignalCounter > 0) {
				// Turn on audio ampli
				ioManager.setAudioAmpli(true);
			}
			//
			if (iActiveSignalCounter == 0) {
				// Turn off audio ampli
				ioManager.setAudioAmpli(false);
			}
		}
	}

}
