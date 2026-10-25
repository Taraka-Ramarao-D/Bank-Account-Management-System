package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single bank account. Money is held as {@link BigDecimal} (scale 2) so
 * that decimal amounts are never subject to binary floating-point error.
 * This class holds state only; all business rules live in BankService.
 */
public class Account {
    private final long accountNumber;
    private final String holderName;
    private final AccountType type;
    private BigDecimal balance;
    private boolean active;
    private final LocalDateTime createdAt;

    public Account(long accountNumber, String holderName, AccountType type,
                   BigDecimal balance, boolean active, LocalDateTime createdAt) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.type = type;
        this.balance = balance;
        this.active = active;
        this.createdAt = createdAt;
    }

    public long getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public AccountType getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Package use is by BankService only; the GUI never mutates an account directly. */
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return accountNumber + " | " + holderName + " | " + type
                + " | " + balance + " | " + (active ? "Active" : "Closed");
    }
}
