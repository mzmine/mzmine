<div align="center">

<img src="assets/logo.png" alt="ChemAudit" width="80" />

# API Reference

### Complete REST API Documentation

[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-85EA2D?logo=swagger&logoColor=white)](http://localhost:8001/api/v1/docs)

**Interactive Docs:** http://localhost:8001/api/v1/docs | **ReDoc:** http://localhost:8001/api/v1/redoc

</div>

---

## Table of Contents

- [Overview](#overview)
- [Authentication](#authentication)
- [CSRF Protection](#csrf-protection)
- [Endpoints](#endpoints)
  - [Health](#health)
  - [Configuration](#configuration)
  - [Validation](#validation)
  - [Structural Alerts](#structural-alerts)
  - [Scoring](#scoring)
  - [Standardization](#standardization)
  - [Batch Processing](#batch-processing)
  - [Export](#export)
  - [Database Integrations](#database-integrations)
  - [API Key Management](#api-key-management)
- [WebSocket](#websocket)
- [Error Handling](#error-handling)
- [Rate Limits](#rate-limits)

---

## Overview

### Base URL

**Development (Docker):**
```
http://localhost:8001/api/v1
```

**Production (behind Nginx):**
```
http://localhost/api/v1
```

### Content Type

All JSON requests must include:
```
Content-Type: application/json
```

File uploads use `multipart/form-data`.

### Response Format

Responses are returned directly as JSON objects (no wrapper envelope). Each endpoint defines its own response schema.

---

## Authentication

Authentication is **optional** for development. For production, use API keys:

```bash
curl -H "X-API-Key: your-api-key" \
  http://localhost:8001/api/v1/validate
```

API keys are managed via the `/api-keys` endpoints (requires admin secret). Keys are stored as hashes in Redis with expiration support. Authenticated requests get higher rate limits (300 req/min vs 10 req/min).

---

## CSRF Protection

Browser-based requests (those with an `Origin` header matching configured CORS origins) require a CSRF token for state-changing methods (POST, PUT, DELETE, PATCH).

```bash
# 1. Fetch a CSRF token
curl http://localhost:8001/api/v1/csrf-token
# {"csrf_token": "..."}

# 2. Include it in subsequent requests
curl -X POST http://localhost:8001/api/v1/validate \
  -H "Content-Type: application/json" \
  -H "X-CSRF-Token: your-csrf-token" \
  -d '{"molecule": "CCO"}'
```

CSRF validation is skipped for API key authenticated requests (`X-API-Key` header).

---

## Endpoints

### Health

#### `GET /health`

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

---

### Configuration

#### `GET /config`

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

---

### Validation

#### `POST /validate`

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
      "message": "All atoms have valid valence",
      "affected_atoms": [],
      "details": {}
    }
  ],
  "execution_time_ms": 12,
  "cached": false
}
```

---

#### `POST /validate/async`

Validate using the Celery high-priority queue. Use when batch jobs are running and you need responsive single validation.

**Query Parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `timeout` | int | Max wait time in seconds (1-60, default: 30) |

**Request:** Same as `POST /validate`

**Response:** Same as `POST /validate` with additional `queue` field.

---

#### `GET /checks`

List available validation checks grouped by category.

**Response:**
```json
{
  "basic": ["valence_check", "kekulize_check", "sanitization_check"],
  "stereo": ["undefined_stereo_check", "stereo_consistency_check"],
  "representation": ["smiles_length_check", "inchi_generation_check"]
}
```

---

### Structural Alerts

#### `POST /alerts`

Screen molecule for structural alerts (PAINS, BRENK, NIH, ZINC, ChEMBL).

**Request:**
```json
{
  "molecule": "c1ccc2c(c1)nc(n2)Sc3nnnn3C",
  "format": "smiles",
  "catalogs": ["PAINS"]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `molecule` | string | Yes | Molecule to screen |
| `format` | string | No | Input format (default: `auto`) |
| `catalogs` | array | No | Alert catalogs to screen against (default: `["PAINS"]`) |

**Response:**
```json
{
  "status": "completed",
  "molecule_info": {
    "input_string": "c1ccc2c(c1)nc(n2)Sc3nnnn3C",
    "canonical_smiles": "Cn1nnnc1Sc1nc2ccccc2[nH]1",
    "molecular_formula": "C9H8N5S",
    "num_atoms": 15
  },
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
  "screened_catalogs": ["PAINS_A", "PAINS_B", "PAINS_C"],
  "has_critical": false,
  "has_warning": true,
  "execution_time_ms": 8
}
```

---

#### `POST /alerts/quick-check`

Fast check for presence of alerts (no detailed results).

**Request:** Same as `POST /alerts`

**Response:**
```json
{
  "has_alerts": true,
  "checked_catalogs": ["PAINS"]
}
```

---

#### `GET /alerts/catalogs`

List available structural alert catalogs.

**Response:**
```json
{
  "catalogs": {
    "PAINS": {
      "name": "PAINS",
      "description": "Pan-Assay Interference Compounds",
      "pattern_count": 480,
      "severity": "warning",
      "note": "87 FDA-approved drugs match PAINS patterns"
    },
    "BRENK": {
      "name": "BRENK",
      "description": "Unwanted chemical moieties",
      "pattern_count": 105,
      "severity": "warning"
    }
  },
  "default_catalogs": ["PAINS"]
}
```

---

### Scoring

#### `POST /score`

Calculate comprehensive molecular scores.

**Request:**
```json
{
  "molecule": "CCO",
  "format": "smiles",
  "include": ["ml_readiness", "np_likeness", "scaffold", "druglikeness", "safety_filters", "admet", "aggregator"]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `molecule` | string | Yes | Molecule to score |
| `format` | string | No | Input format (default: `auto`) |
| `include` | array | No | Score types to calculate (default: all) |

**Available score types:**

| Type | Description |
|------|-------------|
| `ml_readiness` | ML-readiness score (0-100) with descriptor/fingerprint breakdown |
| `np_likeness` | Natural product likeness score (-5 to +5) |
| `scaffold` | Murcko scaffold extraction |
| `druglikeness` | Drug-likeness filters (Lipinski, QED, Veber, Ro3, Ghose, Egan, Muegge) |
| `safety_filters` | Safety alerts (PAINS, Brenk, NIH, ZINC, ChEMBL pharma filters) |
| `admet` | ADMET predictions (SAscore, ESOL, Fsp3, CNS MPO, Pfizer/GSK rules, Golden Triangle) |
| `aggregator` | Aggregator likelihood prediction |

<details>
<summary><b>Example Response</b></summary>

```json
{
  "molecule_info": {
    "input_string": "CCO",
    "canonical_smiles": "CCO",
    "molecular_formula": "C2H6O",
    "molecular_weight": 46.07
  },
  "ml_readiness": {
    "score": 85,
    "breakdown": {
      "descriptors_score": 30,
      "descriptors_max": 30,
      "descriptors_successful": 200,
      "descriptors_total": 200,
      "fingerprints_score": 40,
      "fingerprints_max": 40,
      "fingerprints_successful": ["morgan", "rdkit", "maccs", "topological"],
      "fingerprints_failed": [],
      "size_score": 15,
      "size_max": 30,
      "molecular_weight": 46.07,
      "num_atoms": 3,
      "size_category": "small"
    },
    "interpretation": "Good ML candidate",
    "failed_descriptors": []
  },
  "np_likeness": {
    "score": -1.2,
    "interpretation": "Synthetic-like molecule",
    "caveats": [],
    "details": {}
  },
  "scaffold": {
    "scaffold_smiles": null,
    "generic_scaffold_smiles": null,
    "has_scaffold": false,
    "message": "No ring system found",
    "details": {}
  },
  "druglikeness": {
    "lipinski": {
      "passed": true,
      "violations": 0,
      "mw": 46.07,
      "logp": -0.31,
      "hbd": 1,
      "hba": 1,
      "details": {}
    },
    "qed": {
      "score": 0.41,
      "properties": {},
      "interpretation": "Low drug-likeness"
    },
    "veber": { "passed": true, "rotatable_bonds": 0, "tpsa": 20.23 },
    "ro3": { "passed": true, "violations": 0 },
    "ghose": { "passed": false, "violations": 2 },
    "egan": { "passed": true },
    "muegge": { "passed": false, "violations": 1 },
    "interpretation": "Passes Lipinski and Veber rules"
  },
  "safety_filters": {
    "pains": { "passed": true, "alerts": [], "alert_count": 0 },
    "brenk": { "passed": true, "alerts": [], "alert_count": 0 },
    "nih": { "passed": true, "alerts": [], "alert_count": 0 },
    "zinc": { "passed": true, "alerts": [], "alert_count": 0 },
    "chembl": { "passed": true, "total_alerts": 0 },
    "all_passed": true,
    "total_alerts": 0,
    "interpretation": "No safety alerts detected"
  },
  "admet": {
    "synthetic_accessibility": {
      "score": 1.0,
      "classification": "Easy",
      "interpretation": "Very easy to synthesize"
    },
    "solubility": {
      "log_s": -0.74,
      "solubility_mg_ml": 8.38,
      "classification": "Soluble",
      "interpretation": "Good aqueous solubility"
    },
    "complexity": {
      "fsp3": 0.5,
      "num_stereocenters": 0,
      "num_rings": 0,
      "num_aromatic_rings": 0,
      "bertz_ct": 2.0,
      "classification": "Low complexity"
    },
    "bioavailability": {
      "oral_absorption_likely": true,
      "cns_penetration_likely": true
    },
    "pfizer_rule": { "passed": true },
    "gsk_rule": { "passed": true },
    "golden_triangle": { "in_golden_triangle": false }
  },
  "aggregator": {
    "likelihood": "Low",
    "risk_score": 0.1,
    "logp": -0.31,
    "tpsa": 20.23,
    "mw": 46.07,
    "aromatic_rings": 0,
    "risk_factors": [],
    "interpretation": "Low risk of aggregation"
  },
  "execution_time_ms": 45
}
```

</details>

---

### Standardization

#### `POST /standardize`

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

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `molecule` | string | Yes | Molecule to standardize |
| `format` | string | No | Input format (default: `auto`) |
| `options.include_tautomer` | bool | No | Canonicalize tautomers (default: `false`, may lose E/Z stereo) |
| `options.preserve_stereo` | bool | No | Preserve stereochemistry (default: `true`) |

**Response:**
```json
{
  "molecule_info": {
    "input_smiles": "CC(=O)Oc1ccccc1C(=O)[O-].[Na+]",
    "canonical_smiles": "CC(=O)OC1=CC=CC=C1C(=O)[O-].[Na+]"
  },
  "result": {
    "original_smiles": "CC(=O)Oc1ccccc1C(=O)[O-].[Na+]",
    "standardized_smiles": "CC(=O)OC1=CC=CC=C1C(O)=O",
    "success": true,
    "error_message": null,
    "steps_applied": [
      {
        "step_name": "standardizer",
        "applied": true,
        "description": "Applied standardization rules",
        "changes": []
      },
      {
        "step_name": "get_parent",
        "applied": true,
        "description": "Extracted parent molecule",
        "changes": ["Removed fragment: [Na+]"]
      }
    ],
    "checker_issues": [],
    "excluded_fragments": ["[Na+]"],
    "stereo_comparison": null,
    "structure_comparison": {
      "original_atom_count": 14,
      "standardized_atom_count": 13,
      "original_formula": "C9H7NaO4",
      "standardized_formula": "C9H8O4",
      "original_mw": 202.14,
      "standardized_mw": 180.16,
      "mass_change_percent": -10.87,
      "is_identical": false,
      "diff_summary": "Salt removed"
    },
    "mass_change_percent": -10.87
  },
  "execution_time_ms": 15
}
```

---

#### `GET /standardize/options`

Get available standardization options with descriptions.

**Response:**
```json
{
  "options": {
    "include_tautomer": {
      "type": "boolean",
      "default": false,
      "description": "Include tautomer canonicalization",
      "warning": "May lose E/Z double bond stereochemistry"
    },
    "preserve_stereo": {
      "type": "boolean",
      "default": true,
      "description": "Attempt to preserve stereochemistry during standardization"
    }
  },
  "pipeline_steps": [
    {"name": "checker", "description": "Detect structural issues", "always_run": true},
    {"name": "standardizer", "description": "Fix common issues", "always_run": true},
    {"name": "get_parent", "description": "Extract parent molecule, remove salts", "always_run": true},
    {"name": "tautomer_canonicalization", "description": "Canonicalize tautomers", "always_run": false}
  ]
}
```

---

### Batch Processing

#### `POST /batch/upload`

Upload file for batch processing. Accepts SDF, CSV, TSV, and TXT files.

**Request:** `multipart/form-data`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | file | Yes | SDF, CSV, TSV, or TXT file |
| `smiles_column` | string | No | Column name for SMILES in CSV (default: `SMILES`) |
| `name_column` | string | No | Column name for molecule names/IDs |
| `include_extended_safety` | bool | No | Include NIH and ZINC filters (default: `false`) |
| `include_chembl_alerts` | bool | No | Include ChEMBL pharma filters (default: `false`) |
| `include_standardization` | bool | No | Run ChEMBL standardization pipeline (default: `false`) |

**Response:**
```json
{
  "job_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "pending",
  "total_molecules": 10000,
  "message": "Job submitted. Processing 10000 molecules."
}
```

---

#### `POST /batch/detect-columns`

Detect columns in a delimited text file for SMILES and Name/ID selection.

**Request:** `multipart/form-data` with `file` field (.csv, .tsv, .txt)

**Response:**
```json
{
  "columns": ["Name", "SMILES", "Activity", "MW"],
  "suggested_smiles": "SMILES",
  "suggested_name": "Name",
  "column_samples": {
    "Name": "Aspirin",
    "SMILES": "CC(=O)Oc1ccccc1C(=O)O",
    "Activity": "Active",
    "MW": "180.16"
  },
  "row_count_estimate": 5000,
  "file_size_mb": 1.2
}
```

---

#### `GET /batch/{job_id}`

Get batch job results with pagination and filtering.

**Query Parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `page` | int | Page number (default: 1) |
| `page_size` | int | Results per page (1-100, default: 50) |
| `status_filter` | string | Filter by `success` or `error` |
| `min_score` | int | Minimum validation score (0-100) |
| `max_score` | int | Maximum validation score (0-100) |
| `sort_by` | string | Sort field (`index`, `name`, `smiles`, `score`, `qed`, `safety`, `status`, `issues`) |
| `sort_dir` | string | Sort direction (`asc`, `desc`) |

**Response:**
```json
{
  "job_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "complete",
  "statistics": {
    "total": 1000,
    "successful": 985,
    "errors": 15,
    "avg_validation_score": 87.5,
    "avg_ml_readiness_score": 72.3,
    "avg_qed_score": 0.45,
    "avg_sa_score": 3.2,
    "lipinski_pass_rate": 0.82,
    "safety_pass_rate": 0.91,
    "score_distribution": {"0-20": 5, "21-40": 10, "41-60": 50, "61-80": 300, "81-100": 635},
    "alert_summary": {"PAINS": 12, "BRENK": 8},
    "issue_summary": {"valence_check": 5, "stereo_check": 10},
    "processing_time_seconds": 125.4
  },
  "results": [
    {
      "smiles": "CCO",
      "name": "Ethanol",
      "index": 0,
      "status": "success",
      "validation": {},
      "alerts": {},
      "scoring": {},
      "standardization": null
    }
  ],
  "page": 1,
  "page_size": 50,
  "total_results": 1000,
  "total_pages": 20
}
```

---

#### `GET /batch/{job_id}/status`

Lightweight status check for a batch job.

**Response:**
```json
{
  "job_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "processing",
  "progress": 45.5,
  "processed": 455,
  "total": 1000,
  "eta_seconds": 68,
  "error_message": null
}
```

---

#### `GET /batch/{job_id}/stats`

Get aggregate statistics for a batch job (without individual results).

**Response:** Same `statistics` object as in `GET /batch/{job_id}`.

---

#### `DELETE /batch/{job_id}`

Cancel a running batch job.

**Response:**
```json
{
  "job_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "cancelling",
  "message": "Job cancellation requested"
}
```

---

### Export

#### `GET /batch/{job_id}/export`

Export batch results to a file.

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `format` | string | Yes | `csv`, `excel`, `sdf`, `json`, or `pdf` |
| `score_min` | int | No | Minimum validation score filter |
| `score_max` | int | No | Maximum validation score filter |
| `status` | string | No | Filter by `success`, `error`, or `warning` |
| `indices` | string | No | Comma-separated molecule indices (e.g., `0,1,5,23`) |

**Response:** File download with appropriate `Content-Disposition` header.

**Supported formats:**

| Format | Extension | Description |
|--------|-----------|-------------|
| `csv` | `.csv` | Comma-separated values with all validation data |
| `excel` | `.xlsx` | Formatted spreadsheet with conditional coloring and summary |
| `sdf` | `.sdf` | Structure-data file with properties |
| `json` | `.json` | Full result objects with metadata |
| `pdf` | `.pdf` | Report with charts, statistics, and molecule images |

---

#### `POST /batch/{job_id}/export`

Export with molecule selection via request body (for large selections exceeding URL limits).

**Request:**
```json
{
  "indices": [0, 1, 5, 23, 42]
}
```

Query parameters same as GET version.

---

### Database Integrations

#### `POST /integrations/pubchem/lookup`

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
  "molecular_formula": "C2H6O",
  "molecular_weight": 46.07,
  "synonyms": ["ethanol", "ethyl alcohol"],
  "url": "https://pubchem.ncbi.nlm.nih.gov/compound/702"
}
```

---

#### `POST /integrations/chembl/bioactivity`

Search ChEMBL for bioactivity data.

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
  "chembl_id": "CHEMBL545",
  "pref_name": "ETHANOL",
  "max_phase": 4,
  "bioactivity_count": 1250,
  "bioactivities": [
    {
      "target_chembl_id": "CHEMBL240",
      "target_name": "GABA receptor",
      "activity_type": "IC50",
      "activity_value": 100.0,
      "activity_unit": "nM"
    }
  ],
  "url": "https://www.ebi.ac.uk/chembl/compound_report_card/CHEMBL545"
}
```

---

#### `POST /integrations/coconut/lookup`

Search COCONUT natural products database.

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
  "coconut_id": "CNP0123456",
  "name": "Compound Name",
  "smiles": "CCO",
  "molecular_weight": 46.07,
  "organism": "Genus species",
  "url": "https://coconut.naturalproducts.net/compounds/CNP0123456"
}
```

---

### API Key Management

All endpoints require admin authentication via `X-Admin-Secret` header.

#### `POST /api-keys`

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

---

#### `GET /api-keys`

List all API keys (metadata only, not the actual key values).

**Response:**
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

---

#### `DELETE /api-keys/{key_id}`

Revoke an API key. Returns 204 No Content on success.

---

## WebSocket

### `WS /ws/batch/{job_id}`

Real-time batch processing progress updates via WebSocket.

**Connect after uploading a file:**
```javascript
const ws = new WebSocket('ws://localhost:8001/ws/batch/' + jobId);

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log(`Progress: ${data.progress}% (${data.processed}/${data.total})`);
};

// Keep alive
setInterval(() => ws.send('ping'), 30000);
```

**Message format:**
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

**Status values:** `processing`, `complete`, `failed`, `cancelled`

---

## Error Handling

### Error Response Format

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

Or for simple errors:
```json
{
  "detail": "Job abc123 not found"
}
```

### HTTP Status Codes

| Code | Description |
|------|-------------|
| 400 | Invalid input (bad SMILES, unsupported format, file too large) |
| 403 | IP banned or CSRF validation failed |
| 404 | Resource not found (job ID, API key) |
| 429 | Rate limit exceeded |
| 500 | Internal server error |
| 504 | Timeout (async validation) |

---

## Rate Limits

Default rate limits (anonymous / unauthenticated):

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

API key authenticated requests: **300 req/min** for all endpoints.

Repeated rate limit violations (100+ within the tracking window) result in a temporary IP ban (default: 60 minutes).

**Rate Limit Headers:**
```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 8
X-RateLimit-Reset: 1706500800
```

---

<div align="center">

**Interactive API Explorer:** http://localhost:8001/api/v1/docs

</div>
