#!/usr/bin/env bash
set -euo pipefail

PID_FILE=".frontend.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "Frontend PID file not found"
  exit 0
fi

PID=$(cat "$PID_FILE")
if kill -0 "$PID" 2>/dev/null; then
  echo "Stopping frontend (PID $PID)..."
  kill "$PID"
  rm -f "$PID_FILE"
else
  echo "Frontend process not running"
  rm -f "$PID_FILE"
fi
