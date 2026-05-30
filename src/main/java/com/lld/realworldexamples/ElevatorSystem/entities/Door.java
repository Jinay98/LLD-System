package com.lld.realworldexamples.ElevatorSystem.entities;

import com.lld.realworldexamples.ElevatorSystem.enums.DoorState;

class Door {
    private DoorState state;

    public Door() {
        this.state = DoorState.CLOSED;
    }

    public synchronized void open() {
        this.state = DoorState.OPEN;
    }

    public synchronized void close() {
        this.state = DoorState.CLOSED;
    }

    public synchronized boolean isOpen() {
        return state == DoorState.OPEN;
    }

    public synchronized DoorState getState() {
        return state;
    }
}
