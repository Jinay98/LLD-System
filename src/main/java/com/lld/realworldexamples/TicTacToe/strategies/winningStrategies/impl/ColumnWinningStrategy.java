package com.lld.realworldexamples.TicTacToe.strategies.winningStrategies.impl;

import com.lld.realworldexamples.TicTacToe.enums.Symbol;
import com.lld.realworldexamples.TicTacToe.models.Board;
import com.lld.realworldexamples.TicTacToe.strategies.winningStrategies.IWinningStrategy;

public class ColumnWinningStrategy implements IWinningStrategy {
    @Override
    public boolean checkWin(Board board, int row, int col, Symbol symbol) {
        for (int i = 0; i < board.getSize(); i++) {
            if(board.getGrid()[i][col].getSymbol() != symbol){
                return false;
            }
        }
        return true;
    }
}
