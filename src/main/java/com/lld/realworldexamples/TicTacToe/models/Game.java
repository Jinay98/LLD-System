package com.lld.realworldexamples.TicTacToe.models;

import com.lld.realworldexamples.TicTacToe.enums.GameStatus;
import com.lld.realworldexamples.TicTacToe.enums.Symbol;
import com.lld.realworldexamples.TicTacToe.observers.GameObservers;
import com.lld.realworldexamples.TicTacToe.strategies.winningStrategies.IWinningStrategy;
import com.lld.realworldexamples.TicTacToe.strategies.winningStrategies.impl.ColumnWinningStrategy;
import com.lld.realworldexamples.TicTacToe.strategies.winningStrategies.impl.DiagonalWinningStrategy;
import com.lld.realworldexamples.TicTacToe.strategies.winningStrategies.impl.RowWinningStrategy;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private Board board;
    private Player[] players;
    private int currentPlayerIndex;
    private GameStatus status;
    private List<IWinningStrategy> winningStrategies;
    private List<GameObservers> observers;

    public Game(Player player1, Player player2, int size) {
        players = new Player[2];
        players[0] = player1;
        players[1] = player2;
        board = new Board(size);
        currentPlayerIndex = 0;
        this.status = GameStatus.IN_PROGRESS;
        this.winningStrategies = initializeStrategies();
        this.observers = new ArrayList<>();

    }

    private List<IWinningStrategy> initializeStrategies() {
        List<IWinningStrategy> strategies = new ArrayList<>();
        strategies.add(new RowWinningStrategy());
        strategies.add(new ColumnWinningStrategy());
        strategies.add(new DiagonalWinningStrategy());
        return strategies;
    }

    public void addObservers(GameObservers observer) {
        observers.add(observer);
    }

    private boolean checkForWin(int row, int col, Symbol symbol) {
        for (IWinningStrategy winningStrategy : winningStrategies) {
            if (winningStrategy.checkWin(board, row, col, symbol)) {
                return true;
            }
        }
        return false;
    }

    public void notifyAllObservers() {
        for (GameObservers observer : observers) {
            observer.update(this);
        }
    }

    public void makeMove(int row, int col) {
        if (status != GameStatus.IN_PROGRESS) {
            throw new RuntimeException("Game is already over!");
        }

        // Validate the move
        if (!board.isCellEmpty(row, col)) {
            throw new RuntimeException(
                    "Cell (" + row + ", " + col + ") is already occupied"
            );
        }

        Player currentPlayer = players[currentPlayerIndex];
        Symbol toBePlacedSymbol = currentPlayer.getSymbol();

        board.placeSymbol(row, col, toBePlacedSymbol);

        if (checkForWin(row, col, toBePlacedSymbol)) {
            if (toBePlacedSymbol == Symbol.X) {
                status = GameStatus.WINNER_X;
            } else {
                status = GameStatus.WINNER_O;
            }
            notifyAllObservers();
            return;
        }
        if (board.isFull()) {
            status = GameStatus.DRAW;
            notifyAllObservers();
            return;
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % 2;

    }

    public Board getBoard() { return board; }
    public Player getCurrentPlayer() { return players[currentPlayerIndex]; }
    public GameStatus getStatus() { return status; }

    public Player getWinner() {
        if (status == GameStatus.WINNER_X) {
            return players[0].getSymbol() == Symbol.X ? players[0] : players[1];
        } else if (status == GameStatus.WINNER_O) {
            return players[0].getSymbol() == Symbol.O ? players[0] : players[1];
        }
        return null;
    }

    public void printBoard() {
        board.printBoard();
    }

}
