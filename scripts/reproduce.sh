#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mvn clean verify
java -Xmx2g -jar target/assignment3-graphs.jar benchmark
python scripts/report.py
