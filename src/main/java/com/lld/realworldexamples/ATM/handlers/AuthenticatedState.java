package com.lld.realworldexamples.ATM.handlers;

import com.lld.realworldexamples.ATM.ATM;
import com.lld.realworldexamples.ATM.entities.Card;
import com.lld.realworldexamples.ATM.enums.ATMState;
import com.lld.realworldexamples.ATM.enums.Denomination;
import com.lld.realworldexamples.ATM.exceptions.ATMException;

import java.util.Map;

public class AuthenticatedState implements ATMStateHandler {
    @Override
    public void insertCard(ATM atm, Card card) {
        throw new ATMException("A card is already inserted");
    }

    @Override
    public void authenticate(ATM atm, String pin) {
        throw new ATMException("Already authenticated");
    }

    @Override
    public void withdraw(ATM atm, double amount) {
        int intAmount = (int) amount;
        if (intAmount <= 0 || intAmount % 10 != 0) {
            throw new ATMException("Amount must be a positive multiple of $10");
        }

        // Check account balance first
        double balance = atm.getCurrentAccount().getBalance();
        if (amount > balance) {
            throw new ATMException(
                    "Insufficient funds. Account balance: $" + balance + ", requested: $" + amount
            );
        }

        // Check if ATM can dispense this amount with available bills
        if (!atm.getCashDispenser().canDispense(intAmount)) {
            throw new ATMException("ATM cannot dispense $" + intAmount +
                    " with available denominations");
        }

        // CRITICAL: Dispense cash BEFORE debiting account
        // If the dispenser jams after debiting, the customer loses money
        Map<Denomination, Integer> dispensed = atm.getCashDispenser().dispense(intAmount);
        atm.getCurrentAccount().debit(amount);

        System.out.println("Dispensing $" + intAmount + ": " + dispensed);
    }

    @Override
    public void deposit(ATM atm, double amount) {
        if (amount <= 0) {
            throw new ATMException("Deposit amount must be positive");
        }
        atm.getCurrentAccount().credit(amount);
        System.out.println("Deposited $" + amount + " successfully");
    }

    @Override
    public double checkBalance(ATM atm) {
        return atm.getCurrentAccount().getBalance();
    }

    @Override
    public void ejectCard(ATM atm) {
        atm.setCurrentCard(null);
        atm.setCurrentAccount(null);
        atm.setState(ATMState.IDLE);
        System.out.println("Card ejected successfully");
    }
}
