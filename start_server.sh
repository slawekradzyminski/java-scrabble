#!/usr/bin/env bash
set -euo pipefail

PID_FILE=".server.pid"
LOG_DIR="logs"
LOG_FILE="$LOG_DIR/backend.log"
FST_PATH="artifacts/osps.fst"
META_PATH="artifacts/osps.fst.meta.json"

mkdir -p "$LOG_DIR"

if [[ ! -f "$FST_PATH" || ! -f "$META_PATH" ]]; then
  echo "Dictionary artifacts missing; generating from osps_shortened.txt..."
  ./gradlew :packages:dictionary-tools:compileOsps -PospsInput=osps_shortened.txt -PospsOutput="$FST_PATH"
fi

if [[ -f "$PID_FILE" ]]; then
  PID=$(cat "$PID_FILE")
  if kill -0 "$PID" 2>/dev/null; then
    echo "Server already running (PID $PID)"
    exit 0
  fi
fi

FST_ABS="$(pwd)/$FST_PATH"
META_ABS="$(pwd)/$META_PATH"

nohup ./gradlew :apps:backend:bootRun \
  --args="--dictionary.fstPath=$FST_ABS --dictionary.metaPath=$META_ABS" \
  >"$LOG_FILE" 2>&1 &
PID=$!
echo "$PID" > "$PID_FILE"

echo "Server started (PID $PID). Logs: $LOG_FILE"
