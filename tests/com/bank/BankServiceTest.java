package com.bank;

import com.bank.model.Account;
import com.bank.model.AccountType;
import com.bank.service.BankService;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Automated tests for BankService. No external test library is used, so the
 * suite runs with only a JDK: {@code test.sh} compiles and runs this class.
 * Each check prints PASS/FAIL and the process exits non-zero if any check fails.
 */
public class BankServiceTest {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        Path data = Files.createTempDirectory("bank-test-");
        BankService bank = new BankService(data);

        Account savings = bank.createAccount("Asha Rao", AccountType.SAVINGS, bd("1000"));
        check("first account number is 1001", savings.getAccountNumber() == 1001);
        check("opening balance recorded", savings.getBalance().compareTo(bd("1000")) == 0);

        bank.deposit(1001, bd("500"));
        check("deposit increases balance", bank.getAccount(1001).getBalance().compareTo(bd("1500")) == 0);

        bank.withdraw(1001, bd("200"));
        check("withdrawal decreases balance", bank.getAccount(1001).getBalance().compareTo(bd("1300")) == 0);

        checkThrows("withdrawal above balance is rejected",
                () -> bank.withdraw(1001, bd("99999")));
        check("failed withdrawal leaves balance unchanged",
                bank.getAccount(1001).getBalance().compareTo(bd("1300")) == 0);
        checkThrows("negative deposit is rejected", () -> bank.deposit(1001, bd("-10")));
        checkThrows("zero withdrawal is rejected", () -> bank.withdraw(1001, bd("0")));
        checkThrows("unknown account is rejected", () -> bank.deposit(9999, bd("10")));

        Account current = bank.createAccount("Vikram Reddy", AccountType.CURRENT, bd("300"));
        check("second account number is sequential", current.getAccountNumber() == 1002);
        bank.transfer(1001, 1002, bd("300"));
        check("transfer debits source", bank.getAccount(1001).getBalance().compareTo(bd("1000")) == 0);
        check("transfer credits destination", bank.getAccount(1002).getBalance().compareTo(bd("600")) == 0);
        checkThrows("transfer to same account is rejected",
                () -> bank.transfer(1001, 1001, bd("10")));
        checkThrows("transfer above balance is rejected",
                () -> bank.transfer(1001, 1002, bd("99999")));

        bank.applyInterest(1001, bd("4"));
        check("4% interest on 1000 is 40", bank.getAccount(1001).getBalance().compareTo(bd("1040")) == 0);
        checkThrows("interest on current account is rejected",
                () -> bank.applyInterest(1002, bd("4")));

        checkThrows("account with a balance cannot be closed",
                () -> bank.closeAccount(1002));
        bank.withdraw(1002, bd("600"));
        bank.closeAccount(1002);
        check("zero-balance account closes", !bank.getAccount(1002).isActive());
        checkThrows("closed account accepts no deposit",
                () -> bank.deposit(1002, bd("10")));

        List<Account> found = bank.searchByHolderName("asha");
        check("case-insensitive name search finds the account",
                found.size() == 1 && found.get(0).getAccountNumber() == 1001);
        check("name search with no match is empty", bank.searchByHolderName("nobody").isEmpty());

        // Persistence: a fresh service over the same directory must see the same state.
        BankService reloaded = new BankService(data);
        check("reloaded savings balance matches",
                reloaded.getAccount(1001).getBalance().compareTo(bd("1040")) == 0);
        check("reloaded account is still closed",
                !reloaded.getAccount(1002).isActive());
        check("transaction history survives reload",
                reloaded.transactionsFor(1001).size() == bank.transactionsFor(1001).size()
                        && !reloaded.transactionsFor(1001).isEmpty());

        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + name);
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void checkThrows(String name, Runnable action) {
        try {
            action.run();
            check(name, false);
        } catch (IllegalArgumentException expected) {
            check(name, true);
        }
    }
}
