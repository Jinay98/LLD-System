package com.lld.realworldexamples.VendingMachine.states;

import com.lld.realworldexamples.VendingMachine.VendingMachine;
import com.lld.realworldexamples.VendingMachine.enums.Coin;

public abstract class VendingMachineState {
    VendingMachine machine;

    public VendingMachineState(VendingMachine machine) {
        this.machine = machine;
    }

    public abstract void insertCoin(Coin coin);

    public abstract void selectItem(String code);

    public abstract void dispense();

    public abstract void refund();
}
