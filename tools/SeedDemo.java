import com.bank.model.AccountType;
import com.bank.service.BankService;
import java.math.BigDecimal;
import java.nio.file.Path;

/**
 * Temporary screenshot helper (not part of the shipped application):
 * creates the demo data shown in the report screenshots using the real
 * BankService, so the GUI screenshot shows genuine service output.
 * Usage: java -cp out SeedDemo <data-dir>
 */
public class SeedDemo {
    public static void main(String[] args) {
        BankService bank = new BankService(Path.of(args[0]));
        if (!bank.listAccounts().isEmpty()) {
            System.out.println("Data directory already has accounts; leaving it unchanged.");
            return;
        }
        bank.createAccount("Priya Sharma", AccountType.SAVINGS, new BigDecimal("10000"));
        bank.createAccount("Rahul Verma", AccountType.CURRENT, new BigDecimal("5000"));
        bank.deposit(1001, new BigDecimal("1500"));
        bank.withdraw(1001, new BigDecimal("500"));
        bank.transfer(1001, 1002, new BigDecimal("1000"));
        bank.applyInterest(1001, new BigDecimal("4"));
        System.out.println("Seeded: Priya Sharma savings = "
                + bank.getAccount(1001).getBalance()
                + ", Rahul Verma current = " + bank.getAccount(1002).getBalance());
    }
}
