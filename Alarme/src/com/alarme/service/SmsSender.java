package com.alarme.service;

import com.alarme.core.conf.ConfigRepository;
import com.alarme.core.conf.RecipientInfo;
import com.alarme.service.MessageQueue.MessageContent;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.stream.Collectors;


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
						  url = url.replace("{2}", URLEncoder.encode(msg.getMsgSubject() + "\r\n" + msg.getMsgText(), "UTF-8"));
						  URL smsService = new URL(url);
						  log.debug("Sending SMS to " + r.getSmsFreeUser() + " (" + r.getEmail() + ")");
						  HttpURLConnection conn = (HttpURLConnection)smsService.openConnection();
						  InputStream is = conn.getInputStream();
						  String result = new BufferedReader(new InputStreamReader(is))
								  .lines().collect(Collectors.joining("\n"));
						  log.debug("Retour service FREE : Content = [" + result + "]");
						  int rc = conn.getResponseCode();
						  log.debug("Retour service FREE : HTTP Response Code = [" + rc + "]");
						  if (rc != 200) {
							  log.error("SMS not sent.");
						  }
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
