package com.observer;

import java.util.List;
import java.util.Map;

import com.ass.AssFact;
import com.common.def.ThreadLocalKey;
import com.concurrent.Job;
import org.apache.commons.collections.CollectionUtils;


public class ObserverJob<O, P> extends Job<BaseObserver<O, P>>
{
    private BaseObserver<O, P> observer;
    private List<ObservableEvent> events;

    @Override
    public BaseObserver execute()
    {
        ThreadLocalKey.BYPASS_READ_REPLICA.put(observer.isBypassReadReplica());

        if (observer instanceof Observer) {
            AssFact.BAD_ARGUMENT.isNotNull(CollectionUtils.isNotEmpty(events), "events cannot be null/empty for observer job");
            AssFact.BAD_ARGUMENT.isEqual(1, events.size(), "single event observer {" + observer
                    + "} was passed multiple events {" + events + "}");
            ObservableEvent event = events.get(0);
            ((Observer) observer).update(event.getObservable(), event.getParameter());
        } else if (observer instanceof BulkObserver) {
            ((BulkObserver) observer).update(events);
        }

        return observer;
    }

    public BaseObserver<O, P> getObserver() {
        return observer;
    }

    public ObserverJob<O, P> setObserver(BaseObserver<O, P> observer) {
        this.observer = observer;
        return this;
    }

    public List<ObservableEvent> getEvents() {
        return events;
    }

    public ObserverJob<O,P> setEvents(List<ObservableEvent> events) {
        this.events = events;
        return this;
    }

    @Override
    protected Map<String, Object> buildToStringMap()
    {
        Map<String, Object> toStringMap = super.buildToStringMap();

        toStringMap.put("events", events);
        toStringMap.put("observer", observer);

        return toStringMap;
    }
}
