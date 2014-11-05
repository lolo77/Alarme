package com.alarme.core.io;

import com.alarme.core.IStoppable;


/**
 * 
 * @author ffradet
 * 
 */
public interface IIoManager extends IStoppable {

	public long getRunPeriodDuration();
	
	
	public boolean isAllDown();
	
	
	public boolean isOpenDoor();


	public boolean isOpenWin();


	public int getSensorCount();


	public boolean getSensor(int index);


	public boolean isInputCode();


	public boolean isAlimSecteur();


	public boolean changeCode(String newCode);


	public String getKeyboardInput();


	public void setLed(boolean enable, ELed led);


	public void setAlarm(boolean enable);


	public void setAudioAmpli(boolean enable);


	public void refreshInputs();

	public String computePasswordRequestedCode(String sender);
}
