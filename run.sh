#!/usr/bin/env bash
# Compile and run Colour Droplet (requires a JDK with javac on PATH).
set -e
cd "$(dirname "$0")"
command -v javac >/dev/null 2>&1 || { echo "JDK not found: put javac on your PATH first."; exit 1; }
[ -f lib/flatlaf-3.7.2.jar ] || { echo "FlatLaf jar missing from the lib folder."; exit 1; }
mkdir -p out
javac -encoding UTF-8 -cp "lib/*" -d out src/*.java
java -cp "out:lib/*" ColorDropletApp
