package com.lld.realworldexamples.ATM;

import com.lld.realworldexamples.ATM.entities.Account;
import com.lld.realworldexamples.ATM.entities.Card;
import com.lld.realworldexamples.ATM.entities.CashDispenser;
import com.lld.realworldexamples.ATM.enums.ATMState;
import com.lld.realworldexamples.ATM.handlers.ATMStateHandler;
import com.lld.realworldexamples.ATM.handlers.AuthenticatedState;
import com.lld.realworldexamples.ATM.handlers.CardInsertedState;
import com.lld.realworldexamples.ATM.handlers.IdleState;
import com.lld.realworldexamples.ATM.services.BankService;

import java.util.HashMap;
import java.util.Map;

public class ATM {
    private static final Object lock = new Object();
    private static volatile ATM instance;
    private final Map<ATMState, ATMStateHandler> stateHandlers;
    private final BankService bankService;
    private final CashDispenser cashDispenser;
    private ATMState currentState;
    private Card currentCard;
    private Account currentAccount;

    private ATM(BankService bankService, CashDispenser cashDispenser) {
        this.bankService = bankService;
        this.cashDispenser = cashDispenser;
        this.currentState = ATMState.IDLE;

        // Initialize state handlers
        this.stateHandlers = new HashMap<>();
        stateHandlers.put(ATMState.IDLE, new IdleState());
        stateHandlers.put(ATMState.CARD_INSERTED, new CardInsertedState());
        stateHandlers.put(ATMState.AUTHENTICATED, new AuthenticatedState());
    }

    public static ATM getInstance(BankService bankService, CashDispenser cashDispenser) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ATM(bankService, cashDispenser);
                }
            }
        }
        return instance;
    }

    // Public methods - all delegate to current state handler
    public synchronized void insertCard(Card card) {
        stateHandlers.get(currentState).insertCard(this, card);
    }

    public synchronized void authenticate(String pin) {
        stateHandlers.get(currentState).authenticate(this, pin);
    }

    public synchronized void withdraw(double amount) {
        stateHandlers.get(currentState).withdraw(this, amount);
    }

    public synchronized void deposit(double amount) {
        stateHandlers.get(currentState).deposit(this, amount);
    }

    public synchronized double checkBalance() {
        return stateHandlers.get(currentState).checkBalance(this);
    }

    public synchronized void ejectCard() {
        stateHandlers.get(currentState).ejectCard(this);
    }

    // Internal methods for state handlers to call
    public void setState(ATMState state) {
        this.currentState = state;
    }

    public Card getCurrentCard() {
        return currentCard;
    }

    public void setCurrentCard(Card card) {
        this.currentCard = card;
    }

    public Account getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(Account account) {
        this.currentAccount = account;
    }

    public BankService getBankService() {
        return bankService;
    }

    public CashDispenser getCashDispenser() {
        return cashDispenser;
    }

    ATMState getCurrentState() {
        return currentState;
    }
}
