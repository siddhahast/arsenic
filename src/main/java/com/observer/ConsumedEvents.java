package com.observer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.observer.BaseObserver;
import com.observer.ObservableEvent;

public class ConsumedEvents
{
    private Map<Class<? extends BaseObserver>, List<ObservableEvent<?, ?>>> eventsByObserverClass;
    private Map<ObservableEvent, List<BaseObserver<?, ?>>> observersByEvent;

    public ConsumedEvents()
    {
        eventsByObserverClass = new HashMap<>();
        observersByEvent = new HashMap<>();
    }

    public <O, P> boolean addEventIfUnique(BaseObserver observer, ObservableEvent<O, P> event)
    {
        List<ObservableEvent<?, ?>> eventsByObserverClass = getEventsByObserverClass(observer);
        boolean added = addEventIfUnique(observer.getEventEqualityComparator(), event, eventsByObserverClass);
        if (added)
        {
            getObserversByEvent(event).add(observer);
        }
        return added;
    }

    private boolean addEventIfUnique(Comparator<ObservableEvent> comparator, ObservableEvent<?, ?> event, List<ObservableEvent<?, ?>> eventsByObserverClass)
    {
        ObservableEvent<?, ?> match = null;
        boolean added = false;

        if (comparator != null)
        {
            // by default comparator is null
            // only apply custom unique logic when comparator is defined
            match = eventsByObserverClass.stream().//
                    filter(e -> comparator.compare(e, event) == 0).findFirst().orElse(null);
        }

        if (match == null)
        {
            eventsByObserverClass.add(event);
            added = true;
        }
        return added;
    }

    public List<BaseObserver<?, ?>> getObserversByEvent(ObservableEvent<?, ?> event)
    {
        List<BaseObserver<?, ?>> observers = observersByEvent.get(event);
        if (observers == null)
        {
            observersByEvent.put(event, observers = new ArrayList<>());
        }
        return observers;

    }
    private List<ObservableEvent<?, ?>> getEventsByObserverClass(BaseObserver<?, ?> observer)
    {
        Class<? extends BaseObserver> observerClass = observer.getClass();
        List<ObservableEvent<?, ?>> eventsByClass = eventsByObserverClass.get(observer.getClass());
        if (eventsByClass == null)
        {
            eventsByObserverClass.put(observerClass, eventsByClass = new ArrayList());
        }
        return eventsByClass;
    }

    public Map<Class<? extends BaseObserver>, List<ObservableEvent<?, ?>>> getEventsByObserverClass()
    {
        return eventsByObserverClass;
    }

    public Map<ObservableEvent, List<BaseObserver<?, ?>>> getObserversByEvent()
    {
        return observersByEvent;
    }

}
