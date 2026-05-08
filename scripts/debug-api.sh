#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://127.0.0.1:8080}"
API="$BASE/api"

curl -sf "$BASE/actuator/health" || true
echo
curl -sf "$API/c/products?limit=5" | head -c 400
echo
curl -sf -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"buyer","password":"demo123"}' | head -c 200
echo
curl -sf "$API/c/seckill/activities" | head -c 400
echo
echo ok
