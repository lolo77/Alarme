package com.alarme.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.alarme.core.conf.ConfigRepository;
import com.alarme.service.MessageQueue.MessageContent;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;


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

	/**
	   * Be sure to specify the name of your application. If the application name is {@code null} or
	   * blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
	   */
	  private static final String APPLICATION_NAME = "hk-rpi-serris/1.0";

	  /** Directory to store user credentials. */
	  //System.getProperty("user.home")
	  private static final java.io.File DATA_STORE_DIR =
	      new java.io.File("res/calendar");

	  /**
	   * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
	   * globally shared instance across your application.
	   */
	  private FileDataStoreFactory dataStoreFactory;
	 
	  /** Global instance of the HTTP transport. */
	  private HttpTransport httpTransport;

	  /** Global instance of the JSON factory. */
	  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	  private com.google.api.services.calendar.Calendar client;

	  
	  private static SmsSender instance = null;

	  /**
	   * 
	   */
	  public SmsSender() {
		  try {
		      // initialize the transport
		      httpTransport = GoogleNetHttpTransport.newTrustedTransport();

		      // initialize the data store factory
		      dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

		      // authorization
		      Credential credential = authorize();

		      // set up global Calendar instance
		      client = new com.google.api.services.calendar.Calendar.Builder(
		          httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
		  }
		  catch (Exception e) {
			  client = null;
		  }
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
	   *  Authorizes the installed application to access user's protected data. 
	   */
	  private Credential authorize() throws Exception {
	    // load client secrets
		  //CalendarSample.class.getResourceAsStream(
	    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
	        new InputStreamReader(new FileInputStream(new File("res/client_secrets.json"))));
	    if (clientSecrets.getDetails().getClientId().startsWith("Enter")
	        || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
	      log.debug(
	          "Enter Client ID and Secret from https://code.google.com/apis/console/?api=calendar "
	          + "into res/client_secrets.json");
	      System.exit(1);
	    }
	    // set up authorization code flow
	    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
	        httpTransport, JSON_FACTORY, clientSecrets,
	        Collections.singleton(CalendarScopes.CALENDAR)).setDataStoreFactory(dataStoreFactory)
	        .build();
	    // authorize
	    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
	  }
	  
	  /**
	   * 
	   * @param msg
	   * @return
	   */
	  private static Event newEvent(MessageContent msg) {
		  Event event = new Event();
		  
		  Date d = new Date(msg.getTime());
		  SimpleDateFormat sf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		  
		  event.setSummary(msg.getMsgSubject() + " " + sf.format(d));
		  
		  // Fill the message
		  event.setDescription(msg.getMsgText() + " [" + sf.format(d) + "]");
		  
		  Date startDate = new Date(System.currentTimeMillis() + 10*60*1000);
		  DateTime start = new DateTime(startDate, TimeZone.getTimeZone("UTC"));
		  event.setStart(new EventDateTime().setDateTime(start));
		  
		  Date endDate = new Date(startDate.getTime() + 5*60*1000);
		  DateTime end = new DateTime(endDate, TimeZone.getTimeZone("UTC"));
		  event.setEnd(new EventDateTime().setDateTime(end));
		  
		  return event;
	  }
	  
	  
	  /**
	   * 
	   * @param msg
	   * @return
	   */
	  public boolean sendSmsMessage(MessageContent msg) throws SmsException {
		  //
		  try {
			  Event event = newEvent(msg);
			  ConfigRepository conf = ConfigRepository.getInstance();
			  String sUser = conf.getProperties().getProperty(ConfigRepository.KEY_MAIL_USER);
			  String sSendSMS = conf.getProperties().getProperty(ConfigRepository.KEY_ENABLE_SMS);
			  //
			  if ((sUser != null) && (sSendSMS != null) && ("1".equals(sSendSMS))) {
				  client.events().insert(sUser, event).execute();
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
