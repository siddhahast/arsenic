package com.common.def;

import java.util.Collection;
import java.util.List;

public enum ThreadLocalKey 
{
	
	OBSERVABLE_EVENT(List.class);
	
	private Class<?> valueType;

	private <X> ThreadLocalKey(Class<X> valueType)
	{
		this.valueType = valueType;
	}

	public Class<?> getValueType() 
	{
		return valueType;
	}

	public void setValueType(Class<?> valueType) 
	{
		this.valueType = valueType;
	}
	
	public <X> void addToList(X value)
	{
		
	}
	
	private <X> void addToList(X value, Class collectionClass)
	{
		
	}

}
