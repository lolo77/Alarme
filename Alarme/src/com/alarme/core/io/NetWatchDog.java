package com.alarme.core.io;

import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;

/**
 * 
 * @author ffradet
 * 
 */
public class NetWatchDog implements Runnable {

	private static final Logger log = Logger.getLogger(NetWatchDog.class);
	
	private static final long DELAY_TEST_CONNECT = 60 * 1000;
	
	private static final String[] TAB_URL = {"http://www.google.com", "http://www.yahoo.com", "http://www.microsoft.com"};
	
	private Boolean connected = null;

	@Override
	public void run() {
		log.debug("NetWatchDog is now running.");
		try {
			//
			while (true) {
				refreshConnection();
				
				Thread.sleep(DELAY_TEST_CONNECT);
			}
		} catch (InterruptedException e) {
			log.debug("NetWatchDog was interrupted.");
		}
	}

	/**
	 * 
	 * @param sUrl
	 * @return
	 */
	private boolean isConnected(String sUrl) {
		boolean b = false;
		URL url;
		try {
			url = new URL(sUrl);
			URLConnection con = url.openConnection();
			con.connect();
			b = true;
		} catch (Exception e) {
			log.debug("NetWatchDog : CONNECTION IS DOWN ! " + sUrl);
		}
		
		return b;
	}
	
	
	/**
	 * 
	 */
	private void refreshConnection() {
		boolean b = false;
		//
		for (String sUrl : TAB_URL) {
			b = isConnected(sUrl);
			//
			if (b) {
				break;
			}
		}
		//
		if ((connected == null) || (b != connected)) {
			log.debug("NetWatchDog : connected = " + connected);
			connected = b;
		}
	}
	

	/**
	 * 
	 * @return
	 */
	public boolean isConnected() {
		//
		if (connected == null) {
			refreshConnection();
		}
		return connected;
	}

}
