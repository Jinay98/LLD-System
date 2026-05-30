package com.lld.realworldexamples.ElevatorSystem.strategies;

import com.lld.realworldexamples.ElevatorSystem.entities.Elevator;
import com.lld.realworldexamples.ElevatorSystem.enums.Direction;

import java.util.List;

public interface DispatchStrategy {
    Elevator selectElevator(List<Elevator> elevators, int floor, Direction direction);
}
