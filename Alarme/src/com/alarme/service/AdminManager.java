package com.alarme.service;

import com.alarme.core.conf.ConfigRepository;
import com.alarme.core.conf.RecipientInfo;
import com.alarme.core.conf.Sensor;
import com.alarme.core.conf.SensorRepository;
import com.alarme.core.io.IIoManager;
import com.alarme.service.MessageQueue.EMedia;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 
 * @author ffradet
 *
 */
public class AdminManager implements Runnable {

	private static final Logger log = Logger.getLogger(AdminManager.class);
	private static final long DELAY = 5 * 60 * 1000;

	private static AdminManager instance = null;

	private Thread adminThread;
	private IIoManager ioManager = null;

	public interface AdminActions {
		public interface Password {
			public static final String ACTION = "PWD";
			public static final String PARAM_OLD = "OLD";
			public static final String PARAM_NEW = "NEW";
			public static final String PARAM_LOST = "LOST";
		}

		public interface Email {
			public static final String ACTION = "EMAIL";
			public static final String PARAM_ADD = "ADD";
			public static final String PARAM_REMOVE = "REMOVE";
			public static final String PARAM_LIST = "LIST";
		}

		public interface Admin {
			public static final String ACTION = "ADMIN";
			public static final String PARAM_QUIET = "QUIET"; // shut down the alarm
															// sound if playing
			public static final String PARAM_LOGS = "LOGS";
			public static final String PARAM_SENSORS = "SENSORS";
		}
	}

	/**
	 * 
	 * @return
	 */
	public static AdminManager getInstance() {

		if (instance == null) {
			instance = new AdminManager();
			instance.start();
		}

		return instance;
	}

	private AdminManager() {
		adminThread = new Thread(this);
	}

	/**
	 * 
	 * @param ioManager
	 */
	public void bindIoManager(IIoManager ioManager) {
		this.ioManager = ioManager;
	}

	/**
	 * 
	 */
	private void start() {
		adminThread.start();
	}

	@Override
	public void run() {
		// Let the time to the other threads to be started
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			log.error("Thread interrupted");
			return;
		}
		while (true) {

			List<String> adminActions = JavaEmailReader.getInstance()
					.getAdminMessages();
//			log.debug("Admin action : " + adminActions);

			for (int i = 0; i < adminActions.size(); i++) {
				String sender = adminActions.get(i++);
				String action = adminActions.get(i++);
				String params = adminActions.get(i);

				manageAction(sender, action, params);
			}

			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	public void stop() {
		adminThread.interrupt();
	}

	/**
	 * 
	 * @param sender
	 * @param action
	 * @param params
	 */
	private void manageAction(String sender, String action, String params) {
		log.debug("manageAction : sender = " + sender + ", action = " + action + ", params = " + params);
		RecipientInfo senderUser = ConfigRepository.getInstance().getRecipientInfoByEmail(sender);
		if (senderUser == null) {
			log.warn("Sender " + sender + " not recognized as regular user for action " + action);
			return;
		}
		Properties p = new Properties();
		try {
			p.load(new StringReader(params));
		} catch (IOException e) {
			log.error("Unhandled param format : " + params);
			return;
		}
		if (AdminActions.Password.ACTION.equals(action)) {
			managePassword(sender, p);
		}
		else
		if (AdminActions.Email.ACTION.equals(action)) {
			manageEmail(sender, p);
		}
		else
		if (AdminActions.Admin.ACTION.equals(action)) {
			manageAdmin(sender, p);
		}
		else {
			// Send help message
			Properties props = ConfigRepository.getInstance().getProperties();
			MessageQueue.getInstance().createAndPushMessageTo(Arrays.asList(senderUser), "Mode d'emploi", "Vous trouverez ci-joint le descriptif de votre système d'alarme.", EMedia.EMAIL, "res/alarme.pdf");
		}
	}

	/**
	 * 
	 * @param sender
	 * @param p
	 */
	private void managePassword(String sender, Properties p) {

		RecipientInfo senderUser = ConfigRepository.getInstance().getRecipientInfoByEmail(sender);
		if (senderUser == null) {
			log.warn("Sender " + sender + " not recognized as regular user for action managePassword.");
			return;
		}

		Properties props = ConfigRepository.getInstance().getProperties();

		String sOld = p.getProperty(AdminActions.Password.PARAM_OLD);
		String sNew = p.getProperty(AdminActions.Password.PARAM_NEW);
		String sLost = p.getProperty(AdminActions.Password.PARAM_LOST);

		// Change the current pass
		if ((sOld != null) && (sNew != null) && (!sNew.isEmpty())
				&& (!sNew.equals(sOld))) {
			if (sOld.equals(props.getProperty(ConfigRepository.KEY_MAIL_PASS))) {
				log.info("PWD : changed to " + sNew);
				props.setProperty(ConfigRepository.KEY_MAIL_PASS, sNew);
				ConfigRepository.getInstance().save();
			} else {
				log.info("PWD : old pass is incorrect : " + sOld);
			}
		}

		// Send the current pass by email back to the sender
		if (sLost != null) {
			log.debug("PWD : sending pass");
			String code = ioManager.computePasswordRequestedCode(sender);
			MessageQueue.getInstance().createAndPushMessageTo(Arrays.asList(senderUser), "RE : PWD", "Saisissez le code suivant sur le clavier du système d'alarme : " + code, EMedia.EMAIL);
		}
	}

	/**
	 * 
	 * @param sender
	 * @param p
	 */
	private void manageEmail(String sender, Properties p) {
		RecipientInfo senderUser = ConfigRepository.getInstance().getRecipientInfoByEmail(sender);
		if (senderUser == null) {
			log.warn("Sender " + sender + " not recognized as regular user for action manageEmail.");
			return;
		}
/*
		Properties props = ConfigRepository.getInstance().getProperties();

		String sAdd = p.getProperty(AdminActions.Email.PARAM_ADD);
		String sRem = p.getProperty(AdminActions.Email.PARAM_REMOVE);
		String sLst = p.getProperty(AdminActions.Email.PARAM_LIST);

		boolean bList = false;

		// Add mail recipients (comma-separated)
		if ((sAdd != null) && (!sAdd.isEmpty())) {
			String cur = props
					.getProperty(ConfigRepository.KEY_MAIL_RECIPIENTS);
			List<String> lstCur = new ArrayList<String>(Arrays.asList(cur
					.split("\\s*,\\s*")));
			String[] tabAdd = sAdd.split("\\s*,\\s*");
			for (String s : tabAdd) {
				if (!lstCur.contains(s)) {
					lstCur.add(s);
					MessageQueue.getInstance().createAndPushMessageTo(s, "EMAIL", "Votre adresse a été ajoutée à la liste des destinataires.",
							EMedia.EMAIL);
				}
			}
			cur = "";
			for (String s : lstCur) {
				if (!cur.isEmpty()) {
					cur += ", ";
				}
				cur += s;
			}
			props.setProperty(ConfigRepository.KEY_MAIL_RECIPIENTS, cur);
			bList = true;
		}

		// Remove mail recipients (comma-separated)
		if ((sRem != null) && (!sRem.isEmpty())) {
			String cur = props
					.getProperty(ConfigRepository.KEY_MAIL_RECIPIENTS);
			String[] tabCur = cur.split("\\s*,\\s*");
			List<String> lstCur = new ArrayList<String>(Arrays.asList(tabCur));
			String[] tabRem = sRem.split("\\s*,\\s*");
			for (String s : tabRem) {
				int i = lstCur.indexOf(s);
				if (i >= 0) {
					lstCur.remove(i);
					MessageQueue.getInstance().createAndPushMessageTo(s, "EMAIL", "Votre adresse a été retirée de la liste des destinataires.",
							EMedia.EMAIL);
				}
			}
			cur = "";
			for (String s : lstCur) {
				if (!cur.isEmpty()) {
					cur += ", ";
				}
				cur += s;
			}
			props.setProperty(ConfigRepository.KEY_MAIL_RECIPIENTS, cur);
			bList = true;
		}

		if (bList) {
			ConfigRepository.getInstance().save();
		}

		// List mail recipients (comma-separated)
		if ((bList) || ((sLst != null) && (!sLst.isEmpty()))) {
			String cur = props
					.getProperty(ConfigRepository.KEY_MAIL_RECIPIENTS);
			MessageQueue.getInstance().createAndPushMessageTo(sender, "RE : EMAIL", cur,
					EMedia.EMAIL);
		}
*/
	}

	/**
	 * 
	 * @param sender
	 * @param p
	 */
	private void manageAdmin(String sender, Properties p) {

		RecipientInfo senderUser = ConfigRepository.getInstance().getRecipientInfoByEmail(sender);
		if (senderUser == null) {
			log.warn("Sender " + sender + " not recognized as regular user for action manageAdmin.");
			return;
		}

		String sQuiet = p.getProperty(AdminActions.Admin.PARAM_QUIET);
		String sLogs = p.getProperty(AdminActions.Admin.PARAM_LOGS);
		String sSensors = p.getProperty(AdminActions.Admin.PARAM_SENSORS);

		if (sQuiet != null) {
			log.info("Alarm is quiet");
			ioManager.setAlarm(false);
		}

		if (sLogs != null) {
			log.info("Sending logs");
			MessageQueue.getInstance().createAndPushMessageTo(Arrays.asList(senderUser), "RE : ADMIN", "",
					EMedia.EMAIL, "log.txt");
		}

		if (sSensors != null) {
			log.info("Sending sensors status");
			String s = "";
			int iSensorCount = ioManager.getSensorCount();
			//
			for (int i = 0; i < iSensorCount; i++) {
				//
				boolean b = ioManager.getSensor(i);
				Sensor sensor = SensorRepository.getInstance().getByPort(i);
				//
				if (sensor != null) {
					s += sensor.getDescription();
					s += " : ";
					s += b ? "OUVERT" : "FERME";
					s += "\n";
				}
			}
			MessageQueue.getInstance().createAndPushMessageTo(Arrays.asList(senderUser), "RE : ADMIN", s,
					EMedia.EMAIL);
		}
	}

	public static void main(String args[]) {
//		AdminManager m = AdminManager.getInstance();
//		MessageQueue.getInstance(); // start the instance
		// m.manageAction("PWD", "OLD=totojaja123\nNEW=tititata789");
		// m.manageAction("PWD", "LOST");
//		m.manageAction("EMAIL",
//				"ADD=a@b.c,flohjk@gmail.com\nREMOVE=a@b.c,g@h.k");
		
//		 m.manageAction("flohjk@gmail.com", "HELP", "");
		String s = "*" + String
				.valueOf((int) (Math.random() * 9000 + 1000)) + "#";
		log.debug("s = "  + s);
		try {
			log.debug("URL Encode = " + URLEncoder.encode("été", "UTF-8"));
		} catch (UnsupportedEncodingException e) {

		}
//		try {
//			Thread.sleep(15000);
//		} catch (InterruptedException e) {
//		}
//		m.stopThread();
//		log.debug("stopThread");
//		MessageQueue.getInstance().stopThread();
	}
}
