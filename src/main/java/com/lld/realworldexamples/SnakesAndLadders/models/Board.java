package com.lld.realworldexamples.SnakesAndLadders.models;

import com.lld.realworldexamples.SnakesAndLadders.entities.BoardEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {
    private int size;
    private Map<Integer, Integer> snakesAndLadders;

    public Board(int size, List<BoardEntity> boardEntities) {
        this.size = size;
        this.snakesAndLadders = new HashMap<>();

        for (BoardEntity entity : boardEntities) {
            snakesAndLadders.put(entity.getStart(), entity.getEnd());
        }
    }

    public int getFinalPosition(int position) {
        return snakesAndLadders.getOrDefault(position, position);
    }

    public int getSize() {
        return size;
    }
}
