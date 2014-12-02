package com.alarme.app;

import java.io.IOException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.log4j.Logger;

import com.alarme.core.IStateMachine;
import com.alarme.core.StateMachine;
import com.alarme.service.AdminManager;
import com.alarme.service.MessageQueue;
import com.alarme.service.MessageQueue.EMedia;
import com.alarme.service.SignalManager;


/**
 * 
 * @author ffradet
 * 
 */
public class Alarme {

	private static final Logger log = Logger.getLogger(Alarme.class);
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		MessageQueue.getInstance().createAndPushMessage("Systeme d'alarme en cours de lancement", "", EMedia.EMAIL);
		
//		 Logger.log("Welcome to OpenCV " + Core.VERSION);
//		 System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//		 Mat m = Mat.eye(3, 3, CvType.CV_8UC1);
//		 Logger.log("m = " + m.dump());

		if (AudioSystem.getMixer(null) == null) {
			log.debug("AudioSystem Unavailable, exiting!");
			MessageQueue.getInstance().createAndPushMessage("Systeme d'alarme arrete", "AudioSystem Unavailable, exiting!", EMedia.EMAIL);
			System.exit(-1);
		}

		IStateMachine machine = new StateMachine();
		SignalManager.getInstance().bindIoManager(machine.getIoManager());
		AdminManager.getInstance().bindIoManager(machine.getIoManager());
		//
		while (!machine.isTerminated()) {
			machine.run();
		}

		// Logger.log("Video starting");
		//
		// Mat webcam_image = new Mat();
		// // BufferedImage temp;
		// VideoCapture capture = new VideoCapture(0);
		// boolean bOpened = capture.isOpened();
		// Logger.log("opened : " + bOpened);
		// if (bOpened) {
		// capture.read(webcam_image);
		// if (!webcam_image.empty()) {
		// Logger.log("image w=" + webcam_image.width() + " ; h="
		// + webcam_image.height());
		// }
		// }

		AdminManager.getInstance().stop();
		MessageQueue.getInstance().createAndPushMessage("Systeme d'alarme arrete", "Thread terminated.", EMedia.EMAIL);
		MessageQueue.getInstance().flush();
		MessageQueue.getInstance().stopThread();
		log.debug("Thread terminated.");
	}
}
