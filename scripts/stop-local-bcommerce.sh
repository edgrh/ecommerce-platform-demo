#!/usr/bin/env bash
# Free ports 8080 (gateway) / 8081 (commerce) so Maven repackage can rename jars.
set -euo pipefail

kill_listeners_on_port() {
  local port=$1
  if command -v lsof >/dev/null 2>&1; then
    local pids
    pids=$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)
    if [[ -n "${pids:-}" ]]; then
      echo "Port $port: stopping PID(s): $pids"
      kill -TERM $pids 2>/dev/null || true
      sleep 1
      kill -KILL $pids 2>/dev/null || true
    else
      echo "Port $port: no listener."
    fi
  elif command -v fuser >/dev/null 2>&1; then
    if fuser "$port/tcp" >/dev/null 2>&1; then
      echo "Port $port: fuser -k"
      fuser -k "$port/tcp" 2>/dev/null || true
    else
      echo "Port $port: no listener."
    fi
  else
    echo "Need lsof or fuser to free port $port." >&2
    exit 1
  fi
}

echo "=== Release ports 8080 / 8081 ==="
kill_listeners_on_port 8080
kill_listeners_on_port 8081
sleep 2
echo ""
echo "If Maven still cannot rename jar: stop IDE Run/Debug, kill stray java, check AV locks on target/."
