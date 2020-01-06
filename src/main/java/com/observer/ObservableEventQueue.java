package com.observer;

import com.common.def.ThreadLocalKey;

import java.util.List;

public class ObservableEventQueue 
{

	public static void push(Object observable, Object parameter)
	{
		ThreadLocalKey.OBSERVABLE_EVENT.addToList(new ObservableEvent<>(observable, parameter));
	}

	public static void consume(ObserverManager manager, ObserverPhase phase)
	{
		Throwable exception = ThreadLocalKey.SERVICE_EXCEPTION.get();
		List<ObservableEvent<?, ?>> events = ThreadLocalKey.OBSERVABLE_EVENT.get();

		manager.consume(events, exception, phase);
	}
}
