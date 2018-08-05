package com.alarme.service;

import com.alarme.core.conf.ConfigRepository;
import com.alarme.core.conf.RecipientInfo;
import com.alarme.service.MessageQueue.MessageContent;
import org.apache.log4j.Logger;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;


/**
 * Based on internet example of google calendar api
 * 
 * @author google, ffradet
 *
 */
public class SmsSender {
	
	private static final Logger log = Logger.getLogger(SmsSender.class);
	
	/**
	 * 
	 */
	public static class SmsException extends Exception {

		private static final long serialVersionUID = -507390226929046849L;

		public SmsException(String arg0) {
			super(arg0);
		}

		public SmsException(Throwable arg0) {
			super(arg0);
		}
		
	}


	  private static SmsSender instance = null;

	  /**
	   * 
	   */
	  public SmsSender() {
	  }
	  
	  
	  /**
	   * 
	   * @return
	   */
	  public static SmsSender getInstance() {
		  //
		  if (instance == null) {
			  instance = new SmsSender();
		  }
		  
		  return instance;
	  }


	  /**
	   * 
	   * @param msg
	   * @return
	   */

	  
	  /**
	   * 
	   * @param msg
	   * @return
	   */
	  public boolean sendSmsMessage(MessageContent msg) throws SmsException {
		  //
		  try {
			  ConfigRepository conf = ConfigRepository.getInstance();
			  String sSendSMS = conf.getProperties().getProperty(ConfigRepository.KEY_ENABLE_SMS);
			  //
			  if ((sSendSMS != null) && ("1".equals(sSendSMS))) {
				  for (RecipientInfo r : msg.getRecipients())
				  {
					  if (r.getSmsFreeUser() != null) {
						  String url = ConfigRepository.getInstance().getProperties().getProperty(ConfigRepository.KEY_SMS_URL_FREE);
						  url = url.replace("{0}", r.getSmsFreeUser());
						  url = url.replace("{1}", r.getSmsFreePass());
						  url = url.replace("{2}", URLEncoder.encode(msg.getMsgText(), "UTF-8"));
						  URL smsService = new URL(url);
						  log.debug("Sending SMS to " + r.getSmsFreeUser() + " (" + r.getEmail() + ")");
						  smsService.openConnection();
					  }
				  }
				  log.debug("Message SMS sent successfully : " + msg);
			  } else {
				  log.debug("Message SMS not sent (mail.smtp.user is null or enableSMS is not 1) : " + msg);
			  }
			  
			  return true;
		  }
		  catch (Exception e) {
			  throw new SmsException(e);
		  }
	  }
}
