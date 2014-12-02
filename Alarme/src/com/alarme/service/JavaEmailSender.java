package com.alarme.service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;

import com.alarme.core.conf.ConfigRepository;
import com.alarme.service.MessageQueue.MessageContent;


/**
 * Based on internet example of javax.mail
 * Acces SMTP au serveur GMAIL
 * 
 * @author unknown, ffradet
 *
 */
public class JavaEmailSender {

	private static final Logger log = Logger.getLogger(JavaEmailSender.class);

	private static JavaEmailSender instance = null;
	/**
	 * 
	 */
	private JavaEmailSender() {
		
	}

	/**
	 * 
	 * @return
	 */
	public static JavaEmailSender getInstance() {
		//
		if (instance == null) {
			instance = new JavaEmailSender();
		}
		return instance;
	}

	
	/**
	 * 
	 * @param msg
	 * @throws MessagingException
	 */
	public void sendEmailMessage(MessageContent msg) throws MessagingException {

		Properties propsConfig = ConfigRepository.getInstance().getProperties();

		// Create email sending properties
		Properties props = new Properties();
		props.put("mail.smtp.auth", propsConfig.getProperty(ConfigRepository.KEY_MAIL_AUTH));
		props.put("mail.smtp.starttls.enable", propsConfig.getProperty(ConfigRepository.KEY_MAIL_TLS_ENABLE));
		props.put("mail.smtp.host", propsConfig.getProperty(ConfigRepository.KEY_MAIL_HOST));
		props.put("mail.smtp.port", propsConfig.getProperty(ConfigRepository.KEY_MAIL_PORT));

		final String user = propsConfig.getProperty(ConfigRepository.KEY_MAIL_USER);
		final String pass = propsConfig.getProperty(ConfigRepository.KEY_MAIL_PASS);
		//
		if (user == null) {
			log.warn("No mail sent : mail.smtp.user is null.");
			return;
		}

		// System.setProperty("socksProxyHost", "");
		// System.setProperty("socksProxyPort", "");
		// System.setProperty("java.net.socks.username", "");
		// System.setProperty("java.net.socks.password", "");

		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(user, pass);
					}
				});

		// Create a default MimeMessage object.
		MimeMessage message = new MimeMessage(session);

		// Set From: header field of the header.
		message.setFrom(new InternetAddress("raspi"));

		// Set To: header field of the header.
		String[] recipients = msg.getEmailAddressTo().split(",");
		//
		for (String dest : recipients) {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(
					dest.trim()));
		}

		SimpleDateFormat sf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date d = new Date(msg.getTime());
		
		// Set Subject: header field
		message.setSubject(msg.getMsgSubject() + " " + sf.format(d));

		// Create the message part
		BodyPart messageBodyPart = new MimeBodyPart();

		// Fill the message and tag it with a timestamp
		String desc = msg.getMsgText().replace("\n", "<br/>");
		messageBodyPart.setContent(desc + " [" + sf.format(d) + "]", "text/html; charset=utf-8");
		
		// Create a multipart message
		Multipart multipart = new MimeMultipart();
		
		// Set text message part
		multipart.addBodyPart(messageBodyPart);

		for (String file : msg.getFiles()) {
			// Part two is attachment
			messageBodyPart = new MimeBodyPart();
			DataSource source = new FileDataSource(file);
			messageBodyPart.setDataHandler(new DataHandler(source));
			File f = new File(file);
			messageBodyPart.setFileName(f.getName());
			multipart.addBodyPart(messageBodyPart);
		}

		// Send the complete message parts
		message.setContent(multipart);

		// throw new MessagingException("bidon");

		log.debug("Sending : " + msg.toString());
		// Send message
		Transport.send(message);
		log.debug("Message EMAIL sent successfully!");

	}
}
