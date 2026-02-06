---
sidebar_position: 1
title: Overview
description: ChemAudit REST API overview, base URLs, and quick start examples
---

# API Overview

ChemAudit provides a comprehensive REST API for chemical structure validation, scoring, and analysis. The API is documented with OpenAPI/Swagger and includes interactive documentation.

## Base URLs

### Development (Docker)

```
http://localhost:8001/api/v1
```

When using Docker Compose for development, the backend API is accessible on port 8001.

### Production (behind Nginx)

```
http://localhost/api/v1
```

In production, all services run behind Nginx on port 80 (or 443 with SSL).

## Interactive Documentation

ChemAudit includes auto-generated interactive API documentation:

| Format | URL | Features |
|--------|-----|----------|
| **Swagger UI** | http://localhost:8001/api/v1/docs | Interactive testing, request/response examples |
| **ReDoc** | http://localhost:8001/api/v1/redoc | Clean documentation, easier to read |

:::tip Try the API
Use Swagger UI to test endpoints directly in your browser without writing any code.
:::

## Content Type

All JSON requests must include the Content-Type header:

```
Content-Type: application/json
```

File uploads use `multipart/form-data`.

## Response Format

Responses are returned directly as JSON objects (no wrapper envelope). Each endpoint defines its own response schema.

## Quick Start Example

Validate a molecule using curl:

```bash
curl -X POST http://localhost:8001/api/v1/validate \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CCO",
    "format": "smiles"
  }'
```

Response:

```json
{
  "status": "completed",
  "molecule_info": {
    "input_smiles": "CCO",
    "canonical_smiles": "CCO",
    "inchi": "InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3",
    "inchikey": "LFQSCWFLJHTTHZ-UHFFFAOYSA-N",
    "molecular_formula": "C2H6O",
    "molecular_weight": 46.07
  },
  "overall_score": 95,
  "issues": [],
  "execution_time_ms": 12
}
```

## Python Example

```python
import requests

response = requests.post(
    "http://localhost:8001/api/v1/validate",
    json={
        "molecule": "CCO",
        "format": "smiles"
    }
)

result = response.json()
print(f"Score: {result['overall_score']}")
```

## JavaScript Example

```javascript
const response = await fetch('http://localhost:8001/api/v1/validate', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    molecule: 'CCO',
    format: 'smiles'
  })
});

const result = await response.json();
console.log(`Score: ${result.overall_score}`);
```

## API Features

### Caching

Validation results are cached by InChIKey for 1 hour. Repeated requests for the same molecule return cached results with `cached: true` flag.

### Rate Limiting

API requests are rate-limited based on authentication:

- **Anonymous**: 10 requests/min (most endpoints)
- **Authenticated**: 300 requests/min (with API key)

See [Rate Limits](/docs/api/rate-limits) for details.

### Error Handling

Errors return appropriate HTTP status codes with detailed error messages. See [Error Handling](/docs/api/error-handling) for details.

## API Sections

- **[Authentication](/docs/api/authentication)** - API key authentication and CSRF protection
- **[Endpoints](/docs/api/endpoints)** - Complete endpoint reference
- **[WebSocket](/docs/api/websocket)** - Real-time batch processing updates
- **[Error Handling](/docs/api/error-handling)** - Error responses and status codes
- **[Rate Limits](/docs/api/rate-limits)** - Rate limit tiers and headers

## Next Steps

1. **[Try the interactive docs](http://localhost:8001/api/v1/docs)** - Test endpoints in your browser
2. **[Review authentication](/docs/api/authentication)** - Set up API keys
3. **[Explore endpoints](/docs/api/endpoints)** - Full API reference
