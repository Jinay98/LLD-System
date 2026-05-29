package com.lld.realworldexamples.SnakesAndLadders.entities;

public class SnakeEntity extends BoardEntity {
    public SnakeEntity(int start, int end) {
        super(start, end);
        if(start <= end){
            throw new RuntimeException("Invalid snake start and end coordinates");
        }
    }
}
