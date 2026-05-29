package com.lld.realworldexamples.SnakesAndLadders;

import com.lld.realworldexamples.SnakesAndLadders.entities.BoardEntity;
import com.lld.realworldexamples.SnakesAndLadders.entities.LadderEntity;
import com.lld.realworldexamples.SnakesAndLadders.entities.SnakeEntity;
import com.lld.realworldexamples.SnakesAndLadders.models.Dice;
import com.lld.realworldexamples.SnakesAndLadders.models.Game;

import java.util.*;

public class SnakeAndLadderDemo {
    public static void main(String[] args) {
        List<BoardEntity> boardEntities = List.of(
                new SnakeEntity(17, 7),
                new SnakeEntity(54, 34),
                new SnakeEntity(62, 19),
                new SnakeEntity(98, 79),
                new LadderEntity(3, 38),
                new LadderEntity(24, 33),
                new LadderEntity(42, 93),
                new LadderEntity(72, 84)
        );

        List<String> players = Arrays.asList("Alice", "Bob", "Charlie");

        Game game = new Game.Builder()
                .setBoard(100, boardEntities)
                .setPlayers(players)
                .setDice(new Dice(1, 6))
                .build();

        game.play();
    }
}
