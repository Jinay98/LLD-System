package com.lld.realworldexamples.TicTacToe.models;

import com.lld.realworldexamples.TicTacToe.enums.Symbol;

public class Cell {
    private Symbol symbol;

    public Cell() {
        symbol = Symbol.EMPTY;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    public boolean isEmpty() {
        return symbol == Symbol.EMPTY;
    }
}
