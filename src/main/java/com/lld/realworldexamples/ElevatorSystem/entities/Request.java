package com.lld.realworldexamples.ElevatorSystem.entities;

import com.lld.realworldexamples.ElevatorSystem.enums.Direction;
import com.lld.realworldexamples.ElevatorSystem.enums.RequestType;

class Request {
    private final int floor;
    private final Direction direction;
    private final RequestType type;
    private final long timestamp;

    public Request(int floor, Direction direction, RequestType type) {
        this.floor = floor;
        this.direction = direction;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public int getFloor() {
        return floor;
    }

    public Direction getDirection() {
        return direction;
    }

    public RequestType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Request{floor=" + floor + ", direction=" + direction +
                ", type=" + type + "}";
    }
}
