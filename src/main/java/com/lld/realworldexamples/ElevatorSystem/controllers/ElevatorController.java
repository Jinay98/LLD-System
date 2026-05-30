package com.lld.realworldexamples.ElevatorSystem.controllers;

import com.lld.realworldexamples.ElevatorSystem.entities.Elevator;
import com.lld.realworldexamples.ElevatorSystem.enums.Direction;
import com.lld.realworldexamples.ElevatorSystem.enums.ElevatorState;

public class ElevatorController implements Runnable {
    private final Elevator elevator;
    private volatile boolean running;

    public ElevatorController(Elevator elevator) {
        this.elevator = elevator;
        this.running = true;
    }

    @Override
    public void run() {
        while (running) {
            if (elevator.hasRequests()) {
                processRequests();
            } else {
                // No requests, go idle and wait
                elevator.setDirection(Direction.IDLE);
                elevator.setState(ElevatorState.IDLE);
                try {
                    Thread.sleep(100); // Avoid busy-waiting
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public void stop() {
        this.running = false;
    }

    private void processRequests() {
        Direction dir = elevator.getDirection();

        if (dir == Direction.IDLE) {
            // Starting fresh: pick a direction based on available requests
            if (elevator.hasUpRequests()) {
                elevator.setDirection(Direction.UP);
            } else if (elevator.hasDownRequests()) {
                elevator.setDirection(Direction.DOWN);
            }
            return;
        }

        int nextStop = elevator.getNextStop();

        if (nextStop == -1) {
            // No more requests in current direction, try reversing
            if (dir == Direction.UP && elevator.hasDownRequests()) {
                elevator.setDirection(Direction.DOWN);
            } else if (dir == Direction.DOWN && elevator.hasUpRequests()) {
                elevator.setDirection(Direction.UP);
            } else {
                elevator.setDirection(Direction.IDLE);
                elevator.setState(ElevatorState.IDLE);
            }
            return;
        }

        // Move toward the next stop, one floor at a time
        int currentFloor = elevator.getCurrentFloor();
        if (nextStop > currentFloor) {
            elevator.moveToFloor(currentFloor + 1);
        } else if (nextStop < currentFloor) {
            elevator.moveToFloor(currentFloor - 1);
        }

        // Check if we've arrived at a requested floor
        if (elevator.getCurrentFloor() == nextStop) {
            serveFloor();
        }

        try {
            // Simulate travel time between floors
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void serveFloor() {
        elevator.openDoor();
        try {
            // Simulate passengers entering/exiting
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        elevator.closeDoor();
        elevator.removeCurrentFloorFromRequests();
    }
}
