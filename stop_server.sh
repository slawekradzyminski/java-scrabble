#!/usr/bin/env bash
set -euo pipefail

PID_FILE=".server.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "No PID file found. Is the server running?"
  exit 0
fi

PID=$(cat "$PID_FILE")
if kill -0 "$PID" 2>/dev/null; then
  kill "$PID"
  echo "Stopping server (PID $PID)..."
  wait "$PID" 2>/dev/null || true
else
  echo "Process $PID not running."
fi

rm -f "$PID_FILE"
