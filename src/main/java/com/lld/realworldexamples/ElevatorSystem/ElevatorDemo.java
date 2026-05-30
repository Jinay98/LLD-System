package com.lld.realworldexamples.ElevatorSystem;

import com.lld.realworldexamples.ElevatorSystem.entities.Elevator;
import com.lld.realworldexamples.ElevatorSystem.enums.Direction;

import java.util.List;

public class ElevatorDemo {
    public static void main(String[] args) throws InterruptedException {
        int numElevators = 3;
        int numFloors = 10;

        ElevatorSystem system = ElevatorSystem.getInstance(numElevators, numFloors);

        System.out.println("========== ELEVATOR SYSTEM STARTED ==========");
        System.out.println("Building: " + numFloors + " floors, " +
                numElevators + " elevators\n");

        // Scenario 1: Basic hall button requests
        System.out.println("--- Scenario 1: Hall Button Requests ---");
        system.requestElevator(3, Direction.UP);
        system.requestElevator(7, Direction.DOWN);
        system.requestElevator(5, Direction.UP);

        Thread.sleep(5000);

        // Scenario 2: Internal cabin requests (passenger inside elevator 1 presses floor 8)
        System.out.println("\n--- Scenario 2: Cabin Button Requests ---");
        List<Elevator> elevators = system.getElevators();
        elevators.get(0).addRequest(8, Direction.UP);
        elevators.get(1).addRequest(1, Direction.DOWN);

        Thread.sleep(5000);

        // Scenario 3: Rush hour - multiple simultaneous requests
        System.out.println("\n--- Scenario 3: Rush Hour ---");
        system.requestElevator(1, Direction.UP);
        system.requestElevator(2, Direction.UP);
        system.requestElevator(9, Direction.DOWN);
        system.requestElevator(10, Direction.DOWN);

        Thread.sleep(5000);

        // Shutdown
        System.out.println("\n========== SHUTTING DOWN ==========");
        system.shutdown();
        System.out.println("Elevator system stopped.");
    }
}
