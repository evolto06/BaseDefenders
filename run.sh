#!/usr/bin/env sh
set -e
cd "$(dirname "$0")"
javac -d bin -sourcepath src src/main/Main.java
java -cp bin main.Main
