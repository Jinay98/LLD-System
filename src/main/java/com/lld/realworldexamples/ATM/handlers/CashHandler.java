package com.lld.realworldexamples.ATM.handlers;

import com.lld.realworldexamples.ATM.enums.Denomination;

import java.util.Map;

public interface CashHandler {
    void setNextHandler(CashHandler handler);

    void dispense(int amount, Map<Denomination, Integer> result);
}
