# ChemAudit Python Client

Official Python client for the ChemAudit API - comprehensive chemical structure validation, standardization, and ML-readiness assessment.

## Installation

```bash
pip install chemaudit-client
```

Or install from source:

```bash
cd client
pip install -e .
```

## Quick Start

```python
from chemaudit import ChemAuditClient

# Create client (optional API key for higher rate limits)
client = ChemAuditClient(
    base_url="http://localhost:8000",
    api_key="your-api-key",  # Optional
)

# Validate a molecule
result = client.validate("CCO")
print(f"Validation Score: {result.overall_score}")
print(f"Issues: {len(result.issues)}")

# Screen for structural alerts
alerts = client.screen_alerts("c1ccccc1")
print(f"Total Alerts: {alerts.total_alerts}")

# Calculate ML-readiness score
scores = client.score("CCO")
print(f"ML-Readiness: {scores.scores['ml_readiness_score']}")

# Standardize molecule
standard = client.standardize("[Na+].CC(=O)[O-]")
print(f"Standardized: {standard.standardized_smiles}")

# Close client when done
client.close()
```

## Features

### Synchronous-Only Design

This client is **intentionally synchronous-only** using `httpx.Client`. This design choice provides:

- **Simpler API** for most use cases (scripts, notebooks, CLI tools)
- **No async runtime complexity** for users
- **Batch operations with polling** that work well synchronously
- Easy integration with existing synchronous code

**For async usage:**
- Wrap calls with `asyncio.to_thread()` in async contexts
- Use `httpx.AsyncClient` directly for full async control

```python
# Async usage example
import asyncio
from chemaudit import ChemAuditClient

async def validate_async():
    client = ChemAuditClient()
    result = await asyncio.to_thread(client.validate, "CCO")
    return result
```

### Single Molecule Operations

```python
# Validate with additional analysis
result = client.validate(
    molecule="CCO",
    format="smiles",  # auto, smiles, inchi, mol
    include_alerts=True,
    include_scores=True,
    standardize=True,
)

# Access results
print(f"Overall Score: {result.overall_score}")
for issue in result.issues:
    print(f"- {issue.check_name}: {issue.message}")

# Screen structural alerts
alerts = client.screen_alerts(
    smiles="c1ccccc1",
    catalogs=["PAINS", "BRENK"],  # Optional
)

# Calculate scores
scores = client.score("CCO")
ml_readiness = scores.scores["ml_readiness_score"]
np_likeness = scores.scores["np_likeness_score"]

# Standardize molecule
result = client.standardize(
    smiles="[Na+].CC(=O)[O-]",
    tautomer=False,  # Preserves E/Z stereochemistry
)
print(f"Changes: {result.changes_made}")
```

### Batch Processing

```python
# Submit batch job
response = client.submit_batch(
    file_path="molecules.csv",
    smiles_column="smiles",
)
job_id = response.job_id

# Wait for completion (blocking with polling)
job = client.wait_for_batch(
    job_id,
    poll_interval=2.0,  # Check every 2 seconds
    timeout=3600.0,     # Max 1 hour
)

# Get results (paginated)
results = client.get_batch_results(
    job_id,
    page=1,
    page_size=100,
    score_min=50,  # Filter by score
)

# Iterate through all results
for item in client.iter_batch_results(job_id):
    if item.status == "success":
        score = item.validation["overall_score"]
        print(f"{item.smiles}: {score}")

# Export results
path = client.export_batch(
    job_id,
    format="excel",  # csv, excel, sdf, json
    score_min=50,
)
print(f"Exported to: {path}")

# Get statistics
stats = client.get_batch_stats(job_id)
print(f"Average Score: {stats.avg_validation_score}")
print(f"Alert Distribution: {stats.alert_summary}")
```

### Context Manager

```python
# Automatic cleanup
with ChemAuditClient() as client:
    result = client.validate("CCO")
    print(result.overall_score)
# Client automatically closed
```

## API Reference

### ChemAuditClient

**Constructor:**
```python
ChemAuditClient(
    base_url: str = "http://localhost:8000",
    api_key: Optional[str] = None,
    timeout: float = 30.0,
    max_retries: int = 3,
)
```

**Methods:**

- `validate(molecule, format="auto", include_alerts=False, include_scores=False, standardize=False)` - Validate single molecule
- `screen_alerts(smiles, catalogs=None)` - Screen for structural alerts
- `score(smiles)` - Calculate ML-readiness and other scores
- `standardize(smiles, tautomer=False)` - Standardize molecule
- `submit_batch(file_path, smiles_column="smiles")` - Submit batch job
- `get_batch_status(job_id)` - Get batch job status
- `get_batch_results(job_id, page=1, page_size=100, **filters)` - Get batch results
- `get_batch_stats(job_id)` - Get batch statistics
- `cancel_batch(job_id)` - Cancel running batch job
- `wait_for_batch(job_id, poll_interval=2.0, timeout=3600.0)` - Wait for batch completion
- `iter_batch_results(job_id, page_size=100, **filters)` - Iterate all batch results
- `export_batch(job_id, format="csv", output_path=None, **filters)` - Export batch results
- `close()` - Close HTTP client

### Models

**ValidationResult:**
- `overall_score: int` - Validation score (0-100)
- `issues: List[CheckResult]` - Failed checks
- `all_checks: List[CheckResult]` - All check results
- `molecule_info: MoleculeInfo` - Molecule information

**BatchResult:**
- `results: List[BatchResultItem]` - Paginated results
- `statistics: BatchStatistics` - Aggregate statistics
- `page: int`, `page_size: int`, `total_pages: int` - Pagination info

### Exceptions

- `ChemAuditError` - Base exception
- `APIError` - API returned error
- `RateLimitError` - Rate limit exceeded
- `AuthenticationError` - Invalid API key
- `ValidationError` - Request validation failed
- `BatchJobNotFoundError` - Job not found
- `TimeoutError` - Request or batch job timed out

## Rate Limits

**Anonymous (IP-based):**
- 10 requests/minute

**API Key (key-based):**
- 300 requests/minute

The client automatically retries on rate limit errors (429) with exponential backoff.

## Error Handling

```python
from chemaudit import (
    ChemAuditClient,
    ValidationError,
    RateLimitError,
    AuthenticationError,
    APIError,
)

client = ChemAuditClient()

try:
    result = client.validate("invalid-smiles")
except ValidationError as e:
    print(f"Validation error: {e}")
except RateLimitError as e:
    print(f"Rate limit exceeded. Retry after {e.retry_after} seconds")
except AuthenticationError:
    print("Invalid API key")
except APIError as e:
    print(f"API error {e.status_code}: {e}")
```

## Development

Install development dependencies:

```bash
cd client
pip install -e ".[dev]"
```

Run tests:

```bash
pytest tests/ -v
```

## Examples

See the `examples/` directory for complete examples:

- `basic_usage.py` - Single molecule validation, alerts, scoring
- `batch_processing.py` - Batch file processing and export

## License

MIT License

## Links

- **Documentation:** https://github.com/Kohulan/ChemAudit#readme
- **Issue Tracker:** https://github.com/Kohulan/ChemAudit/issues
- **API Documentation:** http://localhost:8000/docs (when server is running)
