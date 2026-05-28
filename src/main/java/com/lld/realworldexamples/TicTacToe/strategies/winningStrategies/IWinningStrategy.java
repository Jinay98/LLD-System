package com.lld.realworldexamples.TicTacToe.strategies.winningStrategies;

import com.lld.realworldexamples.TicTacToe.enums.Symbol;
import com.lld.realworldexamples.TicTacToe.models.Board;

public interface IWinningStrategy {

    boolean checkWin(Board board, int row, int col, Symbol symbol);
}
