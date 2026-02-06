---
sidebar_position: 2
title: Authentication
description: API key authentication and CSRF protection for ChemAudit API
---

# Authentication

ChemAudit supports API key authentication for higher rate limits and CSRF protection for browser-based clients.

## Authentication Methods

| Method | Use Case | Rate Limit |
|--------|----------|------------|
| **Anonymous** | Development, testing | 10 req/min |
| **API Key** | Production, integration | 300 req/min |

## API Key Authentication

### Using API Keys

Include your API key in the `X-API-Key` header:

```bash
curl -H "X-API-Key: your-api-key" \
  http://localhost:8001/api/v1/validate \
  -H "Content-Type: application/json" \
  -d '{"molecule": "CCO"}'
```

### Generating API Keys

API keys are managed via the `/api-keys` endpoints, which require admin authentication:

**Create a new API key:**

```bash
curl -X POST http://localhost:8001/api/v1/api-keys \
  -H "X-Admin-Secret: your-admin-secret" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-application",
    "description": "Key for production use",
    "expiry_days": 90
  }'
```

Response (201 Created):

```json
{
  "key": "chemaudit_abc123def456...",
  "name": "my-application",
  "created_at": "2026-02-01T00:00:00Z",
  "expires_at": "2026-05-02T00:00:00Z"
}
```

:::danger Save Your API Key
The full API key is shown only once during creation. Save it securely - you cannot retrieve it later.
:::

### Listing API Keys

View metadata for all API keys (without the actual key values):

```bash
curl -H "X-Admin-Secret: your-admin-secret" \
  http://localhost:8001/api/v1/api-keys
```

Response:

```json
[
  {
    "key_id": "abc123def456",
    "name": "my-application",
    "description": "Key for production use",
    "created_at": "2026-02-01T00:00:00Z",
    "last_used": "2026-02-04T12:00:00Z",
    "request_count": 1500,
    "expires_at": "2026-05-02T00:00:00Z",
    "is_expired": false
  }
]
```

### Revoking API Keys

Delete an API key to immediately revoke access:

```bash
curl -X DELETE http://localhost:8001/api/v1/api-keys/{key_id} \
  -H "X-Admin-Secret: your-admin-secret"
```

Returns 204 No Content on success.

## Admin Secret

The admin secret is used to manage API keys and is set via environment variable:

```env
API_KEY_ADMIN_SECRET=your-secure-admin-secret
```

:::warning Protect Admin Secret
The admin secret has full API key management privileges. Keep it secure and never commit it to version control.
:::

## CSRF Protection

### What Is CSRF Protection?

CSRF (Cross-Site Request Forgery) protection prevents unauthorized state-changing requests from malicious websites.

### When Is CSRF Required?

CSRF tokens are required for browser-based requests with an `Origin` header matching configured CORS origins, using state-changing methods (POST, PUT, DELETE, PATCH).

### How to Use CSRF Tokens

**1. Fetch a CSRF token:**

```bash
curl http://localhost:8001/api/v1/csrf-token
```

Response:

```json
{
  "csrf_token": "abc123def456789..."
}
```

**2. Include it in subsequent requests:**

```bash
curl -X POST http://localhost:8001/api/v1/validate \
  -H "Content-Type: application/json" \
  -H "X-CSRF-Token: abc123def456789..." \
  -d '{"molecule": "CCO"}'
```

### CSRF Exemptions

CSRF validation is skipped for:

- **API key authenticated requests**: Requests with valid `X-API-Key` header
- **Non-browser requests**: Requests without `Origin` header
- **Safe methods**: GET, HEAD, OPTIONS requests

:::info API Keys Skip CSRF
If you're using API key authentication, you don't need CSRF tokens.
:::

## Best Practices

### API Key Security

1. **Never commit API keys** to version control
2. **Use environment variables** to store keys
3. **Rotate keys periodically** (set expiry_days)
4. **Use separate keys** for different applications
5. **Revoke unused keys** immediately
6. **Monitor key usage** via request_count and last_used

### Example: Environment Variable

```bash
# .env (never commit this file)
CHEMAUDIT_API_KEY=chemaudit_abc123def456...
```

```python
import os
import requests

api_key = os.environ['CHEMAUDIT_API_KEY']

response = requests.post(
    "http://localhost:8001/api/v1/validate",
    headers={"X-API-Key": api_key},
    json={"molecule": "CCO"}
)
```

### Example: Configuration File

```python
# config.py
import os

class Config:
    CHEMAUDIT_API_KEY = os.environ.get('CHEMAUDIT_API_KEY')
    CHEMAUDIT_BASE_URL = "http://localhost:8001/api/v1"

# api_client.py
import requests
from config import Config

def validate_molecule(smiles):
    response = requests.post(
        f"{Config.CHEMAUDIT_BASE_URL}/validate",
        headers={"X-API-Key": Config.CHEMAUDIT_API_KEY},
        json={"molecule": smiles}
    )
    return response.json()
```

## Rate Limit Benefits

Authenticated requests get significantly higher rate limits:

| Endpoint Type | Anonymous | Authenticated |
|--------------|-----------|---------------|
| Validation | 10/min | 300/min |
| Scoring | 10/min | 300/min |
| Batch results | 60/min | 300/min |
| All other endpoints | 10/min | 300/min |

See [Rate Limits](/docs/api/rate-limits) for complete details.

## Next Steps

- **[Endpoints](/docs/api/endpoints)** - Complete API reference
- **[Rate Limits](/docs/api/rate-limits)** - Rate limit details
- **[Error Handling](/docs/api/error-handling)** - Handling authentication errors
