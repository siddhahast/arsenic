package com.observer;


import java.util.Comparator;
import java.util.function.Predicate;


public interface BaseObserver<O, P> {

    public Class<O> getObservableClass();

    default ObserverPhase getPhase()
    {
        return ObserverPhase.POST_REQUEST;
    }

    default boolean isThrowExceptions()
    {
        return false;
    }

    default boolean doesAllowSubclasses()
    {
        return false;
    }

    default Comparator<ObservableEvent> getEventEqualityComparator()
    {
        return null;
    }

    default Predicate<Throwable> getExceptionPredicate()
    {
        return DumbPredicate.FALSE;
    }

    default Predicate<O> getObservablePredicate()
    {
        return DumbPredicate.TRUE;
    }

    default Predicate<P> getParameterPredicate()
    {
        return DumbPredicate.TRUE;
    }

    default boolean isBypassReadReplica() {
        return true;
    }

    // Override this method to Mode.BACKGROUND in async observers
    default Mode getMode()
    {
        return Mode.SAME_THREAD;
    }

    // timeout for async observers
    default String getTimeout()
    {
        return null;
    }

    // order to run observers in
    default Integer getOrder() {
        return 1;
    }
}