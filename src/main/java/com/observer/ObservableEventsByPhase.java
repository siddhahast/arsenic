package com.observer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ObservableEventsByPhase
{
    private Map<InvokedObserver, Set<ObservableEvent<?, ?>>> phaseCache;

    public ObservableEventsByPhase()
    {
        phaseCache = new ConcurrentHashMap<>();
    }

    public boolean isInvoked(BaseObserver<?, ?> observer, ObserverPhase phase, ObservableEvent<?, ?> event)
    {
        InvokedObserver invokedObserver = new InvokedObserver(observer, phase);
        return phaseCache.get(invokedObserver) != null && phaseCache.get(invokedObserver).contains(event);
    }

    public boolean addInvoked(BaseObserver<?, ?> observer, ObserverPhase phase, ObservableEvent<?, ?> event)
    {
        InvokedObserver invokedObserver = new InvokedObserver(observer, phase);
        if (phaseCache.get(invokedObserver) == null) {
            phaseCache.put(invokedObserver, ConcurrentHashMap.newKeySet());
        }
        return phaseCache.get(invokedObserver).add(event);
    }

    public class InvokedObserver {
        private BaseObserver<?, ?> observer;
        private ObserverPhase phase;

        public InvokedObserver(BaseObserver<?, ?> observer, ObserverPhase phase) {
            this.observer = observer;
            this.phase = phase;
        }

        public BaseObserver<?, ?> getObserver() {
            return observer;
        }
        public void setObserver(BaseObserver<?, ?> observer) {
            this.observer = observer;
        }
        public ObserverPhase getPhase() {
            return phase;
        }
        public void setPhase(ObserverPhase phase) {
            this.phase = phase;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((observer == null) ? 0 : observer.hashCode());
            result = prime * result + ((phase == null) ? 0 : phase.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            InvokedObserver other = (InvokedObserver) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (observer == null) {
                if (other.observer != null)
                    return false;
            } else if (!observer.equals(other.observer))
                return false;
            if (phase != other.phase)
                return false;
            return true;
        }

        private ObservableEventsByPhase getOuterType() {
            return ObservableEventsByPhase.this;
        }

    }
}
