package com.observer;


public interface Observer<O, P> extends BaseObserver<O, P>
{
    public void update(O observable, P parameter);

}
