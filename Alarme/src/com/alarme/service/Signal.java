package com.alarme.service;

import java.io.File;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;


/**
 * 
 * @author ffradet
 * 
 */
public class Signal {

	Clip						clip				= null;

	public static final Signal	ALARME_INTRUSION	= new Signal("res/alarme.wav");
	public static final Signal	ALARME_INCENDIE		= new Signal("res/ISO32001.wav");
	public static final Signal	BIP_KB				= new Signal("res/bip_kb.wav");
	public static final Signal	BIP					= new Signal("res/bip.wav");
	public static final Signal	BIP440				= new Signal("res/bip440.wav");
	public static final Signal	BIPBIP_440			= new Signal("res/bipbip_440.wav");
	public static final Signal	BIPBIP_440_1S		= new Signal("res/bipbip_440_1s.wav");
	public static final Signal	BIPBIP_1320			= new Signal("res/bipbip_1320.wav");


	/**
	 * 
	 * @param res
	 */
	public Signal(String res) {
		//
		try {
			clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(new File(res)));
			clip.addLineListener(SignalManager.getInstance());
		}
		catch (Exception e) {
			clip = null;
		}
	}


	/**
	 * 
	 * @return
	 */
	public boolean isReady() {
		return clip != null;
	}


	/**
	 * 
	 */
	public void start() {
		//
		if (!isReady()) {
			return;
		}
		clip.setFramePosition(0);
		clip.start();
	}


	/**
	 * 
	 */
	public void stop() {
		//
		if (!isReady()) {
			return;
		}
		clip.stop();
//		clip.flush();
	}


	/**
	 * 
	 */
	public void loop() {
		//
		if (!isReady()) {
			return;
		}
		clip.loop(Clip.LOOP_CONTINUOUSLY);
	}


	/**
	 * Use Balance to set output volume on 1 channel
	 * 
	 * @param volCoef
	 *            [0;1]
	 */
	public void setVolume(double volCoef) {
		//
		if (!isReady()) {
			return;
		}
		FloatControl vol = (FloatControl) clip.getControl(Type.MASTER_GAIN);
		vol.setValue((float) (volCoef * 32) - 32);
	}

}
