package com.alarme.core.conf;

/**
 * 
 * @author ffradet
 * 
 */
public class Sensor {

	


	/**
	 * 
	 * @author ffradet
	 * 
	 */
	public static enum EType {
		ENTRY, WINDOW;
	};

	private int		port;
	private EType	type;
	private String	description;


	/**
	 * 
	 * @param pin
	 * @param description
	 */
	public Sensor(int port, EType type, String description) {
		super();
		this.port = port;
		this.type = type;
		this.description = description;
	}


	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}


	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * @return the type
	 */
	public EType getType() {
		return type;
	}

	
	@Override
	public String toString() {
		return "Sensor [port=" + port + ", type=" + type + ", description="
				+ description + "]";
	}
}
