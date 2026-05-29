package com.lld.realworldexamples.SnakesAndLadders.entities;

public class LadderEntity extends BoardEntity {
    public LadderEntity(int start, int end) {
        super(start, end);
        if (start >= end) {
            throw new RuntimeException("Invalid ladder start and end coordinates");
        }
    }
}
