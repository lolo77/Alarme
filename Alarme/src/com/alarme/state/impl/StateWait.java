package com.alarme.state.impl;

import org.apache.log4j.Logger;

import com.alarme.core.conf.DataRepository;
import com.alarme.core.io.ELed;
import com.alarme.core.io.IIoManager;
import com.alarme.service.MessageQueue;
import com.alarme.service.MessageQueue.EMedia;
import com.alarme.service.Signal;
import com.alarme.state.AbstractState;
import com.alarme.state.factory.EState;

/**
 * 
 * @author ffradet
 * 
 */
public class StateWait extends AbstractState {

	private static final Logger log = Logger.getLogger(StateWait.class);

//	private boolean bAlarmIso = false;
	
	@Override
	public void init() {
		super.init();
		getIoManager().setLed(true, ELed.GREEN);
		addState(EState.DISPLAY_OPEN);

		DataRepository.getInstance().setAlarmEnabled(false);
	}

	@Override
	public void stop() {
		super.stop();
		getIoManager().setLed(false, ELed.GREEN);
		removeState(EState.DISPLAY_OPEN);
		removeState(EState.CAPTURE);
	}

	/**
	 * signal "code changé"
	 */
	private void signalCodeChanged() {
		log.info("signalCodeChanged");
		Signal.BIPBIP_1320.start();
	}

	/**
	 * signal "erreur lors du changement de code"
	 */
	private void signalErrorCodeNotChanged() {
		log.info("signalErrorCodeNotChanged");
		Signal.BIP440.start();
	}

	/**
	 * signal "Le code doit comporter au minimum 4 chiffres"
	 */
	private void signalErrorCodeTooShort() {
		log.info("signalErrorCodeTooShort");
		Signal.BIP440.start();
	}

	/**
	 * Test sirène (pendant 1s)
	 */
	private void signalSirene() {
		log.info("signalSirene");
//		Signal.ALARME_INTRUSION.start();
		getIoManager().setAlarm(true);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// NOP
		}
		getIoManager().setAlarm(false);
//		Signal.ALARME_INTRUSION.stop();
	}

	/**
	 * 
	 */
	private void signalConnexionUp() {
		log.info("signalConnexionUp");
		Signal.BIPBIP_1320.start();
	}

	/**
	 * 
	 */
	private void signalConnexionDown() {
		log.info("signalConnexionDown");
		Signal.BIP440.start();
	}

	@Override
	public void run() {

		IIoManager ioManager = getIoManager();
		// User inputs are always "*.....#" (variable length).
		String input = ioManager.getKeyboardInput();
		//
		if (input != null) {

			if (input.startsWith("*") && (input.endsWith("#"))) {

				input = input.substring(1, input.length() - 1);

				// "*#"
				if (input.equals("")) {
					switchStateTo(EState.PRERUN);
				}

				// "*XxxxXxxx#" : change code to "Xxxx" (variable length, 4
				// chars min)
				int iSplitIndex = (input.length() + 1) / 2;
				//
				if ((iSplitIndex >= 4) && (iSplitIndex < (input.length() - 1))) {
					String part1 = input.substring(0, iSplitIndex);
					String part2 = input.substring(iSplitIndex, input.length());
					//
					if (part1.equals(part2)) {
						//
						if (part1.length() < 4) {
							signalErrorCodeTooShort();
						} else {
							//
							if (ioManager.changeCode(part1)) {
								signalCodeChanged();
							} else {
								signalErrorCodeNotChanged();
							}
						}
					}
				}

				// "*1#" Test alarm 100ms
				if (input.equals("1")) {
					signalSirene();
				}

				// "*2#" Test network connection
				if (input.equals("2")) {
					log.info("test Network");
					//
					if (getStateMachine().isConnected()) {
						signalConnexionUp();
					} else {
						signalConnexionDown();
					}
				}

				// "*3#" Empty Message queue
				if (input.equals("3")) {
					MessageQueue.getInstance().clearQueue();
					Signal.BIPBIP_1320.start();
				}

				// "*4#" Send log.txt by email
				if (input.equals("4")) {
					MessageQueue.getInstance()
							.createAndPushMessage("log", "", EMedia.EMAIL,
									"log.txt");
					Signal.BIPBIP_1320.start();
				}
				
//				// "*5#" Toggle AlarmIso
//				if (input.equals("5")) {
//					bAlarmIso ^= true;
//					//
//					if (bAlarmIso)
//						Signal.ALARME_INCENDIE.loop();
//					else
//						Signal.ALARME_INCENDIE.stopThread();
//				}

				// "*q# -> shutdown (dev only, impossible on real keyboard)
				if (input.equals("q")) {
					log.info("Exit");
					endState();
				}
			}
		}
	}
}
