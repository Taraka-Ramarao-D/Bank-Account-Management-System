# Bank Account Management System

A Java desktop application for creating and managing bank accounts, with a
Swing graphical interface, exact decimal money handling, transaction history,
and plain CSV file persistence. No database server and no external library
are required — only a JDK.

## Features (actually implemented)

- Create savings and current accounts (account numbers start at 1001, sequential)
- Deposit and withdraw by account number
- Transfer funds between two different, active accounts
- Apply simple interest to savings accounts at a stated annual rate
- List all accounts, search by holder name (case-insensitive)
- View a selected account's transaction history
- Close an account only when its balance is zero
- Persist accounts and history to `accounts.csv` / `transactions.csv` and reload them on restart

Validation is enforced in one place (`BankService`): non-positive amounts,
overdrafts, transfers to the same account, operations on closed accounts,
interest on current accounts, and closure of a non-empty account are all
rejected with a readable error message, and a failed operation leaves
balances unchanged.

## Technology stack

- Java 17+ (compiled with `javac --release 17`; developed and tested on JDK 21)
- Java Swing (standard library) for the interface
- `java.math.BigDecimal`, scale 2, for all money amounts
- Plain CSV files for persistence (standard library only)
- LaTeX (pdfLaTeX, TeX Live) for the report

## Project structure

```
src/com/bank/
  Main.java                  Entry point
  model/                     Account, Transaction, AccountType, TransactionType
  service/BankService.java   All business rules, validation, persistence calls
  persistence/CsvBankStore.java  CSV load/save for accounts and transactions
  ui/BankApp.java            Swing window, table, history pane, dialogs
tests/com/bank/BankServiceTest.java   Dependency-free automated test suite
docs/report/
  main.tex                   Editable LaTeX report (Overleaf-ready)
  main.pdf                   Compiled report — 11 pages (limit: 15)
  college_logo.jpg           Logo from the supplied course-end template
  main_window.png            Genuine application screenshot used in the report
screenshots/
  main_window.png            Full screenshot of the running application
  main_window_cropped.png    Cropped version used in the report
tools/                       Screenshot helpers only (SeedDemo, CaptureScreen);
                             not part of the shipped application
compile.sh  run.sh  test.sh  Build / run / test scripts
```

## Prerequisites

- JDK 17 or later (`java` and `javac` on PATH). Check with `java -version`.
- For the report only: a LaTeX installation with pdfLaTeX, or an Overleaf
  account (upload the whole `docs/report/` folder and compile with pdfLaTeX).

## Build and run

```sh
./compile.sh   # compiles src/ to out/
./run.sh       # builds if needed, then starts the Swing application
```

Data is stored in `./data/` in the working directory (created on first save).
To use a different data directory, set the system property, for example:

```sh
java -Dbank.data.dir=/path/to/data -cp out com.bank.Main
```

## Testing

```sh
./test.sh
```

Actual result on the final code (JDK 21, 2026-10-25):

```
RESULT: 24 passed, 0 failed
```

The suite covers creation, deposits, withdrawals, overdraft rejection,
negative/zero amount rejection, unknown accounts, transfers (including
same-account and over-balance rejection), savings interest (including
rejection for current accounts), account closure rules, name search, and
persistence reload. Tests use a temporary directory, so they never touch
real data in `./data/`.

## Example usage

1. Start the application with `./run.sh`.
2. Click **Create Account**, enter a holder name, choose SAVINGS or CURRENT,
   and an initial deposit.
3. Select an account in the table to see its transaction history below.
4. Use **Deposit**, **Withdraw**, **Transfer**, or **Apply Interest**
   (savings only) and enter the account number and amount when asked.
5. Use the search box to find accounts by holder name.

Demo data in the report screenshot (sample names, not real customers):
Priya Sharma, savings 1001, ends at 10400.00 after a 10000.00 opening deposit,
1500.00 deposit, 500.00 withdrawal, 1000.00 transfer out, and 400.00 interest
at 4%; Rahul Verma, current 1002, ends at 6000.00 after receiving the transfer.

## Report

- Editable source: `docs/report/main.tex` (plus `college_logo.jpg` and
  `main_window.png` in the same folder — upload the whole folder to Overleaf).
- Compiled PDF: `docs/report/main.pdf` — **11 pages**, within the 15-page limit.
- The report is based on the supplied Course End Project Report LaTeX
  template (title page, table of contents, section order). The template's
  Bluetooth notice-board content was replaced entirely; its logo file was
  kept. The uploaded ZIP contained only that template — no Java source —
  so the application in this repository was built from the project title.
- Title page now shows Batch No. 7, D Taraka Ramarao (25881A05EH) as
  the sole student, Mr. D Ganesh Deshmukh as course facilitator, and
  Department of Computer Science and Engineering (CSE), as requested.
  No title-page placeholders remain.

## Known limitations

- Single-user desktop application; no login or authentication.
- CSV persistence only; concurrent writers are not supported.
- No network, database, scheduled interest, fees, overdraft, or statement export.
- See the report (Section 10) for the full list and proposed future work.
