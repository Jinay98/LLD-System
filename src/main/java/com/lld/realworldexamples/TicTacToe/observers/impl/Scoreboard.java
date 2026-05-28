package com.lld.realworldexamples.TicTacToe.observers.impl;

import com.lld.realworldexamples.TicTacToe.models.Game;
import com.lld.realworldexamples.TicTacToe.models.Player;
import com.lld.realworldexamples.TicTacToe.observers.GameObservers;

import java.util.HashMap;
import java.util.Map;

public class Scoreboard implements GameObservers {

    Map<String, Integer> scores;

    public Scoreboard() {
        scores = new HashMap<>();
    }

    @Override
    public void update(Game game) {
        Player winner = game.getWinner();
        if (winner != null) {
            recordWin(winner);
            System.out.println("Scoreboard updated: " + winner.getName() + " wins!");
        }
    }

    public void recordWin(Player player) {
        scores.merge(player.getName(), 1, Integer::sum);
    }

    public int getScore(String playerName) {
        return scores.getOrDefault(playerName, 0);
    }

    public void printScoreboard() {
        System.out.println("\n===== SCOREBOARD =====");
        if (scores.isEmpty()) {
            System.out.println("No games played yet.");
        } else {
            scores.forEach((name, score) ->
                    System.out.println(name + ": " + score + " wins")
            );
        }
        System.out.println("======================\n");
    }
}
