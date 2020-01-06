package com.database;

public enum DbConnectionPool
{

    MASTER("master"), READ_REPLICA("read");

    private String name;

    private DbConnectionPool(String name)
    {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
