package com.lld.realworldexamples.ATM.handlers;

import com.lld.realworldexamples.ATM.ATM;
import com.lld.realworldexamples.ATM.entities.Card;

public interface ATMStateHandler {
    void insertCard(ATM atm, Card card);
    void authenticate(ATM atm, String pin);
    void withdraw(ATM atm, double amount);
    void deposit(ATM atm, double amount);
    double checkBalance(ATM atm);
    void ejectCard(ATM atm);
}
