package com.lld.realworldexamples.ATM.services;

import com.lld.realworldexamples.ATM.entities.Account;
import com.lld.realworldexamples.ATM.entities.Card;
import com.lld.realworldexamples.ATM.exceptions.ATMException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BankService {
    private final Map<String, Account> accounts;
    private final Map<String, Card> cards;

    public BankService() {
        this.accounts = new ConcurrentHashMap<>();
        this.cards = new ConcurrentHashMap<>();
    }

    public void createAccount(String accountNumber, double initialBalance) {
        accounts.put(accountNumber, new Account(accountNumber, initialBalance));
    }

    public void createCard(String cardNumber, String pin, String accountNumber) {
        if (!accounts.containsKey(accountNumber)) {
            throw new ATMException("Account " + accountNumber + " does not exist");
        }
        cards.put(cardNumber, new Card(cardNumber, pin, accountNumber));
    }

    public Account authenticate(String cardNumber, String pin) {
        Card card = cards.get(cardNumber);
        if (card == null) {
            throw new ATMException("Card not recognized");
        }
        if (!card.getPin().equals(pin)) {
            throw new ATMException("Incorrect PIN");
        }
        Account account = accounts.get(card.getAccountNumber());
        if (account == null) {
            throw new ATMException("Account not found for card " + cardNumber);
        }
        return account;
    }

    public double getBalance(String accountNumber) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new ATMException("Account " + accountNumber + " not found");
        }
        return account.getBalance();
    }

    public void debit(String accountNumber, double amount) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new ATMException("Account " + accountNumber + " not found");
        }
        account.debit(amount);
    }

    public void credit(String accountNumber, double amount) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new ATMException("Account " + accountNumber + " not found");
        }
        account.credit(amount);
    }
}
