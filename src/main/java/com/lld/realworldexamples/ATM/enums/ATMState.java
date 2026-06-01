package com.lld.realworldexamples.ATM.enums;

public enum ATMState {
    IDLE,           // No card inserted, waiting for user
    CARD_INSERTED,  // Card present, awaiting PIN
    AUTHENTICATED   // PIN verified, ready for transactions
}
