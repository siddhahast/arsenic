package com.observer;

import java.util.function.Predicate;

public class DumbPredicate<T> implements Predicate<T>
{
    public static DumbPredicate TRUE = new DumbPredicate<>(true);
    public static DumbPredicate FALSE = new DumbPredicate<>(false);

    private boolean result;

    public DumbPredicate(boolean result)
    {
        this.result = result;
    }

    @Override
    public boolean test(T t)
    {
        return result;
    }

}