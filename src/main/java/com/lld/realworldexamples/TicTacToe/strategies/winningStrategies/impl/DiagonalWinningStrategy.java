package com.lld.realworldexamples.TicTacToe.strategies.winningStrategies.impl;

import com.lld.realworldexamples.TicTacToe.enums.Symbol;
import com.lld.realworldexamples.TicTacToe.models.Board;
import com.lld.realworldexamples.TicTacToe.strategies.winningStrategies.IWinningStrategy;

public class DiagonalWinningStrategy implements IWinningStrategy {
    @Override
    public boolean checkWin(Board board, int row, int col, Symbol symbol) {
        boolean mainDiagonalWin = true;
        for (int i = 0; i < board.getSize(); i++) {
            if (board.getGrid()[i][i].getSymbol() != symbol) {
                mainDiagonalWin = false;
                break;
            }
        }
        if (mainDiagonalWin) {
            return true;
        }
        for (int i = 0; i < board.getSize(); i++) {
            if (board.getGrid()[i][board.getSize() - 1 - i].getSymbol() != symbol) {
                return false;
            }
        }
        return true;
    }
}
