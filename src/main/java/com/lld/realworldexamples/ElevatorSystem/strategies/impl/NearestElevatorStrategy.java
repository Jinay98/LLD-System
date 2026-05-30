package com.lld.realworldexamples.ElevatorSystem.strategies.impl;

import com.lld.realworldexamples.ElevatorSystem.entities.Elevator;
import com.lld.realworldexamples.ElevatorSystem.enums.Direction;
import com.lld.realworldexamples.ElevatorSystem.enums.ElevatorState;
import com.lld.realworldexamples.ElevatorSystem.exceptions.ElevatorException;
import com.lld.realworldexamples.ElevatorSystem.strategies.DispatchStrategy;

import java.util.List;

public class NearestElevatorStrategy implements DispatchStrategy {
    private final int totalFloors;

    public NearestElevatorStrategy(int totalFloors) {
        this.totalFloors = totalFloors;
    }

    @Override
    public Elevator selectElevator(List<Elevator> elevators, int floor, Direction direction) {
        Elevator bestElevator = null;
        int bestScore = -1;

        for (Elevator elevator : elevators) {
            if (elevator.getState() == ElevatorState.OUT_OF_SERVICE) {
                continue; // Skip out-of-service elevators
            }

            int distance = Math.abs(elevator.getCurrentFloor() - floor);
            int score = 0;

            Direction elevatorDir = elevator.getDirection();

            if (elevatorDir == Direction.IDLE) {
                // Idle elevator: score based on proximity
                score = totalFloors - distance;
            } else if (elevatorDir == direction) {
                // Same direction: check if it hasn't passed the floor yet
                boolean hasPassed;
                if (direction == Direction.UP) {
                    hasPassed = elevator.getCurrentFloor() > floor;
                } else {
                    hasPassed = elevator.getCurrentFloor() < floor;
                }

                if (!hasPassed) {
                    // Best case: heading toward us in the right direction
                    score = totalFloors - distance + totalFloors;
                } else {
                    // Already passed, would need to come back
                    score = 1;
                }
            } else {
                // Opposite direction: low priority
                score = 1;
            }

            if (score > bestScore) {
                bestScore = score;
                bestElevator = elevator;
            }
        }

        if (bestElevator == null) {
            throw new ElevatorException("No available elevator for floor " + floor);
        }

        return bestElevator;
    }
}
