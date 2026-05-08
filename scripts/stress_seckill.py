"""
Simple seckill load test (many accounts to avoid per-user Redis limits).

pip install requests
set BCOMMERCE_API=http://127.0.0.1:8080/api
python stress_seckill.py

Env: ACTIVITY_ID (default 1), CONCURRENCY (default 20).
"""

from __future__ import annotations

import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests

BASE = os.environ.get("BCOMMERCE_API", "http://localhost:8080/api").rstrip("/")
ACTIVITY_ID = int(os.environ.get("ACTIVITY_ID", "1"))
CONCURRENCY = int(os.environ.get("CONCURRENCY", "20"))


def main() -> int:
    ts = int(time.time())
    users = [f"lb_{ts}_{i}" for i in range(CONCURRENCY)]
    for u in users:
        requests.post(
            f"{BASE}/auth/register",
            json={"username": u, "password": "demo123"},
            timeout=10,
        )
    tokens = []
    for u in users:
        r = requests.post(
            f"{BASE}/auth/login",
            json={"username": u, "password": "demo123"},
            timeout=10,
        )
        r.raise_for_status()
        tokens.append(r.json()["token"])

    def one(idx: int) -> tuple[int, str]:
        r = requests.post(
            f"{BASE}/c/seckill/orders",
            json={"activityId": ACTIVITY_ID, "quantity": 1},
            headers={
                "Authorization": f"Bearer {tokens[idx]}",
                "X-Idempotency-Key": f"stress-{ts}-{idx}",
            },
            timeout=30,
        )
        return r.status_code, r.text

    ok = fail = 0
    with ThreadPoolExecutor(max_workers=min(32, CONCURRENCY)) as pool:
        futs = [pool.submit(one, i) for i in range(CONCURRENCY)]
        for fut in as_completed(futs):
            code, body = fut.result()
            if code == 200:
                ok += 1
            else:
                fail += 1
                if fail <= 8:
                    print(code, body[:300])

    print(f"done: ok={ok} fail={fail}")
    print("failures often mean sold out, circuit breaker, or rate limits (expected in demos).")
    return 0 if fail == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
