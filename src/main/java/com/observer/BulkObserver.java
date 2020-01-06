package com.observer;

import java.util.List;


public interface BulkObserver<O, P> extends BaseObserver<O, P> {

    public void update(List<ObservableEvent<O, P>> events);
}