package com.bank.service;

import com.bank.model.Account;
import com.bank.model.AccountType;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;
import com.bank.persistence.CsvBankStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All business rules for the Bank Account Management System.
 *
 * Invariants enforced here (and covered by tests):
 * <ul>
 *   <li>Amounts must be positive; a zero or negative amount is rejected.</li>
 *   <li>A withdrawal or transfer may never take a balance below zero.</li>
 *   <li>Closed accounts accept no deposit, withdrawal, transfer or interest.</li>
 *   <li>A transfer validates both accounts and the amount before moving any money.</li>
 *   <li>Interest applies only to savings accounts.</li>
 *   <li>An account may be closed only when its balance is zero.</li>
 *   <li>Every successful movement is recorded as a transaction and saved to disk.</li>
 * </ul>
 * The service is deliberately independent of Swing, so the same code is
 * exercised by the GUI and by the automated tests.
 */
public class BankService {
    /** First account number issued; later numbers are sequential. */
    public static final long FIRST_ACCOUNT_NUMBER = 1001;

    private final CsvBankStore store;
    private final Map<Long, Account> accounts = new LinkedHashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private long nextAccountNumber = FIRST_ACCOUNT_NUMBER;
    private long nextTransactionId = 1;

    public BankService(Path dataDirectory) {
        this.store = new CsvBankStore(dataDirectory);
        try {
            CsvBankStore.LoadedData data = store.load();
            for (Account a : data.accounts()) {
                accounts.put(a.getAccountNumber(), a);
                nextAccountNumber = Math.max(nextAccountNumber, a.getAccountNumber() + 1);
            }
            transactions.addAll(data.transactions());
            for (Transaction t : data.transactions()) {
                nextTransactionId = Math.max(nextTransactionId, t.getId() + 1);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load bank data from " + dataDirectory, e);
        }
    }

    // ----- creation and lookup -----

    public Account createAccount(String holderName, AccountType type, BigDecimal initialDeposit) {
        requireHolderName(holderName);
        requireNonNullType(type);
        BigDecimal opening = money(initialDeposit == null ? BigDecimal.ZERO : initialDeposit);
        if (opening.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative.");
        }
        Account account = new Account(nextAccountNumber++, holderName.trim(), type,
                opening, true, LocalDateTime.now());
        accounts.put(account.getAccountNumber(), account);
        if (opening.compareTo(BigDecimal.ZERO) > 0) {
            record(account, TransactionType.DEPOSIT, opening, "Opening deposit");
        }
        save();
        return account;
    }

    public Account getAccount(long accountNumber) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("No account with number " + accountNumber + ".");
        }
        return account;
    }

    public List<Account> listAccounts() {
        List<Account> result = new ArrayList<>(accounts.values());
        result.sort(Comparator.comparingLong(Account::getAccountNumber));
        return result;
    }

    /** Case-insensitive search of holder names; returns an empty list when nothing matches. */
    public List<Account> searchByHolderName(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase();
        List<Account> result = new ArrayList<>();
        for (Account a : listAccounts()) {
            if (a.getHolderName().toLowerCase().contains(needle)) {
                result.add(a);
            }
        }
        return result;
    }

    public List<Transaction> transactionsFor(long accountNumber) {
        getAccount(accountNumber); // validates that the account exists
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t.getAccountNumber() == accountNumber) {
                result.add(t);
            }
        }
        return result;
    }

    // ----- movements -----

    public Account deposit(long accountNumber, BigDecimal amount) {
        Account account = requireActive(accountNumber);
        BigDecimal value = requirePositive(amount);
        account.setBalance(account.getBalance().add(value));
        record(account, TransactionType.DEPOSIT, value, "");
        save();
        return account;
    }

    public Account withdraw(long accountNumber, BigDecimal amount) {
        Account account = requireActive(accountNumber);
        BigDecimal value = requirePositive(amount);
        if (account.getBalance().compareTo(value) < 0) {
            throw new IllegalArgumentException("Insufficient funds: balance is "
                    + account.getBalance() + ", withdrawal requested is " + value + ".");
        }
        account.setBalance(account.getBalance().subtract(value));
        record(account, TransactionType.WITHDRAWAL, value, "");
        save();
        return account;
    }

    /**
     * Moves money from one account to another. Both accounts, the amount and
     * the source balance are checked before either balance changes, so a
     * failed transfer leaves both accounts untouched.
     */
    public void transfer(long fromAccountNumber, long toAccountNumber, BigDecimal amount) {
        if (fromAccountNumber == toAccountNumber) {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }
        Account from = requireActive(fromAccountNumber);
        Account to = requireActive(toAccountNumber);
        BigDecimal value = requirePositive(amount);
        if (from.getBalance().compareTo(value) < 0) {
            throw new IllegalArgumentException("Insufficient funds in source account.");
        }
        from.setBalance(from.getBalance().subtract(value));
        to.setBalance(to.getBalance().add(value));
        record(from, TransactionType.TRANSFER_OUT, value, "To " + toAccountNumber);
        record(to, TransactionType.TRANSFER_IN, value, "From " + fromAccountNumber);
        save();
    }

    /**
     * Credits simple interest to a savings account: balance * rate / 100.
     * The rate is an annual percentage, e.g. 4 means 4%.
     */
    public Account applyInterest(long accountNumber, BigDecimal annualRatePercent) {
        Account account = requireActive(accountNumber);
        if (account.getType() != AccountType.SAVINGS) {
            throw new IllegalArgumentException("Interest applies only to savings accounts.");
        }
        BigDecimal rate = requirePositive(annualRatePercent);
        BigDecimal interest = money(account.getBalance()
                .multiply(rate).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP));
        if (interest.compareTo(BigDecimal.ZERO) > 0) {
            account.setBalance(account.getBalance().add(interest));
            record(account, TransactionType.INTEREST, interest, "Interest at " + rate + "%");
            save();
        }
        return account;
    }

    public Account closeAccount(long accountNumber) {
        Account account = requireActive(accountNumber);
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(
                    "Account cannot be closed while its balance is " + account.getBalance()
                            + ". Withdraw the balance first.");
        }
        account.setActive(false);
        save();
        return account;
    }

    // ----- helpers -----

    private Account requireActive(long accountNumber) {
        Account account = getAccount(accountNumber);
        if (!account.isActive()) {
            throw new IllegalArgumentException("Account " + accountNumber + " is closed.");
        }
        return account;
    }

    private void record(Account account, TransactionType type, BigDecimal amount, String note) {
        transactions.add(new Transaction(nextTransactionId++, account.getAccountNumber(),
                type, money(amount), money(account.getBalance()), LocalDateTime.now(), note));
    }

    private void save() {
        try {
            store.save(listAccounts(), transactions);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save bank data", e);
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required.");
        }
        BigDecimal value = money(amount);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        return value;
    }

    private static void requireHolderName(String holderName) {
        if (holderName == null || holderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account holder name is required.");
        }
    }

    private static void requireNonNullType(AccountType type) {
        if (type == null) {
            throw new IllegalArgumentException("Account type is required.");
        }
    }
}
