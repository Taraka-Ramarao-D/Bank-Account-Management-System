package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One immutable entry in an account's history. */
public class Transaction {
    private final long id;
    private final long accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final LocalDateTime timestamp;
    private final String note;

    public Transaction(long id, long accountNumber, TransactionType type,
                       BigDecimal amount, BigDecimal balanceAfter,
                       LocalDateTime timestamp, String note) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
        this.note = note == null ? "" : note;
    }

    public long getId() {
        return id;
    }

    public long getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getNote() {
        return note;
    }

    @Override
    public String toString() {
        return timestamp + " | " + type + " | " + amount + " | balance " + balanceAfter
                + (note.isEmpty() ? "" : " | " + note);
    }
}
