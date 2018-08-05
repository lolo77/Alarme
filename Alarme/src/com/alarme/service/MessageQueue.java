package com.alarme.service;

import com.alarme.core.conf.ConfigRepository;
import com.alarme.core.conf.RecipientInfo;
import com.alarme.service.SmsSender.SmsException;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;

/**
 * @author ffradet
 */
public class MessageQueue implements Runnable {

    private static final Logger log = Logger.getLogger(MessageQueue.class);

    /**
     * @author ffradet
     */
    public static enum EMedia {
        EMAIL, SMS, BOTH
    }


    /**
     * @author ffradet
     */
    public static class MessageContent implements Serializable {
        private static final long serialVersionUID = -8368356579883182140L;

        private EMedia media;



        private List<RecipientInfo> lstRecipients;
        private String msgSubject;
        private String msgText;
        private String[] files;
        private long time;

        /**
         * @param msgSubject
         * @param msgText
         * @param files
         */
        public MessageContent(List<RecipientInfo> lstRecipients, String msgSubject, String msgText, String[] files, long time, EMedia media) {
            super();
            this.lstRecipients = lstRecipients;
            this.msgSubject = msgSubject;
            this.msgText = msgText;
            this.files = files;
            this.time = time;
            this.media = media;
        }

        public long getTime() {
            return time;
        }

        public List<RecipientInfo> getRecipients() {
            return lstRecipients;
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
            return "MessageContent [media=" + media + ", msgSubject=" + msgSubject + ", recipients.size=" + ((lstRecipients != null) ? lstRecipients.size() : "N/A")
                    + ", msgText=" + msgText + ", files="
                    + Arrays.toString(files) + ", time=" + time + "]";
        }
    }


    private static final long DELAY_SLEEP_DAEMON = 1000; // 1s
    private static final String CACHE_PATH = "res/temp.dat";
    private static final long DELAY_RETRY = 1000 * 60 * 5; // 5min

    private static MessageQueue instance = null;

    private Thread managerThread = null;
    private List<MessageContent> lstCache = Collections.synchronizedList(new ArrayList<MessageContent>());
    private long lastError = 0;
    private String ip;

    /**
     * @return
     */
    public static MessageQueue getInstance() {
        //
        if (instance == null) {
            instance = new MessageQueue();
            instance.startThread();
        }
        return instance;
    }

    /**
     *
     */
    private MessageQueue() {
        detectIpAddress();

        readCache();
    }

    /**
     *
     */
    private void startThread() {
        managerThread = new Thread(this);
        managerThread.start();
    }

    /**
     *
     */
    @Override
    public void run() {

        while (true) {

            try {
                instance.manage();
            } catch (Exception e) {
                log.error("manage Exception : " + e.getMessage());
            }

            try {
                Thread.sleep(DELAY_SLEEP_DAEMON);
            } catch (InterruptedException e) {
                break;
            }
        }
        log.debug("MessageQueue.run() interrupted.");
    }

    /**
     *
     */
    public void detectIpAddress() {
        ip = "";
        //
        try {
            Enumeration<NetworkInterface> e = NetworkInterface
                    .getNetworkInterfaces();
            //
            while (e.hasMoreElements()) {
                NetworkInterface n = e.nextElement();
                Enumeration<InetAddress> ee = n.getInetAddresses();
                //
                while (ee.hasMoreElements()) {
                    InetAddress i = ee.nextElement();
                    String adr = i.getHostAddress();
                    log.debug("IP found : " + adr);
                    // Filters only on Local Area Network IP class
                    if (adr.startsWith("192.168.")) {
                        ip += adr + " ";
                    }
                }
            }
            log.debug("Current LAN IP is " + ip);
        } catch (Exception e) {
            log.debug("Current LAN IP is unknown");
        }
    }

    /**
     *
     */
    public void stopThread() {
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
     * @param msg
     */
    private void pushMessage(MessageContent msg) {
        log.debug("Email to be pushed : " + msg.getMsgSubject());
        lstCache.add(msg);
        writeCache();
        log.debug("Email pushed");
    }

    /**
     *
     */
    private synchronized void writeCache() {
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
    private synchronized void sendPendingMessages() {
        //
        List<MessageContent> lst = new ArrayList<MessageContent>(lstCache);
        Iterator<MessageContent> iter = lst.iterator();
        int iPreviousSize = lst.size();
        //
        while (iter.hasNext()) {
            MessageContent item = iter.next();
            //
            try {
                sendMessage(item);

                log.debug("Removing this message");
                lstCache.remove(item);
                log.debug("Message removed");

            } catch (AddressException e) {
                log.debug("sendPendingMessages MAIL : " + e.getMessage());
                iter.remove();
            } catch (SmsException e) {
                log.debug("sendPendingMessages SMS : " + e.getMessage());
                iter.remove();
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
    public void flush() {
        sendPendingMessages();
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
     * @param msgSubject
     * @param msgText
     * @param files
     */
    public void createAndPushMessage(List<RecipientInfo> lstRecipients, String msgSubject, String msgText,
                                        EMedia media, String... files) {
        log.debug("createAndPushMessage");
        ConfigRepository conf = ConfigRepository.getInstance();
        createAndPushMessageTo(lstRecipients, msgSubject + " " + ip + " ",
                msgText, media, files);
        log.debug("createAndPushMessage END");
    }

    /**
     * @param msgSubject
     * @param msgText
     * @param files
     */
    public void createAndPushMessageTo(List<RecipientInfo> lstRecipients, String msgSubject, String msgText, EMedia media, String... files) {
        log.debug("createAndPushMessageTo");

        MessageContent msg = new MessageContent(lstRecipients, msgSubject,
                msgText, files, System.currentTimeMillis(), media);
        pushMessage(msg);

        log.debug("createAndPushMessageTo END");
    }

    /**
     * @param msg
     * @throws MessagingException
     * @throws SmsException
     */
    private void sendMessage(MessageContent msg) throws MessagingException, SmsException {
        log.debug("sendMessage");
        //
        if ((msg.getMedia().equals(EMedia.EMAIL))
                || (msg.getMedia().equals(EMedia.BOTH))) {
            JavaEmailSender.getInstance().sendEmailMessage(msg);
        }
        // 27/06/2015 - Google will not support anymore SMS sending
/*        if ((msg.getMedia().equals(EMedia.SMS))
                || (msg.getMedia().equals(EMedia.BOTH))) {
            SmsSender.getInstance().sendSmsMessage(msg);
        }
*/        log.debug("sendMessage END");
    }
}
