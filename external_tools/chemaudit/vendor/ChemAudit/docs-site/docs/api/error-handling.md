---
sidebar_position: 5
title: Error Handling
description: API error responses, HTTP status codes, and common error scenarios
---

# Error Handling

ChemAudit API uses standard HTTP status codes and returns detailed error messages to help you debug issues.

## Error Response Format

### Simple Errors

For simple errors, the response includes a detail message:

```json
{
  "detail": "Job abc123 not found"
}
```

### Detailed Errors

For validation and parsing errors, the response includes structured error information:

```json
{
  "detail": {
    "error": "Failed to parse molecule",
    "errors": ["Invalid SMILES string"],
    "warnings": [],
    "format_detected": "smiles"
  }
}
```

| Field | Description |
|-------|-------------|
| `error` | High-level error message |
| `errors` | List of specific error messages |
| `warnings` | List of warnings (non-fatal) |
| `format_detected` | Detected input format (if applicable) |

## HTTP Status Codes

| Code | Description | Common Causes |
|------|-------------|---------------|
| **200** | Success | Request completed successfully |
| **201** | Created | Resource created (e.g., API key) |
| **204** | No Content | Successful deletion |
| **400** | Bad Request | Invalid input, unsupported format, file too large |
| **403** | Forbidden | IP banned, CSRF validation failed, unauthorized |
| **404** | Not Found | Job ID not found, API key not found, endpoint doesn't exist |
| **429** | Too Many Requests | Rate limit exceeded |
| **500** | Internal Server Error | Unexpected server error |
| **504** | Gateway Timeout | Request took too long (async validation timeout) |

## Common Error Scenarios

### 400 Bad Request

**Invalid SMILES:**

```json
{
  "detail": {
    "error": "Failed to parse molecule",
    "errors": ["Invalid SMILES string: C1CCC1 (unclosed ring)"],
    "format_detected": "smiles"
  }
}
```

**Solution:** Fix the SMILES syntax error.

**File too large:**

```json
{
  "detail": "File size (600 MB) exceeds limit (500 MB)"
}
```

**Solution:** Use a larger deployment profile or split the file.

**Unsupported format:**

```json
{
  "detail": "Unsupported file format: .xyz"
}
```

**Solution:** Convert to SDF, CSV, or other supported formats.

### 403 Forbidden

**IP banned:**

```json
{
  "detail": "IP address banned due to excessive rate limit violations"
}
```

**Solution:** Wait for the ban to expire (default: 60 minutes) or contact admin.

**CSRF validation failed:**

```json
{
  "detail": "CSRF validation failed"
}
```

**Solution:** Include valid CSRF token in X-CSRF-Token header.

**Invalid admin secret:**

```json
{
  "detail": "Invalid admin secret"
}
```

**Solution:** Use correct X-Admin-Secret header value.

### 404 Not Found

**Job not found:**

```json
{
  "detail": "Job abc123 not found"
}
```

**Solution:** Verify job ID is correct and job exists.

**Endpoint not found:**

```json
{
  "detail": "Not Found"
}
```

**Solution:** Check API path and HTTP method.

### 429 Too Many Requests

**Rate limit exceeded:**

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

**Solution:** Wait and retry, or use API key authentication for higher limits.

### 500 Internal Server Error

**Unexpected error:**

```json
{
  "detail": "Internal server error. Please try again later."
}
```

**Solution:** Check backend logs, retry request, report if persistent.

### 504 Gateway Timeout

**Async validation timeout:**

```json
{
  "detail": "Validation timeout after 30 seconds"
}
```

**Solution:** Molecule may be too complex. Try with standard /validate endpoint or increase timeout.

## Error Handling Best Practices

### Check Status Codes

Always check HTTP status codes before parsing response:

```python
response = requests.post(url, json=data)

if response.status_code == 200:
    result = response.json()
    # Process success
elif response.status_code == 400:
    error = response.json()
    print(f"Invalid input: {error['detail']}")
elif response.status_code == 429:
    print("Rate limited, waiting...")
    time.sleep(60)
    # Retry
else:
    print(f"Error {response.status_code}: {response.text}")
```

### Implement Retry Logic

For transient errors (429, 500, 504), implement exponential backoff:

```python
import time
import requests

def validate_with_retry(molecule, max_retries=3):
    for attempt in range(max_retries):
        try:
            response = requests.post(
                "http://localhost:8001/api/v1/validate",
                json={"molecule": molecule},
                timeout=30
            )

            if response.status_code == 200:
                return response.json()
            elif response.status_code == 429:
                # Rate limited, wait and retry
                wait_time = 2 ** attempt  # Exponential backoff
                time.sleep(wait_time)
                continue
            elif response.status_code >= 500:
                # Server error, retry
                time.sleep(2 ** attempt)
                continue
            else:
                # Client error, don't retry
                response.raise_for_status()

        except requests.exceptions.Timeout:
            if attempt == max_retries - 1:
                raise
            time.sleep(2 ** attempt)

    raise Exception("Max retries exceeded")
```

### Parse Error Details

Extract useful information from error responses:

```python
response = requests.post(url, json=data)

if not response.ok:
    error_data = response.json()

    if isinstance(error_data['detail'], dict):
        # Structured error
        print(f"Error: {error_data['detail']['error']}")
        for err in error_data['detail']['errors']:
            print(f"  - {err}")
    else:
        # Simple error
        print(f"Error: {error_data['detail']}")
```

### Handle Rate Limits

Use rate limit headers to avoid hitting limits:

```python
response = requests.post(url, json=data)

remaining = int(response.headers.get('X-RateLimit-Remaining', 0))
if remaining < 5:
    print("Warning: approaching rate limit")
    # Slow down requests

reset_time = int(response.headers.get('X-RateLimit-Reset', 0))
# Schedule next request after reset time
```

### Validate Input Locally

Catch errors early by validating input before making API requests:

```python
def is_valid_smiles(smiles):
    # Basic validation
    if len(smiles) > 10000:
        return False, "SMILES too long"

    if not smiles.strip():
        return False, "Empty SMILES"

    # More sophisticated validation with RDKit
    try:
        from rdkit import Chem
        mol = Chem.MolFromSmiles(smiles)
        if mol is None:
            return False, "Invalid SMILES"
    except:
        return False, "SMILES parsing error"

    return True, None

# Use before API call
valid, error = is_valid_smiles(user_input)
if not valid:
    print(f"Input error: {error}")
else:
    response = requests.post(...)
```

## Next Steps

- **[Rate Limits](/docs/api/rate-limits)** - Understanding rate limits
- **[Authentication](/docs/api/authentication)** - Higher rate limits with API keys
- **[Endpoints](/docs/api/endpoints)** - Full API reference
