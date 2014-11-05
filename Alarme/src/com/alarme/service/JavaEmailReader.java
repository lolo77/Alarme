package com.alarme.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

import org.apache.log4j.Logger;

import com.alarme.core.conf.ConfigRepository;

/**
 * Acces IMAP au serveur GMAIL
 * 
 * @author ffradet
 *
 */
public class JavaEmailReader {

	private static final Logger log = Logger.getLogger(JavaEmailReader.class);

	private static JavaEmailReader instance = null;

	private Store store = null;
	private String username, password;
	private Folder folder;

	/**
	 * 
	 */
	private JavaEmailReader() {

	}

	public static JavaEmailReader getInstance() {
		if (instance == null) {
			instance = new JavaEmailReader();

			Properties propsConfig = ConfigRepository.getInstance()
					.getProperties();

			final String user = propsConfig.getProperty(ConfigRepository.KEY_MAIL_USER);
			final String pass = propsConfig.getProperty(ConfigRepository.KEY_MAIL_PASS);
			//
			if (user == null) {
				log.warn("No mail account configured : mail.smtp.user is null.");
			} else {
				instance.setUserPass(user, pass);
			}
		}

		return instance;
	}

	/**
	 * 
	 * @param username
	 * @param password
	 */
	private void setUserPass(String username, String password) {
		this.username = username.trim().toLowerCase();
		this.password = password;
	}

	private void connect() throws Exception {

		// POP3 access -> Folders and flags not supported
		// String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
		//
		// Properties pop3Props = new Properties();
		//
		// pop3Props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
		// pop3Props.setProperty("mail.pop3.socketFactory.fallback", "false");
		// pop3Props.setProperty("mail.pop3.port", "995");
		// pop3Props.setProperty("mail.pop3.socketFactory.port", "995");
		//
		// URLName url = new URLName("pop3", "pop.gmail.com", 995, "",
		// username, password);
		//
		// session = Session.getInstance(pop3Props, null);
		// store = new POP3SSLStore(session, url);
		// store.connect();

		// IMAP access -> works well
		Properties props = System.getProperties();
		props.setProperty("mail.store.protocol", "imaps");
		Session session = Session.getDefaultInstance(props, null);
		store = session.getStore("imaps");
		store.connect("imap.gmail.com", username, password);
//		log.debug(store);
	}

	/**
	 * 
	 * @param folderName
	 * @throws Exception
	 */
	private void openFolder(String folderName) throws Exception {

		// Open the Folder
		folder = store.getFolder(folderName);

		if (folder == null) {
			throw new Exception("Invalid folder");
		}

		// try to open read/write and if that fails try read-only
		try {

			folder.open(Folder.READ_WRITE);

		} catch (MessagingException ex) {
			log.debug("Open in READ ONLY");
			folder.open(Folder.READ_ONLY);

		}
	}

	private void closeFolder() throws Exception {
		folder.close(false);
	}

//	private int getMessageCount() throws Exception {
//		return folder.getMessageCount();
//	}

	private int getNewMessageCount() throws Exception {
		return folder.getUnreadMessageCount();
	}

	private void disconnect() throws Exception {
		store.close();
	}

//	private void printMessage(int messageNo) throws Exception {
//		log.debug("Getting message number: " + messageNo);
//
//		Message m = null;
//
//		try {
//			m = folder.getMessage(messageNo);
//			dumpPart(m);
//		} catch (IndexOutOfBoundsException iex) {
//			log.error("Message number out of range");
//		}
//	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	private Message[] getUnreadMessages() throws Exception {
		Flags seen = new Flags(Flags.Flag.SEEN);
		FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
		log.debug("folder.search");
		Message[] msgs = folder.search(unseenFlagTerm);
		log.debug("folder.search returned " + msgs.length);

		// Use a suitable FetchProfile
		FetchProfile fp = new FetchProfile();
		fp.add(FetchProfile.Item.ENVELOPE);
		fp.add(FetchProfile.Item.FLAGS);
		fp.add(FetchProfile.Item.CONTENT_INFO);
		log.debug("folder.fetch");
		folder.fetch(msgs, fp);
		
		return msgs;
	}
	
//	private void printAllUnreadMessages() throws Exception {
//
//		Message[] msgs = getUnreadMessages();
//		for (int i = 0; i < msgs.length; i++) {
//			log.debug("--------------------------");
//			log.debug("MESSAGE #" + (i + 1) + ":");
//			dumpPart(msgs[i]);
//		}
//	}
//
//	private static void dumpPart(Part p) throws Exception {
//		if (p instanceof Message) {
//			dumpEnvelope((Message) p);
//		}
//
//		String ct = p.getContentType();
//		try {
//			log.debug("CONTENT-TYPE: " + (new ContentType(ct)).toString());
//		} catch (ParseException pex) {
//			log.debug("BAD CONTENT-TYPE: " + ct);
//		}
//
//		/*
//		 * Using isMimeType to determine the content type avoids fetching the
//		 * actual content data until we need it.
//		 */
//		if (p.isMimeType("text/plain")) {
//			log.debug("This is plain text");
//			log.debug("---------------------------");
//			System.out.println((String) p.getContent());
//		} else {
//			if (p.getContent() instanceof MimeMultipart) {
//				MimeMultipart mp = (MimeMultipart) p.getContent();
//				log.debug("Mime Multipart : " + mp.getCount());
//				log.debug("preamble = " + mp.getPreamble());
//				for (int i = 0; i < mp.getCount(); i++) {
//					BodyPart bp = mp.getBodyPart(i);
//					log.debug("BodyPart #" + i);
//					log.debug("   CONTENT-TYPE : " + bp.getContentType());
//					log.debug("   DISPOSITION : " + bp.getDisposition());
//					log.debug("   DESCRIPTION : " + bp.getDescription());
//					log.debug("   FILENAME : " + bp.getFileName());
//					log.debug("   CONTENT : " + bp.getContent().toString());
//				}
//			}
//			// just a separator
//			log.debug("---------------------------");
//
//		}
//	}
//
//	private static void dumpEnvelope(Message m) throws Exception {
//		log.debug(" UserFlags : ");
//		List<Flag> allFlags = new ArrayList<Flag>();
//		allFlags.add(Flag.ANSWERED);
//		allFlags.add(Flag.DELETED);
//		allFlags.add(Flag.DRAFT);
//		allFlags.add(Flag.FLAGGED);
//		allFlags.add(Flag.RECENT);
//		allFlags.add(Flag.SEEN);
//		allFlags.add(Flag.USER);
//		for (Flag f : m.getFlags().getSystemFlags()) {
//			log.debug("Flag is : " + allFlags.indexOf(f));
//		}
//		log.debug(" ");
//		Address[] a;
//		// FROM
//		if ((a = m.getFrom()) != null) {
//			for (Address element : a) {
//				log.debug("FROM: " + element.toString());
//			}
//		}
//
//		// TO
//		if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
//			for (Address element : a) {
//				log.debug("TO: " + element.toString());
//			}
//		}
//
//		// SUBJECT
//		log.debug("SUBJECT: " + m.getSubject());
//
//		// DATE
//		Date d = m.getSentDate();
//		log.debug("SendDate: " + (d != null ? d.toString() : "UNKNOWN"));
//
//	}

	/**
	 * 
	 * @param emailRfc822
	 * @return
	 */
	private static String getEmailAddressFromRfc822(String emailRfc822) {
		String s = "";
		int iStart = emailRfc822.indexOf("<");
		int iEnd = emailRfc822.indexOf(">");
		
		if ((iStart >= 0) && (iEnd > iStart)) {
			s = emailRfc822.substring(iStart + 1, iEnd);
			
		} else {
			s = emailRfc822;
		}
		s = s.trim();
		return s.toLowerCase();
	}
	
	/**
	 * 
	 * @return
	 */
	private List<String> getAllowedSenders() {
		List<String> out = new ArrayList<String>();

		Properties props = ConfigRepository.getInstance().getProperties();
		String allowedSenders = props.getProperty(ConfigRepository.KEY_MAIL_RECIPIENTS);
		String[] tab = allowedSenders.split(",");
		
		for (String s : tab) {
			s = s.toLowerCase();
			s = s.trim();
			out.add(s);
		}
		return out;
	}
	
	
	/**
	 * 
	 * @param m
	 * @return
	 * @throws Exception
	 */
	private String getAuthorizedSender(Message m) throws Exception {
		
		List<String> tabAllowedSenders = getAllowedSenders();
		Address[] from = m.getFrom();
		String email = null;
		// All the senders must be known
		for (Address a : from) {
			email = getEmailAddressFromRfc822(a.toString());
			
			if (email.equals(username)) {
				log.debug("Ignored email from " + email);
				return null;
			}
			if (!tabAllowedSenders.contains(email)) {
				log.info("Unauthorized admin action :");
				log.debug("Address type = " + a.getType());
				log.info("Address email = " + email);
				return null;
			}
		}
		
		return email;
	}
	
	
	private String getPlainTextContent(Part p) throws Exception {
		if (p.isMimeType("text/plain")) {
			return p.getContent().toString();
		} else {
			if (p.getContent() instanceof MimeMultipart) {
				MimeMultipart mp = (MimeMultipart) p.getContent();
				for (int i = 0; i < mp.getCount(); i++) {
					BodyPart bp = mp.getBodyPart(i);
					if (bp.isMimeType("text/plain")) {
						return bp.getContent().toString();
					}
				}
			}
		}
		
		return "";
	}
	
	
	/**
	 * 
	 * @return
	 */
	public List<String> getAdminMessages() {
		List<String> out = new ArrayList<String>();
		JavaEmailReader gmail = JavaEmailReader.getInstance();
		try {
			gmail.connect();
			gmail.openFolder("INBOX");
			int newMessages = gmail.getNewMessageCount();
			
			if (newMessages > 0) {
				Message[] msgs = getUnreadMessages();
				
				for (Message m : msgs) {
					
					String subject = m.getSubject();
					String content = getPlainTextContent(m);
					String sender = getAuthorizedSender(m);
					if (sender == null) {
						// To mark the email as "read", the content must be accessed.
						continue;
					}

					log.info("Admin action : " + subject);
					log.info("Admin param : " + content);
					out.add(sender);
					out.add(subject);
					out.add(content);
				}
				// Mark as read all the unread messages
				folder.setFlags(msgs, new Flags(Flags.Flag.SEEN), true);

			} else {
//				log.debug("No new message");
			}
			gmail.closeFolder();
			gmail.disconnect();

		} catch (Exception e) {
			log.error("Error : {}", e);
		}
		return out;
	}



	public static void main(String[] args) {
		JavaEmailReader gmail = JavaEmailReader.getInstance();
		List<String> lst = gmail.getAdminMessages();
		log.debug(lst);
//		try {
//
//			JavaEmailReader gmail = JavaEmailReader.getInstance();
//			gmail.connect();
//			gmail.openFolder("INBOX");
//
//			int totalMessages = gmail.getMessageCount();
//			int newMessages = gmail.getNewMessageCount();
//
//			log.debug("Total messages = " + totalMessages);
//			log.debug("New messages = " + newMessages);
//			log.debug("-------------------------------");
//
//			gmail.printAllUnreadMessages();
//			gmail.disconnect();
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			System.exit(-1);
//		}

	}

}

/*
 * 
 * 
 * -------------------------- MESSAGE #1: UserFlags :
 * 
 * FROM: Florent FRADET <flohjk@gmail.com> TO: hkrpiserris@gmail.com SUBJECT:
 * TEST SendDate: Tue Oct 07 09:13:20 CEST 2014 CONTENT-TYPE:
 * multipart/ALTERNATIVE; boundary=001a11394d60245d650504cfefa6 Mime Multipart :
 * 2 preamble = null BodyPart #0 CONTENT-TYPE : TEXT/PLAIN; charset=UTF-8
 * DISPOSITION : null DESCRIPTION : null FILENAME : null CONTENT : titi toto
 * tata
 * 
 * BodyPart #1 CONTENT-TYPE : TEXT/HTML; charset=UTF-8 DISPOSITION : null
 * DESCRIPTION : null FILENAME : null CONTENT : <div dir="ltr"><div>titi
 * toto<br></div>tata<br></div>
 * 
 * ---------------------------
 */
