package com.observer;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


public class ObserverClass<O, P> {
    private BaseObserver<O, P> observer;
    private Class clazz;

    public ObserverClass(BaseObserver<O, P> observer, Class clazz) {
        this.observer = observer;
        this.clazz = clazz;
    }

    public BaseObserver<O, P> getObserver() {
        return observer;
    }
    public void setObserver(BaseObserver<O, P> observer) {
        this.observer = observer;
    }
    public Class getClazz() {
        return clazz;
    }
    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(clazz);
        builder.append(observer);
        return builder.toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj.getClass() != this.getClass()) {
            return false;
        }
        EqualsBuilder builder = new EqualsBuilder();
        ObserverClass other = (ObserverClass) obj;
        builder.append(this.getClazz(), other.getClazz());
        builder.append(this.getObserver(), other.getObserver());
        return builder.isEquals();
    }

}
