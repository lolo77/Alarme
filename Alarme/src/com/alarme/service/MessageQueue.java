package com.alarme.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.log4j.Logger;

import com.alarme.core.conf.ConfigRepository;
import com.alarme.service.SmsSender.SmsException;

/**
 * 
 * @author ffradet
 * 
 */
public class MessageQueue {

	private static final Logger log = Logger.getLogger(MessageQueue.class);

	/**
	 * 
	 * @author ffradet
	 * 
	 */
	public static enum EMedia {
		EMAIL, SMS, BOTH
	};

	/**
	 * 
	 * @author ffradet
	 * 
	 */
	public static class MessageContent implements Serializable {
		private static final long serialVersionUID = -8368356579883182140L;

		private EMedia media;
		private String emailAddressTo;
		private String msgSubject;
		private String msgText;
		private String[] files;
		private long time;

		/**
		 * 
		 * @param emailAddressTo
		 * @param msgSubject
		 * @param msgText
		 * @param files
		 */
		public MessageContent(String emailAddressTo, String msgSubject,
				String msgText, String[] files, long time, EMedia media) {
			super();
			this.emailAddressTo = emailAddressTo;
			this.msgSubject = msgSubject;
			this.msgText = msgText;
			this.files = files;
			this.time = time;
			this.media = media;
		}

		public long getTime() {
			return time;
		}

		public String getEmailAddressTo() {
			return emailAddressTo;
		}

		public String getMsgSubject() {
			return msgSubject;
		}

		public String getMsgText() {
			return msgText;
		}

		public String[] getFiles() {
			return files;
		}

		public EMedia getMedia() {
			return media;
		}

		@Override
		public String toString() {
			return "MessageContent [media=" + media + ", emailAddressTo="
					+ emailAddressTo + ", msgSubject=" + msgSubject
					+ ", msgText=" + msgText + ", files="
					+ Arrays.toString(files) + ", time=" + time + "]";
		}
	};

	/**
	 * 
	 * @author ffradet
	 * 
	 */
	private class MessageManager implements Runnable {

		private static final long DELAY_SLEEP = 1000;

		@Override
		public void run() {
			// Because start is called from inside the MessageQueue constructor, the call to getInstance could create a recursive infinite loop.
			try {
				// Let the time to assign "instance" with a non-null object.
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// NOP
			}
			//
			while (true) {
				MessageQueue.getInstance().manage();
				//
				try {
					Thread.sleep(DELAY_SLEEP);
				} catch (InterruptedException e) {
					// NOP
				}
			}
		}

	};

	private static final String CACHE_PATH = "res/temp.dat";
	private static final long DELAY_RETRY = 1000 * 60 * 5; // 5min

	private static MessageQueue instance = null;
	private MessageManager manager = new MessageManager();
	private Thread managerThread = null;

	private List<MessageContent> lstCache = new ArrayList<MessageContent>();
	private long lastError = 0;
	private String ip = "";

	/**
	 * 
	 * @return
	 */
	public static MessageQueue getInstance() {
		//
		if (instance == null) {
			instance = new MessageQueue();
		}
		return instance;
	}

	/**
	 * 
	 */
	public MessageQueue() {
		detectIpAddress();
		
		readCache();

		managerThread = new Thread(manager);
		managerThread.start();
	}
	
	/**
	 * 
	 */
	public void detectIpAddress() {
		//
		try {
			Enumeration<NetworkInterface> e = NetworkInterface
					.getNetworkInterfaces();
			//
			while (e.hasMoreElements()) {
				NetworkInterface n = (NetworkInterface) e.nextElement();
				Enumeration<InetAddress> ee = n.getInetAddresses();
				//
				while (ee.hasMoreElements()) {
					InetAddress i = ee.nextElement();
					String adr = i.getHostAddress();
					//
					if (adr.startsWith("192.168.")) {
						ip += adr + " ";
					}
				}
			}
			log.debug("Current IP is " + ip);
		} catch (Exception e) {
			log.debug("Current IP is unknown");
		}
	}

	/**
	 * 
	 */
	public void stop() {
		managerThread.interrupt();
	}

	/**
	 * 
	 */
	private void manage() {
//		log.debug("manage");
		//
		if (canRetry()) {
			sendPendingMessages();
		}
	}

	/**
	 * 
	 * @return
	 */
	private boolean canRetry() {
		long tick = System.currentTimeMillis();
		long delay = tick - lastError;
		//
		return (delay > DELAY_RETRY);
	}

	/**
	 * 
	 */
	private void onError() {
		lastError = System.currentTimeMillis();
	}

	/**
	 * 
	 * @param msg
	 */
	private void pushMessage(MessageContent msg) {
		log.debug("Email pushed : " + msg.getMsgSubject());
		lstCache.add(msg);
		writeCache();
	}

	/**
	 * 
	 */
	private void writeCache() {
		log.debug("writeCache : writing email cache : " + lstCache.size()
				+ " items");
		File f = new File(CACHE_PATH);
		ObjectOutputStream s;
		try {
			f.delete();
			f.createNewFile();
			s = new ObjectOutputStream(new FileOutputStream(f));
			s.writeObject(lstCache);
			s.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.debug("writeCache END");
	}

	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void readCache() {
		log.debug("readCache");
		File f = new File(CACHE_PATH);
		//
		if (!f.exists()) {
			return;
		}
		ObjectInputStream s;
		try {
			s = new ObjectInputStream(new FileInputStream(f));
			lstCache = (List<MessageContent>) s.readObject();
			s.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		log.debug("readCache END : " + lstCache.size() + " items");
	}

	/**
	 * 
	 */
	private void sendPendingMessages() {
		//
		Iterator<MessageContent> iter = lstCache.iterator();
		int iPreviousSize = lstCache.size();
		//
		while (iter.hasNext()) {
			MessageContent item = iter.next();
			//
			try {
				sendMessage(item);
				iter.remove();
			} catch (AddressException e) {
				log.debug("sendPendingMessages MAIL : " + e.getMessage());
				iter.remove();
			} catch (SmsException e) {
				log.debug("sendPendingMessages SMS : " + e.getMessage());
			} catch (MessagingException e) {
				log.error("SMTP error : ", e);
				// Stop sending : IO problem
				onError();
				break;
			}
		}

		//
		if (iPreviousSize != lstCache.size()) {
			writeCache();
		}
	}

	/**
	 * 
	 */
	public void clearQueue() {
		log.debug("clearQueue");
		lstCache.clear();
		writeCache();
		log.debug("clearQueue : END");
	}

	/**
	 * 
	 * @param msgSubject
	 * @param msgText
	 * @param files
	 */
	public boolean createAndPushMessage(String msgSubject, String msgText,
			EMedia media, String... files) {
		log.debug("createAndPushMessage");
		ConfigRepository conf = ConfigRepository.getInstance();
		boolean bSent = createAndPushMessageTo(conf.getProperties()
				.getProperty(ConfigRepository.KEY_MAIL_RECIPIENTS), msgSubject + " " + ip + " ",
				msgText, media, files);
		log.debug("createAndPushMessage END");

		return bSent;
	}

	/**
	 * 
	 * @param emailAddressTo
	 * @param msgSubject
	 * @param msgText
	 * @param files
	 */
	public boolean createAndPushMessageTo(String emailAddressTo,
			String msgSubject, String msgText, EMedia media, String... files) {
		log.debug("createAndPushMessageTo");

		boolean bSent = false;
		MessageContent msg = new MessageContent(emailAddressTo, msgSubject,
				msgText, files, System.currentTimeMillis(), media);
		pushMessage(msg);

		log.debug("createAndPushMessageTo END");
		return bSent;
	}

	/**
	 * 
	 * @param msg
	 * @throws AddressException
	 * @throws MessagingException
	 * @throws SmsException
	 */
	private void sendMessage(MessageContent msg) throws AddressException,
			MessagingException, SmsException {
		log.debug("sendMessage");
		//
		if ((msg.getMedia().equals(EMedia.EMAIL))
				|| (msg.getMedia().equals(EMedia.BOTH))) {
			JavaEmailSender.getInstance().sendEmailMessage(msg);
		}
		//
		if ((msg.getMedia().equals(EMedia.SMS))
				|| (msg.getMedia().equals(EMedia.BOTH))) {
			SmsSender.getInstance().sendSmsMessage(msg);
		}
		log.debug("sendMessage END");
	}
}
