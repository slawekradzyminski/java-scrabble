#!/usr/bin/env bash
set -euo pipefail

PID_FILE=".frontend.pid"
LOG_DIR="logs"
LOG_FILE="$LOG_DIR/frontend.log"

mkdir -p "$LOG_DIR"

if [[ -f "$PID_FILE" ]]; then
  PID=$(cat "$PID_FILE")
  if kill -0 "$PID" 2>/dev/null; then
    echo "Frontend already running (PID $PID)"
    exit 0
  fi
fi

if [[ ! -d "apps/frontend/node_modules" ]]; then
  (cd apps/frontend && npm install)
fi

nohup bash -c "cd apps/frontend && npm run dev -- --host" >"$LOG_FILE" 2>&1 &
PID=$!
echo "$PID" > "$PID_FILE"

echo "Frontend started (PID $PID). Logs: $LOG_FILE"
