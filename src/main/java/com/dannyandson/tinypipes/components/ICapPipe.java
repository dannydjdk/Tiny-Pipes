package com.dannyandson.tinypipes.components;

public interface ICapPipe<CapType> {

    public abstract int canAccept(int amount);

    public void didPush(int amount);

}
