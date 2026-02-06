---
sidebar_position: 3
title: Endpoints
description: Complete ChemAudit API endpoint reference with request/response examples
---

# API Endpoints

Complete reference for all ChemAudit API endpoints, organized by feature.

## Health and Configuration

### GET /health

Health check endpoint. Returns system status and RDKit version.

**Response:**

```json
{
  "status": "healthy",
  "app_name": "ChemAudit",
  "app_version": "1.0.0",
  "rdkit_version": "2025.09.3"
}
```

### GET /config

Get public application configuration including deployment limits.

**Response:**

```json
{
  "app_name": "ChemAudit",
  "app_version": "1.0.0",
  "deployment_profile": "medium",
  "limits": {
    "max_batch_size": 10000,
    "max_file_size_mb": 500,
    "max_file_size_bytes": 524288000
  }
}
```

## Validation

### POST /validate

Validate a single molecule. Results are cached by InChIKey for 1 hour.

**Request:**

```json
{
  "molecule": "CCO",
  "format": "auto",
  "checks": ["all"],
  "preserve_aromatic": false
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `molecule` | string | Yes | SMILES, InChI, or MOL block (max 10,000 chars) |
| `format` | string | No | `auto`, `smiles`, `inchi`, `mol` (default: `auto`) |
| `checks` | array | No | Specific checks to run (default: `["all"]`) |
| `preserve_aromatic` | bool | No | Keep aromatic notation in output SMILES (default: `false`) |

**Response:**

```json
{
  "status": "completed",
  "molecule_info": {
    "input_smiles": "CCO",
    "canonical_smiles": "CCO",
    "inchi": "InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3",
    "inchikey": "LFQSCWFLJHTTHZ-UHFFFAOYSA-N",
    "molecular_formula": "C2H6O",
    "molecular_weight": 46.07,
    "num_atoms": 3
  },
  "overall_score": 95,
  "issues": [],
  "all_checks": [
    {
      "check_name": "valence_check",
      "passed": true,
      "severity": "critical",
      "message": "All atoms have valid valence"
    }
  ],
  "execution_time_ms": 12,
  "cached": false
}
```

### POST /validate/async

Validate using the Celery high-priority queue. Use when batch jobs are running.

**Query Parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `timeout` | int | Max wait time in seconds (1-60, default: 30) |

**Request:** Same as POST /validate

**Response:** Same as POST /validate with additional `queue` field

### GET /checks

List available validation checks grouped by category.

**Response:**

```json
{
  "basic": ["valence_check", "kekulize_check", "sanitization_check"],
  "stereo": ["undefined_stereo_check", "stereo_consistency_check"],
  "representation": ["smiles_length_check", "inchi_generation_check"]
}
```

## Structural Alerts

### POST /alerts

Screen molecule for structural alerts.

**Request:**

```json
{
  "molecule": "c1ccc2c(c1)nc(n2)Sc3nnnn3C",
  "format": "smiles",
  "catalogs": ["PAINS"]
}
```

**Response:**

```json
{
  "status": "completed",
  "alerts": [
    {
      "pattern_name": "thiazole_amine_A",
      "description": "Potential assay interference",
      "severity": "warning",
      "matched_atoms": [4, 5, 6, 7],
      "catalog_source": "PAINS_A",
      "smarts": "[#7]-c1nc2ccccc2s1"
    }
  ],
  "total_alerts": 1,
  "has_critical": false,
  "has_warning": true
}
```

### POST /alerts/quick-check

Fast check for presence of alerts (no detailed results).

**Request:** Same as POST /alerts

**Response:**

```json
{
  "has_alerts": true,
  "checked_catalogs": ["PAINS"]
}
```

### GET /alerts/catalogs

List available structural alert catalogs.

**Response:**

```json
{
  "catalogs": {
    "PAINS": {
      "name": "PAINS",
      "description": "Pan-Assay Interference Compounds",
      "pattern_count": 480,
      "severity": "warning"
    }
  },
  "default_catalogs": ["PAINS"]
}
```

## Scoring

### POST /score

Calculate comprehensive molecular scores.

**Request:**

```json
{
  "molecule": "CCO",
  "format": "smiles",
  "include": ["ml_readiness", "druglikeness", "admet"]
}
```

**Available score types:**

- `ml_readiness`: ML-readiness score (0-100)
- `np_likeness`: Natural product likeness
- `scaffold`: Murcko scaffold extraction
- `druglikeness`: Drug-likeness filters
- `safety_filters`: Safety alerts summary
- `admet`: ADMET predictions
- `aggregator`: Aggregator likelihood

**Response:** See [User Guide - Scoring](/docs/user-guide/scoring/overview) for detailed response schemas.

## Standardization

### POST /standardize

Standardize a molecule using ChEMBL-compatible pipeline.

**Request:**

```json
{
  "molecule": "CC(=O)Oc1ccccc1C(=O)[O-].[Na+]",
  "format": "smiles",
  "options": {
    "include_tautomer": false,
    "preserve_stereo": true
  }
}
```

**Response:**

```json
{
  "result": {
    "original_smiles": "CC(=O)Oc1ccccc1C(=O)[O-].[Na+]",
    "standardized_smiles": "CC(=O)OC1=CC=CC=C1C(O)=O",
    "success": true,
    "steps_applied": [...],
    "excluded_fragments": ["[Na+]"],
    "structure_comparison": {
      "mass_change_percent": -10.87
    }
  }
}
```

### GET /standardize/options

Get available standardization options with descriptions.

## Batch Processing

### POST /batch/upload

Upload file for batch processing.

**Request:** `multipart/form-data`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | file | Yes | SDF, CSV, TSV, or TXT file |
| `smiles_column` | string | No | Column name for SMILES in CSV |
| `name_column` | string | No | Column name for molecule names/IDs |
| `include_extended_safety` | bool | No | Include NIH and ZINC filters |
| `include_chembl_alerts` | bool | No | Include ChEMBL pharma filters |
| `include_standardization` | bool | No | Run standardization pipeline |

**Response:**

```json
{
  "job_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "pending",
  "total_molecules": 10000,
  "message": "Job submitted. Processing 10000 molecules."
}
```

### POST /batch/detect-columns

Detect columns in delimited text file for SMILES and Name selection.

### GET /batch/\{job_id\}

Get batch job results with pagination and filtering.

**Query Parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `page` | int | Page number (default: 1) |
| `page_size` | int | Results per page (1-100, default: 50) |
| `status_filter` | string | Filter by `success` or `error` |
| `min_score` | int | Minimum validation score (0-100) |
| `max_score` | int | Maximum validation score (0-100) |
| `sort_by` | string | Sort field |
| `sort_dir` | string | Sort direction (`asc`, `desc`) |

### GET /batch/\{job_id\}/status

Lightweight status check for a batch job.

**Response:**

```json
{
  "job_id": "550e8400-...",
  "status": "processing",
  "progress": 45.5,
  "processed": 455,
  "total": 1000,
  "eta_seconds": 68
}
```

### GET /batch/\{job_id\}/stats

Get aggregate statistics for a batch job (without individual results).

### DELETE /batch/\{job_id\}

Cancel a running batch job.

## Export

### GET /batch/\{job_id\}/export

Export batch results to a file.

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `format` | string | Yes | `csv`, `excel`, `sdf`, `json`, or `pdf` |
| `score_min` | int | No | Minimum validation score filter |
| `score_max` | int | No | Maximum validation score filter |
| `status` | string | No | Filter by `success`, `error`, or `warning` |
| `indices` | string | No | Comma-separated molecule indices |

**Response:** File download with appropriate `Content-Disposition` header

### POST /batch/\{job_id\}/export

Export with molecule selection via request body (for large selections).

**Request:**

```json
{
  "indices": [0, 1, 5, 23, 42]
}
```

Query parameters same as GET version.

## Database Integrations

### POST /integrations/pubchem/lookup

Search PubChem database.

**Request:**

```json
{
  "molecule": "CCO",
  "format": "smiles"
}
```

**Response:**

```json
{
  "found": true,
  "cid": 702,
  "iupac_name": "ethanol",
  "synonyms": ["ethanol", "ethyl alcohol"],
  "url": "https://pubchem.ncbi.nlm.nih.gov/compound/702"
}
```

### POST /integrations/chembl/bioactivity

Search ChEMBL for bioactivity data.

### POST /integrations/coconut/lookup

Search COCONUT natural products database.

## API Key Management

All endpoints require admin authentication via `X-Admin-Secret` header.

### POST /api-keys

Create a new API key.

**Request:**

```json
{
  "name": "my-application",
  "description": "Key for production use",
  "expiry_days": 90
}
```

**Response (201):**

```json
{
  "key": "the-full-api-key-shown-only-once",
  "name": "my-application",
  "created_at": "2026-02-01T00:00:00Z",
  "expires_at": "2026-05-02T00:00:00Z"
}
```

### GET /api-keys

List all API keys (metadata only, not the actual key values).

### DELETE /api-keys/\{key_id\}

Revoke an API key. Returns 204 No Content on success.

## Next Steps

- **[Authentication](/docs/api/authentication)** - API key setup
- **[WebSocket](/docs/api/websocket)** - Real-time batch updates
- **[Error Handling](/docs/api/error-handling)** - Error responses
- **[Rate Limits](/docs/api/rate-limits)** - Rate limit details
- **[Interactive Docs](http://localhost:8001/api/v1/docs)** - Try the API
