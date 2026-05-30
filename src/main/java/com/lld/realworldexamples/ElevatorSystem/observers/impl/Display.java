package com.lld.realworldexamples.ElevatorSystem.observers.impl;

import com.lld.realworldexamples.ElevatorSystem.enums.Direction;
import com.lld.realworldexamples.ElevatorSystem.observers.ElevatorObserver;

public class Display implements ElevatorObserver {
    private final int elevatorId;
    private int currentFloor;
    private Direction currentDirection;

    public Display(int elevatorId) {
        this.elevatorId = elevatorId;
        this.currentFloor = 1;
        this.currentDirection = Direction.IDLE;
    }

    @Override
    public void onElevatorStateChanged(int elevatorId, int floor, Direction direction) {
        // Only update if this notification is for our elevator
        if (this.elevatorId == elevatorId) {
            this.currentFloor = floor;
            this.currentDirection = direction;
            show();
        }
    }

    public void show() {
        System.out.println("Display [Elevator " + elevatorId + "]: Floor " +
                currentFloor + " | Direction: " + currentDirection);
    }

    public int getElevatorId() {
        return elevatorId;
    }
}
