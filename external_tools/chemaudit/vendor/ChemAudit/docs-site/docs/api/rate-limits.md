---
sidebar_position: 6
title: Rate Limits
description: API rate limit tiers, headers, and best practices
---

# Rate Limits

ChemAudit implements rate limiting to ensure fair usage and prevent abuse. Rate limits vary by authentication status and endpoint.

## Rate Limit Tiers

### Anonymous (Unauthenticated)

Default rate limits for requests without an API key:

| Endpoint | Limit |
|----------|-------|
| `POST /validate` | 10 req/min |
| `POST /validate/async` | 30 req/min |
| `GET /checks` | 10 req/min |
| `POST /alerts` | 10 req/min |
| `POST /alerts/quick-check` | 10 req/min |
| `GET /alerts/catalogs` | 10 req/min |
| `POST /score` | 10 req/min |
| `POST /standardize` | 10 req/min |
| `POST /batch/upload` | 10 req/min |
| `GET /batch/{job_id}` | 60 req/min |
| `GET /batch/{job_id}/status` | 10 req/min |
| `GET /batch/{job_id}/stats` | 10 req/min |
| `DELETE /batch/{job_id}` | 10 req/min |
| `POST /batch/detect-columns` | 10 req/min |
| `POST /integrations/*` | 30 req/min |

### Authenticated (with API Key)

Requests with a valid `X-API-Key` header:

| All Endpoints | Limit |
|--------------|-------|
| All | **300 req/min** |

:::tip Get an API Key
Use [API key authentication](/docs/api/authentication) for 30x higher rate limits.
:::

## Rate Limit Headers

Every response includes rate limit headers:

```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1706500800
```

| Header | Description |
|--------|-------------|
| `X-RateLimit-Limit` | Total requests allowed per minute |
| `X-RateLimit-Remaining` | Requests remaining in current window |
| `X-RateLimit-Reset` | Unix timestamp when limit resets |

## When Rate Limited

If you exceed the rate limit, you'll receive a 429 response:

```json
{
  "detail": "Rate limit exceeded. Try again in 42 seconds."
}
```

**Headers:**

```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1706500842
```

## IP Banning

Repeated rate limit violations (100+ within the tracking window) result in a temporary IP ban:

- **Ban duration**: 60 minutes (default)
- **Response**: 403 Forbidden
- **Message**: "IP address banned due to excessive rate limit violations"

:::warning Avoid Bans
Implement proper rate limiting and error handling in your code to avoid IP bans.
:::

## Best Practices

### 1. Monitor Rate Limit Headers

Check headers before making requests:

```python
import requests
import time

def check_rate_limit(response):
    remaining = int(response.headers.get('X-RateLimit-Remaining', 0))
    reset_time = int(response.headers.get('X-RateLimit-Reset', 0))

    if remaining < 5:
        print(f"Warning: Only {remaining} requests remaining")

        # Wait if needed
        if remaining == 0:
            wait_seconds = reset_time - int(time.time())
            if wait_seconds > 0:
                print(f"Waiting {wait_seconds} seconds for rate limit reset...")
                time.sleep(wait_seconds + 1)

response = requests.post(url, json=data)
check_rate_limit(response)
```

### 2. Implement Exponential Backoff

Retry with increasing delays when rate limited:

```python
import time

def api_call_with_backoff(url, data, max_retries=3):
    for attempt in range(max_retries):
        response = requests.post(url, json=data)

        if response.status_code == 200:
            return response.json()
        elif response.status_code == 429:
            # Rate limited
            reset_time = int(response.headers.get('X-RateLimit-Reset', 0))
            wait_time = max(reset_time - int(time.time()), 0) + 1

            print(f"Rate limited. Waiting {wait_time} seconds...")
            time.sleep(wait_time)
        else:
            response.raise_for_status()

    raise Exception("Max retries exceeded")
```

### 3. Batch Requests

Instead of many single validations, use batch processing:

```python
# Bad: Many API calls
for smiles in smiles_list:
    response = requests.post("/validate", json={"molecule": smiles})
    # Likely to hit rate limit

# Good: Single batch upload
with open('molecules.csv', 'rb') as f:
    response = requests.post("/batch/upload", files={"file": f})
job_id = response.json()['job_id']
# Monitor via WebSocket
```

### 4. Use API Keys

Authenticate to get 300 req/min instead of 10:

```python
import os

api_key = os.environ['CHEMAUDIT_API_KEY']

response = requests.post(
    url,
    headers={"X-API-Key": api_key},
    json=data
)
```

### 5. Distribute Requests

Spread requests evenly across time:

```python
import time

def rate_limited_validate(molecules, max_per_minute=10):
    delay = 60.0 / max_per_minute  # Seconds between requests

    results = []
    for molecule in molecules:
        response = requests.post("/validate", json={"molecule": molecule})
        results.append(response.json())

        time.sleep(delay)  # Ensure we don't exceed limit

    return results
```

### 6. Cache Results

Avoid redundant requests by caching:

```python
cache = {}

def validate_with_cache(smiles):
    if smiles in cache:
        return cache[smiles]

    response = requests.post("/validate", json={"molecule": smiles})
    result = response.json()

    cache[smiles] = result
    return result
```

:::info Server-Side Caching
ChemAudit caches validation results by InChIKey for 1 hour, so redundant requests are already fast.
:::

## Rate Limit Strategies

### Strategy 1: Token Bucket

Maintain a local token bucket to ensure you never exceed limits:

```python
import time
from collections import deque

class RateLimiter:
    def __init__(self, max_requests, time_window):
        self.max_requests = max_requests
        self.time_window = time_window
        self.requests = deque()

    def wait_if_needed(self):
        now = time.time()

        # Remove old requests outside time window
        while self.requests and self.requests[0] < now - self.time_window:
            self.requests.popleft()

        # If at limit, wait
        if len(self.requests) >= self.max_requests:
            sleep_time = self.time_window - (now - self.requests[0])
            if sleep_time > 0:
                time.sleep(sleep_time)

        self.requests.append(time.time())

# Usage
limiter = RateLimiter(max_requests=10, time_window=60)

for molecule in molecules:
    limiter.wait_if_needed()
    response = requests.post("/validate", json={"molecule": molecule})
```

### Strategy 2: Adaptive Rate Limiting

Adjust request rate based on server responses:

```python
class AdaptiveRateLimiter:
    def __init__(self):
        self.delay = 0.1  # Start with 100ms delay

    def on_success(self, response):
        remaining = int(response.headers.get('X-RateLimit-Remaining', 0))

        if remaining > 5:
            # Plenty of capacity, speed up
            self.delay = max(0.05, self.delay * 0.9)
        elif remaining < 3:
            # Running low, slow down
            self.delay = min(10.0, self.delay * 1.5)

    def on_rate_limit(self, response):
        # Hit limit, back off significantly
        self.delay = min(30.0, self.delay * 2.0)

    def wait(self):
        time.sleep(self.delay)
```

## Testing Rate Limits

Test your rate limiting implementation:

```python
import time
import requests

# Test hitting the limit
for i in range(15):  # More than limit
    start = time.time()
    response = requests.post("/validate", json={"molecule": "CCO"})

    print(f"Request {i+1}: Status {response.status_code}, "
          f"Remaining: {response.headers.get('X-RateLimit-Remaining')}")

    if response.status_code == 429:
        print("Rate limited as expected")
        break

    time.sleep(0.1)  # Small delay between requests
```

## Next Steps

- **[Authentication](/docs/api/authentication)** - Get API keys for higher limits
- **[Error Handling](/docs/api/error-handling)** - Handle 429 responses
- **[Batch Processing](/docs/user-guide/batch-processing)** - Process many molecules efficiently
