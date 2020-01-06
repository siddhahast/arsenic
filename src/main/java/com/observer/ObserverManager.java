package com.observer;

import java.util.Collection;
import java.util.List;

public interface ObserverManager
{
    public ConsumedEvents consume(Collection<ObservableEvent<?, ?>> events, Throwable exception, ObserverPhase phase);
    public void registerObserver(BaseObserver<?, ?> observer);
}
