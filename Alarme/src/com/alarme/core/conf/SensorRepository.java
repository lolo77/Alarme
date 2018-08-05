package com.alarme.core.conf;

import com.alarme.core.conf.Sensor.EType;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.Map.Entry;

/**
 * 
 * @author ffradet
 * 
 */
public class SensorRepository {

	private static final Logger log = Logger.getLogger(SensorRepository.class);
	
	private List<Sensor> lstSensors = null;
	
	private Map<EType, List<Sensor>> mapSensors = new HashMap<EType, List<Sensor>>();

	private static SensorRepository instance = null;

	/**
	 * 
	 */
	private SensorRepository() {
		reload();
	}

	/**
	 * 
	 * @return
	 */
	public static SensorRepository getInstance() {
		//
		if (instance == null) {
			instance = new SensorRepository();
		}

		return instance;
	}

	/**
	 * 
	 */
	private void reload() {
		log.debug("SensorRepository.reload");
		lstSensors = null;
		ConfigRepository conf = ConfigRepository.getInstance();
		//
		if (!conf.isLoaded()) {
			return;
		}

		lstSensors = new ArrayList<Sensor>();

		Properties props = conf.getProperties();
		//
		for (Entry<Object, Object> item : props.entrySet()) {
			String sKey = (String) item.getKey();
			String sValue = (String) item.getValue();
//			Logger.log("config : key = " + sKey + " ; value = " + sValue);
			//
			if (sKey.startsWith(ConfigRepository.KEY_SENSOR_PREFIX)) {
				//
				try {
					String sPort = sKey.substring(ConfigRepository.KEY_SENSOR_PREFIX.length());
					int iPort = Integer.parseInt(sPort);

					String[] tabParams = sValue.split(",");
					//
					if (tabParams.length != 2) {
						log.debug("Config error ; too few parameters : " + sValue);
						continue;
					}

					EType type = EType.valueOf(tabParams[0].trim());
					String desc = tabParams[1].trim();

					Sensor sensor = new Sensor(iPort, type, desc);
					log.debug("adding Sensor : " + sensor);
					lstSensors.add(sensor);
					List<Sensor> lst = mapSensors.get(type);
					//
					if (lst == null) {
						lst = new ArrayList<Sensor>();
						mapSensors.put(type, lst);
					}
					lst.add(sensor);
				}
				catch (Exception e) {
					log.debug("Config error : " + e.getMessage());
				}
			}
		}
		log.debug("SensorRepository.reload END");
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public List<Sensor> getByType(EType type) {
		List<Sensor> lst = mapSensors.get(type);
		return lst;
	}
	
	/**
	 * 
	 * @param port
	 * @return
	 */
	public Sensor getByPort(int port) {
		//
		for (Sensor item : lstSensors) {
			//
			if (item.getPort() == port) {
				return item;
			}
		}
		return null;
	}
}
 