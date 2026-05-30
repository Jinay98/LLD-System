package com.lld.realworldexamples.ElevatorSystem.observers;

import com.lld.realworldexamples.ElevatorSystem.enums.Direction;

public interface ElevatorObserver {
    void onElevatorStateChanged(int elevatorId, int floor, Direction direction);
}
