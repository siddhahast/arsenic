package com.util.log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.FileAppender;
import org.apache.log4j.spi.LoggingEvent;

public class DailyRollingAppender extends FileAppender
{

	private static Object LOCK = new Object();
	
	private String fileDatePattern = "'.'yyyy-MM-dd";
	private String fileSuffix = ".log";
	
	private SimpleDateFormat dateFormat;
	
	private String fileBase;
	
	public void activateOptions()
	{
		
		this.dateFormat = new SimpleDateFormat(fileDatePattern);
		this.fileName = getTodayFile();
		
		super.activateOptions();
	}
	
	protected String getTodayFile()
	{
		return fileBase + dateFormat.format(new Date()) + fileSuffix;
	}
	
	protected boolean isActivationNeeded(String todayFile)
	{
		if(todayFile.equals(fileName))
		{
			return false;
		}
		
		File file = new File(todayFile);
		
		if(!file.exists())
		{
			synchronized (LOCK) {
			
				if(!file.exists())
				{
					try
					{
						boolean fileCreated = file.createNewFile();
						// log this file created variable somewhere !!!
					}
					catch(IOException ex)
					{
						// somewhere log it ..!!
					}
				}
			}
		}
		
		if(!file.canWrite())
		{
			
			return false;
		}
		
		return true;
	}
	
	protected void subAppend(LoggingEvent event)
	{
		String todayFile = getTodayFile();
		
		if(isActivationNeeded(todayFile))
		{
			this.fileName = todayFile;
			activateOptions();
		}
		
		super.subAppend(event);
	}

	public String getFileDatePattern() {
		return fileDatePattern;
	}

	public void setFileDatePattern(String fileDatePattern) {
		this.fileDatePattern = fileDatePattern;
	}

	public String getFileSuffix() {
		return fileSuffix;
	}

	public void setFileSuffix(String fileSuffix) {
		this.fileSuffix = fileSuffix;
	}

	public String getFileBase() {
		return fileBase;
	}

	public void setFileBase(String fileBase) {
		this.fileBase = fileBase;
	}
	
	
}
