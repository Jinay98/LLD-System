package com.lld.realworldexamples.TicTacToe.strategies.winningStrategies.impl;

import com.lld.realworldexamples.TicTacToe.enums.Symbol;
import com.lld.realworldexamples.TicTacToe.models.Board;
import com.lld.realworldexamples.TicTacToe.strategies.winningStrategies.IWinningStrategy;

public class RowWinningStrategy implements IWinningStrategy {
    @Override
    public boolean checkWin(Board board, int row, int col, Symbol symbol) {
        for (int j = 0; j < board.getSize(); j++) {
            if (board.getGrid()[row][j].getSymbol() != symbol) {
                return false;
            }
        }
        return true;
    }
}
