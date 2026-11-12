#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/classes
find src/main/java -name '*.java' > build/sources.txt
javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt
java -cp build/classes app.Main "$@"
