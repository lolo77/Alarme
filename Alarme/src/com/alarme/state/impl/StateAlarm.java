package com.alarme.state.impl;

import com.alarme.core.conf.ConfigRepository;
import com.alarme.core.conf.Sensor;
import com.alarme.core.conf.SensorRepository;
import com.alarme.core.io.IIoManager;
import com.alarme.service.MessageQueue;
import com.alarme.service.MessageQueue.EMedia;
import com.alarme.state.AbstractState;
import com.alarme.state.factory.EState;
import org.apache.log4j.Logger;

/**
 * 
 * @author ffradet
 * 
 */
public class StateAlarm extends AbstractState {

	private static final Logger log = Logger.getLogger(StateAlarm.class);

	private static final long STATE_FULL_DURATION = 15 * 60 * 1000; // 15min in
																	// ms

	private boolean bHalfVol = false;

	@Override
	public void init() {
		super.init();
		bHalfVol = false;

		// Start sirène 100%
		// Signal.ALARME_INTRUSION.setVolume(1.0);
		// Signal.ALARME_INTRUSION.loop();

		IIoManager io = getIoManager();
		io.setAlarm(true); // Start piezo alarm

		String s = "";
		int iSensorCount = io.getSensorCount();
		//
		for (int i = 0; i < iSensorCount; i++) {
			//
			if (io.getSensor(i)) {
				Sensor sensor = SensorRepository.getInstance().getByPort(i);
				//
				if (sensor != null) {
					s += sensor.getDescription();
					s += "\n";
				}
			}
		}

		log.debug(s);

		// Send message
		MessageQueue sender = MessageQueue.getInstance();
		sender.createAndPushMessage(ConfigRepository.getInstance().getRecipients(), "Intrusion detectee, sirene activee", s,
				EMedia.BOTH);

		// Start video capture
		addState(EState.CAPTURE);

	}

	@Override
	public void stop() {
		super.stop();
//		Signal.ALARME_INTRUSION.stop();

		IIoManager io = getIoManager();
		io.setAlarm(false);
	}

	@Override
	public void run() {
		//
		if ((getTime() > STATE_FULL_DURATION) && (!bHalfVol)) {
			IIoManager io = getIoManager();
			io.setAlarm(false);

			bHalfVol = true;
			// Signal.ALARME_INTRUSION.setVolume(0.5);
			// Sirène 50% après 5min

			// Send message
			MessageQueue sender = MessageQueue.getInstance();
			sender.createAndPushMessage(ConfigRepository.getInstance().getRecipients(), "Sirene desactivee apres delai", "", EMedia.BOTH);
		}

		//
		if (getIoManager().isInputCode()) {
			// Stop sirène
			switchStateTo(EState.WAIT);

			// Send message
			MessageQueue sender = MessageQueue.getInstance();
			sender.createAndPushMessage(ConfigRepository.getInstance().getRecipients(), (bHalfVol) ? "Code secret saisi" : "Sirene desactivee manuellement", "", EMedia.BOTH);
		}
	}

}
