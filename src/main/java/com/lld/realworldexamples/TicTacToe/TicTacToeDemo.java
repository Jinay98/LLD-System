package com.lld.realworldexamples.TicTacToe;

import com.lld.realworldexamples.TicTacToe.enums.Symbol;
import com.lld.realworldexamples.TicTacToe.models.Player;

public class TicTacToeDemo {
    public static void main(String[] args) {
        TicTacToeSystem system = TicTacToeSystem.getInstance();

        Player alice = new Player("Alice", Symbol.X);
        Player bob = new Player("Bob", Symbol.O);

        // Game 1: Alice wins
        System.out.println("========== GAME 1 ==========");
        system.createGame(alice, bob);

        system.makeMove(alice, 0, 0);  // X at (0,0)
        system.makeMove(bob, 1, 0);    // O at (1,0)
        system.makeMove(alice, 0, 1);  // X at (0,1)
        system.makeMove(bob, 1, 1);    // O at (1,1)
        system.makeMove(alice, 0, 2);  // X at (0,2) - Alice wins!

        System.out.println("Game 1 Result: " + system.getGameStatus());

        // Game 2: Bob wins
        System.out.println("\n========== GAME 2 ==========");
        system.createGame(alice, bob);

        system.makeMove(alice, 0, 0);  // X at (0,0)
        system.makeMove(bob, 1, 1);    // O at (1,1) - center
        system.makeMove(alice, 0, 1);  // X at (0,1)
        system.makeMove(bob, 0, 2);    // O at (0,2)
        system.makeMove(alice, 2, 0);  // X at (2,0)
        system.makeMove(bob, 2, 2);    // O at (2,2) - Bob wins diagonal!

        System.out.println("Game 2 Result: " + system.getGameStatus());

        // Game 3: Draw
        System.out.println("\n========== GAME 3 ==========");
        system.createGame(alice, bob);

        system.makeMove(alice, 0, 0);  // X
        system.makeMove(bob, 0, 1);    // O
        system.makeMove(alice, 0, 2);  // X
        system.makeMove(bob, 1, 1);    // O
        system.makeMove(alice, 1, 0);  // X
        system.makeMove(bob, 1, 2);    // O
        system.makeMove(alice, 2, 1);  // X
        system.makeMove(bob, 2, 0);    // O
        system.makeMove(alice, 2, 2);  // X - Draw!

        System.out.println("Game 3 Result: " + system.getGameStatus());

        // Final scoreboard
        system.printScoreboard();
    }
}
