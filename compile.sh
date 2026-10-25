#!/bin/sh
# Compile the application. Requires JDK 17 or later; no external dependencies.
set -e
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
find src -name '*.java' > /tmp/bank-sources.txt
javac --release 17 -d out @/tmp/bank-sources.txt
echo "Compiled successfully to out/"
