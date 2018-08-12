package com.alarme.core.conf;

import org.apache.log4j.Logger;

import java.io.*;


/**
 * 
 * @author ffradet
 * 
 */
public class DataRepository {

	private static final Logger log = Logger.getLogger(DataRepository.class);
	
	private static final String		FILE_PATH	= "res/state.dat";

	private static DataRepository	instance	= null;

	private String					code		= "1234";			// Default secret code
	private boolean					alarmEnabled		= false;	// Alarm is disabled by default


	/**
	 * 
	 */
	private DataRepository() {
		reload();
		log.debug("SECRET CODE IS " + code);
	}


	/**
	 * 
	 * @return
	 */
	public static DataRepository getInstance() {
		if (instance == null) {
			instance = new DataRepository();
		}

		return instance;
	}


	/**
	 * 
	 */
	public boolean reload() {
		log.debug("DataRepository.reload");
		File f = new File(FILE_PATH);
		//
		try {
			BufferedInputStream s = new BufferedInputStream(
					new FileInputStream(f));
			byte[] buf = new byte[16];
			int iLen = s.read(buf);
			//
			if (iLen > 1) {

				byte[] codeBin = new byte[iLen-1];
				//
				for (int i = 0; i < iLen-1; i++) {
					codeBin[i] = (byte) (buf[i] ^ 0xff);
				}

				code = new String(codeBin);
				alarmEnabled = (buf[iLen-1] == 1);
			}
			s.close();
		}
		catch (Exception e) {
			log.debug("DataRepository.reload END KO : File not found 'res/state.dat' : defaulting to factory config.");
			return false;
		}
		log.debug("DataRepository.reload END OK");
		return true;
	}


	/**
	 * 
	 */
	public boolean save() {
		log.debug("DataRepository.save");
		File f = new File(FILE_PATH);
		//
		try {
			BufferedOutputStream s = new BufferedOutputStream(
					new FileOutputStream(f));

			byte[] buf = code.getBytes();
			int iLen = buf.length;
			//
			byte[] codeBin = new byte[iLen + 1];
			//
			for (int i = 0; i < iLen; i++) {
				codeBin[i] = (byte) (buf[i] ^ 0xff);
			}
			codeBin[iLen] = (alarmEnabled) ? (byte) 1 : (byte) 0;
			s.write(codeBin);
			s.close();
		}
		catch (Exception e) {
			log.debug("DataRepository.save : END KO");
			e.printStackTrace();
			return false;
		}
		log.debug("DataRepository.save END OK");
		return true;
	}


	/**
	 * 
	 * @param newCode
	 */
	public void setCode(String newCode) {
		code = newCode;
	}


	/**
	 * 
	 * @return
	 */
	public String getCode() {
		return code;
	}


	/**
	 * @return the enabled
	 */
	public boolean isAlarmEnabled() {
		return alarmEnabled;
	}


	/**
	 * @param enabled
	 *            the enabled to set
	 */
	public void setAlarmEnabled(boolean enabled) {
		//
		if (this.alarmEnabled != enabled) {
			this.alarmEnabled = enabled;
			save();
		}
	}
}
