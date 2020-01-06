package com.observer;

public class ObservableEvent<O, P> {

	private O observable;
	private P parameter;
	
	public ObservableEvent(O observable, P parameter)
	{
		this.observable = observable;
		this.parameter = parameter;
	}
	
	public ObservableEvent<O, P> setParameter(P paramter)
	{
		this.parameter = paramter;
		return this;
	}
	
	public ObservableEvent<O, P> setObservable(O observable)
	{
		this.observable = observable;
		return this;
	}
	
	public P getParameter()
	{
		return parameter;
	}
	
	public O getObservable()
	{
		return observable;
	}
	
}
