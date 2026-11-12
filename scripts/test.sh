#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/classes build/test-classes
find src/main/java -name '*.java' > build/sources.txt
find src/test/java -name '*.java' > build/test-sources.txt
javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt
javac --release 17 -encoding UTF-8 -cp build/classes -d build/test-classes @build/test-sources.txt
java -cp build/classes:build/test-classes app.PlannerTests
