import http from 'k6/http';
import { check } from 'k6';

// Usage:
//   k6 run scripts/k6-10kqps-activities.js
//   k6 run -e BASE_URL=http://127.0.0.1:8080 scripts/k6-10kqps-activities.js
//   k6 run -e RATE=10000 -e DURATION=2m scripts/k6-10kqps-activities.js
//
// Notes:
// - This test targets a READ endpoint to evaluate gateway/service throughput.
// - 10k QPS on a single laptop is usually unrealistic; watch dropped_iterations.

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const RATE = Number(__ENV.RATE || 10000); // requests per second
const DURATION = __ENV.DURATION || '2m';

export const options = {
  scenarios: {
    activities_10k: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 1000,
      maxVUs: 20000,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    dropped_iterations: ['count==0'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const res = http.get(`${BASE_URL}/api/c/seckill/activities`, {
    tags: { name: 'GET /api/c/seckill/activities' },
    timeout: '10s',
  });
  check(res, { 'status is 200': (r) => r.status === 200 });
}

