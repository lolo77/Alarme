package com.alarme.core.conf;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;


/**
 * 
 * @author ffradet
 * 
 */
public class ConfigRepository {

	private static final Logger log = Logger.getLogger(ConfigRepository.class);
	
	public static final String KEY_SENSOR_PREFIX = "sensor.";
	public static final String KEY_ENABLE_SMS = "enableSMS";
	public static final String KEY_SMS_URL_FREE = "sms.url.free";
	public static final String KEY_RECIPIENT_PREFIX = "recipient.";
	public static final String KEY_RECIPIENT_EMAIL_SUFFIX = ".email";
	public static final String KEY_RECIPIENT_SMS_FREE_USER_SUFFIX = ".sms.free.user";
	public static final String KEY_RECIPIENT_SMS_FREE_PASS_SUFFIX = ".sms.free.pass";
	public static final String KEY_MAIL_USER = "mail.smtp.user";
	public static final String KEY_MAIL_PASS = "mail.smtp.pass";
	public static final String KEY_MAIL_HOST = "mail.smtp.host";
	public static final String KEY_MAIL_PORT = "mail.smtp.port";
	public static final String KEY_MAIL_AUTH = "mail.smtp.auth";
	public static final String KEY_MAIL_TLS_ENABLE = "mail.smtp.starttls.enable";

	
	
	private static final String		PROPS_PATH	= "res/init.conf";
	private Properties				props		= null;

	private static ConfigRepository	instance;
	private ArrayList<RecipientInfo> lstRecipients = null;


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


	public RecipientInfo getRecipientInfo(int num) {
		String email = props.getProperty(KEY_RECIPIENT_PREFIX + Integer.toString(num) + KEY_RECIPIENT_EMAIL_SUFFIX);
		if (email == null) {
			return null;
		}
		String smsFreeUser = props.getProperty(KEY_RECIPIENT_PREFIX + Integer.toString(num) + KEY_RECIPIENT_SMS_FREE_USER_SUFFIX);
		String smsFreePass = props.getProperty(KEY_RECIPIENT_PREFIX + Integer.toString(num) + KEY_RECIPIENT_SMS_FREE_PASS_SUFFIX);
		RecipientInfo out = new RecipientInfo(email, smsFreeUser, smsFreePass);
		return out;
	}

	public ArrayList<RecipientInfo> getRecipients() {
		if (lstRecipients == null) {
			lstRecipients = new ArrayList<RecipientInfo>();
			int i = 0;
			RecipientInfo r;
			while ((r = getRecipientInfo(i)) != null)
			{
				lstRecipients.add(r);
				i++;
			}
		}
		return lstRecipients;
	}

	public RecipientInfo getRecipientInfoByEmail(String email) {
		for (RecipientInfo r : getRecipients()) {
			if (email.equalsIgnoreCase(r.getEmail())) {
				return r;
			}
		}
		return null;
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
