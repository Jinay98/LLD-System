package com.lld.realworldexamples.ElevatorSystem.entities;

import com.lld.realworldexamples.ElevatorSystem.observers.impl.Display;

public class Floor {
    private final int floorNumber;
    private final Display display;
    private boolean upButtonPressed;
    private boolean downButtonPressed;

    public Floor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.upButtonPressed = false;
        this.downButtonPressed = false;
        this.display = new Display(0); // Floor display, not tied to specific elevator
    }

    public void pressUpButton() {
        this.upButtonPressed = true;
    }

    public void pressDownButton() {
        this.downButtonPressed = true;
    }

    public void resetButtons() {
        this.upButtonPressed = false;
        this.downButtonPressed = false;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public boolean isUpButtonPressed() {
        return upButtonPressed;
    }

    public boolean isDownButtonPressed() {
        return downButtonPressed;
    }

    public Display getDisplay() {
        return display;
    }
}
