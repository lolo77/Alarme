package com.alarme.core.io;

/**
 * 
 * @author ffradet
 *
 */
public enum ELed {
	GREEN(2), ORANGE(1), RED(0);

	private int index;

	private ELed(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}
}