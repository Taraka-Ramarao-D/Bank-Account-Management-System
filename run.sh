#!/bin/sh
# Build (if needed) and start the Swing application. Data is stored in ./data/.
set -e
cd "$(dirname "$0")"
if [ ! -d out ]; then
  ./compile.sh
fi
java -cp out com.bank.Main
