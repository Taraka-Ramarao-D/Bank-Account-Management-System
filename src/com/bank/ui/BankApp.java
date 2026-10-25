package com.bank.ui;

import com.bank.model.Account;
import com.bank.model.AccountType;
import com.bank.model.Transaction;
import com.bank.service.BankService;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Swing front end for the Bank Account Management System.
 * All rules live in {@link BankService}; this class only collects input,
 * calls the service, shows its errors, and refreshes the display.
 * The data directory can be overridden with the system property
 * {@code bank.data.dir} (default: {@code data}).
 */
public class BankApp extends JFrame {
    private final BankService service;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextArea historyArea;
    private final JTextField searchField;

    public BankApp(BankService service) {
        super("Bank Account Management System");
        this.service = service;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);

        tableModel = new DefaultTableModel(
                new Object[]{"Account No.", "Holder", "Type", "Balance", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshHistory(selectedAccountNumber(table));
            }
        });

        searchField = new JTextField();
        JPanel buttons = new JPanel(new GridLayout(2, 4, 6, 6));
        buttons.add(button("Create Account", this::onCreate));
        buttons.add(button("Deposit", () -> onAmount("Deposit", (n, a) -> service.deposit(n, a))));
        buttons.add(button("Withdraw", () -> onAmount("Withdraw", (n, a) -> service.withdraw(n, a))));
        buttons.add(button("Transfer", this::onTransfer));
        buttons.add(button("Apply Interest", this::onInterest));
        buttons.add(button("Close Account", this::onClose));
        buttons.add(button("Refresh", this::refreshAccounts));
        buttons.add(button("Clear Search", () -> {
            searchField.setText("");
            refreshAccounts();
        }));

        JPanel searchPanel = new JPanel(new BorderLayout(6, 6));
        JButton searchButton = new JButton("Search by holder name");
        searchButton.addActionListener(e -> refreshAccounts(service.searchByHolderName(searchField.getText())));
        searchPanel.add(new JLabel("Search holder: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        historyArea = new JTextArea(10, 80);
        historyArea.setEditable(false);

        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.add(buttons, BorderLayout.CENTER);
        top.add(searchPanel, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(new JScrollPane(historyArea), BorderLayout.SOUTH);
        refreshAccounts();
    }

    private JButton button(String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(e -> {
            try {
                action.run();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Could not complete operation", JOptionPane.ERROR_MESSAGE);
            }
            refreshAccounts();
        });
        return button;
    }

    private void onCreate() {
        JTextField nameField = new JTextField();
        JComboBox<AccountType> typeBox = new JComboBox<>(AccountType.values());
        JTextField depositField = new JTextField("0");
        int ok = JOptionPane.showConfirmDialog(this,
                new Object[]{"Holder name:", nameField, "Account type:", typeBox,
                        "Initial deposit:", depositField},
                "Create Account", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            Account created = service.createAccount(nameField.getText(),
                    (AccountType) typeBox.getSelectedItem(), parse(depositField.getText()));
            JOptionPane.showMessageDialog(this,
                    "Account created: " + created.getAccountNumber()
                            + " for " + created.getHolderName() + ".");
        }
    }

    private interface AmountOperation {
        void apply(long accountNumber, BigDecimal amount);
    }

    private void onAmount(String title, AmountOperation operation) {
        String number = JOptionPane.showInputDialog(this, "Account number:");
        if (number == null) {
            return;
        }
        String amount = JOptionPane.showInputDialog(this, "Amount:");
        if (amount == null) {
            return;
        }
        operation.apply(Long.parseLong(number.trim()), parse(amount));
        JOptionPane.showMessageDialog(this, title + " completed.");
    }

    private void onTransfer() {
        String from = JOptionPane.showInputDialog(this, "From account number:");
        if (from == null) {
            return;
        }
        String to = JOptionPane.showInputDialog(this, "To account number:");
        if (to == null) {
            return;
        }
        String amount = JOptionPane.showInputDialog(this, "Amount:");
        if (amount == null) {
            return;
        }
        service.transfer(Long.parseLong(from.trim()), Long.parseLong(to.trim()), parse(amount));
        JOptionPane.showMessageDialog(this, "Transfer completed.");
    }

    private void onInterest() {
        String number = JOptionPane.showInputDialog(this, "Savings account number:");
        if (number == null) {
            return;
        }
        String rate = JOptionPane.showInputDialog(this, "Annual interest rate (%):", "4");
        if (rate == null) {
            return;
        }
        Account account = service.applyInterest(Long.parseLong(number.trim()), parse(rate));
        JOptionPane.showMessageDialog(this,
                "Interest applied. New balance: " + account.getBalance() + ".");
    }

    private void onClose() {
        String number = JOptionPane.showInputDialog(this, "Account number to close:");
        if (number == null) {
            return;
        }
        service.closeAccount(Long.parseLong(number.trim()));
        JOptionPane.showMessageDialog(this, "Account closed.");
    }

    private Long selectedAccountNumber(JTable table) {
        int row = table.getSelectedRow();
        return row < 0 ? null : (Long) tableModel.getValueAt(row, 0);
    }

    private void refreshAccounts() {
        refreshAccounts(service.listAccounts());
    }

    private void refreshAccounts(List<Account> accounts) {
        tableModel.setRowCount(0);
        for (Account a : accounts) {
            tableModel.addRow(new Object[]{a.getAccountNumber(), a.getHolderName(),
                    a.getType(), a.getBalance(), a.isActive() ? "Active" : "Closed"});
        }
        // Show the first account's history by default, so the history pane is not empty.
        if (tableModel.getRowCount() > 0 && table.getSelectedRow() < 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private void refreshHistory(Long accountNumber) {
        if (accountNumber == null) {
            historyArea.setText("Select an account to see its transaction history.");
            return;
        }
        StringBuilder out = new StringBuilder("Transactions for account " + accountNumber + ":\n");
        for (Transaction t : service.transactionsFor(accountNumber)) {
            out.append(t).append('\n');
        }
        historyArea.setText(out.toString());
    }

    private static BigDecimal parse(String text) {
        return new BigDecimal(text.trim());
    }

    public static void main(String[] args) {
        Path dataDirectory = Path.of(System.getProperty("bank.data.dir", "data"));
        SwingUtilities.invokeLater(() -> new BankApp(new BankService(dataDirectory)).setVisible(true));
    }
}
