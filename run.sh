#!/bin/sh
# Сборка и запуск приложения одной командой (REQ-INST-001). Нужен JDK 17+.
set -e
cd "$(dirname "$0")"
mkdir -p out
find src -name '*.java' > out/sources.txt
javac -encoding UTF-8 -d out @out/sources.txt
exec java -Dfile.encoding=UTF-8 -cp out labequip.Main
