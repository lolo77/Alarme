package com.alarme.core.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;


/**
 * 
 * @author ffradet
 * 
 */
public class ConfigRepository {

	private static final Logger log = Logger.getLogger(ConfigRepository.class);
	
	public static final String KEY_SENSOR_PREFIX = "sensor.";
	public static final String KEY_ENABLE_SMS = "enableSMS";
	public static final String KEY_MAIL_RECIPIENTS = "mail.recipients";
	public static final String KEY_MAIL_USER = "mail.smtp.user";
	public static final String KEY_MAIL_PASS = "mail.smtp.pass";
	public static final String KEY_MAIL_HOST = "mail.smtp.host";
	public static final String KEY_MAIL_PORT = "mail.smtp.port";
	public static final String KEY_MAIL_AUTH = "mail.smtp.auth";
	public static final String KEY_MAIL_TLS_ENABLE = "mail.smtp.starttls.enable";
	public static final String KEY_GOOGLE_ID = "google.client.id";
	public static final String KEY_GOOGLE_SECRET = "google.client.secret";
	
	
	
	private static final String		PROPS_PATH	= "res/init.conf";
	private Properties				props		= null;

	private static ConfigRepository	instance;


	/**
	 * No reload until restart
	 */
	private ConfigRepository() {
		reload();
	}


	public static ConfigRepository getInstance() {
		//
		if (instance == null) {
			instance = new ConfigRepository();
		}

		return instance;
	}


	/**
	 * 
	 */
	private void reload() {
		log.debug("ConfigRepository.reload");
		
		File f = new File(PROPS_PATH);
		props = new Properties();
		//
		try {
			props.load(new FileInputStream(f));
		}
		catch (Exception e) {
			e.printStackTrace();
			props = null;
		}
		log.debug("ConfigRepository.reload END");
	}


	/**
	 * 
	 * @return
	 */
	public boolean isLoaded() {
		return props != null;
	}


	/**
	 * 
	 * @return
	 */
	public Properties getProperties() {
		return props;
	}
	
	
	/**
	 * 
	 */
	public void save() {
		File f = new File(PROPS_PATH);
		try {
			props.store(new FileOutputStream(f), "");
		} catch (FileNotFoundException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
	}
}
