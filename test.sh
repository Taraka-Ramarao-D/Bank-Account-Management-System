#!/bin/sh
# Compile the application and its tests, then run the test suite.
set -e
cd "$(dirname "$0")"
./compile.sh
javac --release 17 -cp out -d out tests/com/bank/BankServiceTest.java
java -cp out com.bank.BankServiceTest
