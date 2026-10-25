package com.bank.persistence;

import com.bank.model.Account;
import com.bank.model.AccountType;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * File persistence using two plain CSV files in one directory:
 * {@code accounts.csv} and {@code transactions.csv}.
 *
 * No database or external dependency is required, which keeps the project
 * runnable with only a JDK. Fields that contain a comma, quote or newline
 * are quoted following the usual CSV rule (a quote inside a field is doubled).
 */
public class CsvBankStore {
    private final Path directory;

    public CsvBankStore(Path directory) {
        this.directory = directory;
    }

    public Path getDirectory() {
        return directory;
    }

    /** Result of a load: the accounts and transactions found on disk (possibly empty). */
    public record LoadedData(List<Account> accounts, List<Transaction> transactions) {
    }

    public LoadedData load() throws IOException {
        List<Account> accounts = new ArrayList<>();
        Path accountsFile = directory.resolve("accounts.csv");
        if (Files.exists(accountsFile)) {
            for (String line : Files.readAllLines(accountsFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> f = parseCsvLine(line);
                accounts.add(new Account(
                        Long.parseLong(f.get(0)),
                        f.get(1),
                        AccountType.valueOf(f.get(2)),
                        new BigDecimal(f.get(3)),
                        Boolean.parseBoolean(f.get(4)),
                        LocalDateTime.parse(f.get(5))));
            }
        }
        List<Transaction> transactions = new ArrayList<>();
        Path transactionsFile = directory.resolve("transactions.csv");
        if (Files.exists(transactionsFile)) {
            for (String line : Files.readAllLines(transactionsFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> f = parseCsvLine(line);
                transactions.add(new Transaction(
                        Long.parseLong(f.get(0)),
                        Long.parseLong(f.get(1)),
                        TransactionType.valueOf(f.get(2)),
                        new BigDecimal(f.get(3)),
                        new BigDecimal(f.get(4)),
                        LocalDateTime.parse(f.get(5)),
                        f.size() > 6 ? f.get(6) : ""));
            }
        }
        return new LoadedData(accounts, transactions);
    }

    public void save(List<Account> accounts, List<Transaction> transactions) throws IOException {
        Files.createDirectories(directory);
        List<String> accountLines = new ArrayList<>();
        for (Account a : accounts) {
            accountLines.add(csv(a.getAccountNumber(), a.getHolderName(), a.getType(),
                    a.getBalance().toPlainString(), a.isActive(), a.getCreatedAt()));
        }
        Files.write(directory.resolve("accounts.csv"), accountLines, StandardCharsets.UTF_8);
        List<String> transactionLines = new ArrayList<>();
        for (Transaction t : transactions) {
            transactionLines.add(csv(t.getId(), t.getAccountNumber(), t.getType(),
                    t.getAmount().toPlainString(), t.getBalanceAfter().toPlainString(),
                    t.getTimestamp(), t.getNote()));
        }
        Files.write(directory.resolve("transactions.csv"), transactionLines, StandardCharsets.UTF_8);
    }

    private static String csv(Object... values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            String value = String.valueOf(values[i]);
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                out.append('"').append(value.replace("\"", "\"\"")).append('"');
            } else {
                out.append(value);
            }
        }
        return out.toString();
    }

    /** Minimal CSV line parser that understands the quoting produced by {@link #csv}. */
    static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
