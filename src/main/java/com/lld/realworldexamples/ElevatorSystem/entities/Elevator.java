package com.lld.realworldexamples.ElevatorSystem.entities;

import com.lld.realworldexamples.ElevatorSystem.enums.Direction;
import com.lld.realworldexamples.ElevatorSystem.enums.ElevatorState;
import com.lld.realworldexamples.ElevatorSystem.exceptions.ElevatorException;
import com.lld.realworldexamples.ElevatorSystem.observers.ElevatorObserver;
import com.lld.realworldexamples.ElevatorSystem.observers.impl.Display;

import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class Elevator {
    private final int id;
    private int currentFloor;
    private Direction direction;
    private ElevatorState state;
    private final Door door;
    private final Display display;
    // Ascending order: ceiling() gives us the next floor above
    private final TreeSet<Integer> upRequests;
    // Descending order: ceiling() gives us the next floor below (highest first)
    private final TreeSet<Integer> downRequests;
    // Thread-safe list for observers that may be modified during iteration
    private final CopyOnWriteArrayList<ElevatorObserver> observers;
    private final int totalFloors;

    public Elevator(int id, int totalFloors) {
        this.id = id;
        this.currentFloor = 1;
        this.direction = Direction.IDLE;
        this.state = ElevatorState.IDLE;
        this.door = new Door();
        this.display = new Display(id);
        this.upRequests = new TreeSet<>();
        // Reverse order so ceiling() returns the next floor BELOW current
        this.downRequests = new TreeSet<>(Collections.reverseOrder());
        this.observers = new CopyOnWriteArrayList<>();
        this.totalFloors = totalFloors;
        addObserver(display);
    }

    public synchronized void addRequest(int floor, Direction direction) {
        if (state == ElevatorState.OUT_OF_SERVICE) {
            throw new ElevatorException("Elevator " + id + " is out of service");
        }
        if (floor < 1 || floor > totalFloors) {
            throw new ElevatorException("Invalid floor: " + floor);
        }
        if (floor == currentFloor) {
            return; // Already at the requested floor
        }

        // Add to the appropriate request set based on direction
        if (direction == Direction.UP || floor > currentFloor) {
            upRequests.add(floor);
        } else {
            downRequests.add(floor);
        }

        System.out.println("Elevator " + id + " received request for floor " + floor);
    }

    public synchronized int getNextStop() {
        if (direction == Direction.UP) {
            // Find the next floor at or above current position
            Integer next = upRequests.ceiling(currentFloor);
            if (next != null) return next;
            // No more up requests, check down requests
            if (!downRequests.isEmpty()) return -1; // Signal to reverse
        } else if (direction == Direction.DOWN) {
            // ceiling() on reversed set gives us the next floor at or below current
            Integer next = downRequests.ceiling(currentFloor);
            if (next != null) return next;
            // No more down requests, check up requests
            if (!upRequests.isEmpty()) return -1; // Signal to reverse
        }
        return -1; // No requests
    }

    public synchronized void moveToFloor(int floor) {
        this.currentFloor = floor;
        if (direction == Direction.UP) {
            this.state = ElevatorState.MOVING_UP;
        } else if (direction == Direction.DOWN) {
            this.state = ElevatorState.MOVING_DOWN;
        }
        notifyObservers();
    }

    public void openDoor() {
        door.open();
        synchronized (this) {
            state = ElevatorState.DOOR_OPEN;
        }
        System.out.println("Elevator " + id + " arrived at floor " +
                currentFloor + ", opening door");
    }

    public void closeDoor() {
        door.close();
        System.out.println("Elevator " + id + " door closed");
    }

    public synchronized void removeCurrentFloorFromRequests() {
        upRequests.remove(currentFloor);
        downRequests.remove(currentFloor);
    }

    public void addObserver(ElevatorObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers() {
        for (ElevatorObserver observer : observers) {
            observer.onElevatorStateChanged(id, currentFloor, direction);
        }
    }

    public synchronized boolean hasRequests() {
        return !upRequests.isEmpty() || !downRequests.isEmpty();
    }

    public synchronized boolean hasUpRequests() {
        return !upRequests.isEmpty();
    }

    public synchronized boolean hasDownRequests() {
        return !downRequests.isEmpty();
    }

    public int getId() {
        return id;
    }

    public synchronized int getCurrentFloor() {
        return currentFloor;
    }

    public synchronized Direction getDirection() {
        return direction;
    }

    public synchronized void setDirection(Direction direction) {
        this.direction = direction;
    }

    public synchronized ElevatorState getState() {
        return state;
    }

    public synchronized void setState(ElevatorState state) {
        this.state = state;
    }

    public int getTotalFloors() {
        return totalFloors;
    }

    public Display getDisplay() {
        return display;
    }
}
