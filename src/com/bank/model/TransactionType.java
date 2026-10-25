package com.bank.model;

/** Kind of movement recorded in an account's transaction history. */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT,
    INTEREST
}
