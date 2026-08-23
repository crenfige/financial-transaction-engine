import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  stages: [
    { duration: '30s', target: 50 },   // Ramp-up
    { duration: '1m',  target: 200 },  // Carga sostenida
    { duration: '30s', target: 0 },    // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<250', 'p(99)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.API_URL || 'http://transaction-engine-app:8080';

export default function () {
  const sourceAccountId = uuidv4();
  const destinationAccountId = uuidv4();
  const idempotencyKey = `TX-${uuidv4()}`;

  const payload = JSON.stringify({
    sourceAccountId: sourceAccountId,
    destinationAccountId: destinationAccountId,
    amount: (Math.random() * 500 + 10).toFixed(2),
    currency: 'USD',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
  };

  // 1. Envío de transacción válida
  const res = http.post(`${BASE_URL}/api/v1/transactions`, payload, params);

  check(res, {
    'status is 201 or 200': (r) => r.status === 201 || r.status === 200,
  });

  // 2. Reintento de la misma Idempotency-Key (10% de probabilidad)
  if (Math.random() < 0.10) {
    const duplicateRes = http.post(`${BASE_URL}/api/v1/transactions`, payload, params);
    check(duplicateRes, {
      'idempotent retry handled': (r) => r.status === 200 || r.status === 201,
    });
  }

  sleep(0.1);
}