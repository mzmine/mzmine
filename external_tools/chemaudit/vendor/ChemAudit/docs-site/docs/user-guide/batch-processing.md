---
sidebar_position: 2
title: Batch Processing
description: Process large datasets with up to 1 million molecules and real-time progress tracking
---

# Batch Processing

Process large datasets efficiently with ChemAudit's batch validation system. Handle from hundreds to millions of molecules with real-time WebSocket progress updates.

## Specifications

Batch processing capabilities vary by deployment profile:

| Feature | Range | Notes |
|---------|-------|-------|
| **Max Molecules** | 1,000 - 1,000,000 | Configurable per profile |
| **Max File Size** | 100 MB - 1 GB | Depends on deployment |
| **Supported Formats** | SDF, CSV, TSV, TXT | Auto-detected |
| **Progress Updates** | Real-time | Via WebSocket |
| **Worker Queues** | Default + Priority | Separate queues for responsiveness |

:::info Check Your Limits
Your deployment limits are available at `/api/v1/config`. The frontend displays these automatically.
:::

## Supported File Formats

### SDF Files

Structure-Data Files are the preferred format for batch processing:

```
Aspirin
  RDKit          2D

 13 13  0  0  0  0  0  0  0  0999 V2000
   ...atom coordinates...
M  END
$$$$
Caffeine
  RDKit          2D
   ...
$$$$
```

:::tip SDF Benefits
SDF files preserve 2D/3D coordinates, can include properties, and are widely supported by chemistry software.
:::

### CSV/TSV Files

Delimited text files must have a column containing SMILES strings:

```csv
Name,SMILES,Activity,MW
Aspirin,CC(=O)Oc1ccccc1C(=O)O,Active,180.16
Caffeine,Cn1cnc2c1c(=O)n(c(=O)n2C)C,Active,194.19
Ethanol,CCO,Inactive,46.07
```

**CSV Requirements:**

- Must have a header row
- SMILES column is required
- Optional name/ID column
- UTF-8 encoding recommended

**Supported SMILES column names:**
- `SMILES`, `smiles`, `Smiles`
- `CANONICAL_SMILES`, `canonical_smiles`
- Or select manually during upload

## How to Process Batch Files

### Web Interface

1. **Navigate** to the Batch Processing page
2. **Drag and drop** your file or click to browse
3. **For CSV/TSV**: Select the SMILES column and optional Name column
4. **Configure options**:
   - Extended safety filters (NIH, ZINC)
   - ChEMBL alerts
   - Standardization pipeline
5. **Click** "Upload and Process"
6. **Monitor** real-time progress via WebSocket
7. **View results** with sorting, filtering, and pagination
8. **Export** results in your preferred format

:::tip Column Detection
ChemAudit automatically suggests likely SMILES and Name columns based on content analysis.
:::

### API

#### Upload and Start Processing

```bash
# Upload SDF file
curl -X POST http://localhost:8001/api/v1/batch/upload \
  -F "file=@molecules.sdf"

# Upload CSV with column selection
curl -X POST http://localhost:8001/api/v1/batch/upload \
  -F "file=@molecules.csv" \
  -F "smiles_column=SMILES" \
  -F "name_column=Name" \
  -F "include_extended_safety=true"
```

Response:
```json
{
  "job_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "pending",
  "total_molecules": 10000,
  "message": "Job submitted. Processing 10000 molecules."
}
```

#### Check Progress

```bash
curl http://localhost:8001/api/v1/batch/{job_id}/status
```

Response:
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

#### Get Results

```bash
# Get paginated results
curl "http://localhost:8001/api/v1/batch/{job_id}?page=1&page_size=50"

# Get statistics only
curl http://localhost:8001/api/v1/batch/{job_id}/stats
```

## Processing Options

| Option | Description | Default |
|--------|-------------|---------|
| **include_extended_safety** | Screen against NIH and ZINC filters | `false` |
| **include_chembl_alerts** | Screen against ChEMBL pharma filters | `false` |
| **include_standardization** | Run ChEMBL standardization pipeline | `false` |

:::warning Performance Impact
Enabling all options increases processing time. For large batches, consider running with basic options first, then re-process specific molecules if needed.
:::

## Filtering and Sorting Results

Filter and sort results to focus on molecules of interest:

### Available Filters

| Filter | Type | Description |
|--------|------|-------------|
| **Status** | `success` or `error` | Processing outcome |
| **Min/Max Score** | 0-100 | Validation score range |
| **Sort By** | Various | See sort fields below |

### Sort Fields

- `index`: Original file order
- `name`: Molecule name (if provided)
- `smiles`: SMILES string alphabetically
- `score`: Validation score
- `qed`: QED drug-likeness score
- `safety`: Safety filter score
- `status`: Success/error status
- `issues`: Number of validation issues

### Sort Direction

- `asc`: Ascending order
- `desc`: Descending order

**Example:**

```bash
# Get molecules with score >= 80, sorted by QED descending
curl "http://localhost:8001/api/v1/batch/{job_id}?min_score=80&sort_by=qed&sort_dir=desc"
```

## Real-Time Progress Updates

ChemAudit provides real-time progress via WebSocket:

### JavaScript Example

```javascript
const ws = new WebSocket('ws://localhost:8001/ws/batch/' + jobId);

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log(`Progress: ${data.progress}% (${data.processed}/${data.total})`);
  console.log(`ETA: ${data.eta_seconds} seconds`);
};

// Send keep-alive pings
setInterval(() => ws.send('ping'), 30000);

ws.onclose = () => {
  console.log('Job complete or connection closed');
};
```

### Message Format

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

**Status values:**
- `processing`: Job in progress
- `complete`: Job finished successfully
- `failed`: Job encountered fatal error
- `cancelled`: Job was cancelled

:::info Developer Tip
For Python integration, see the [WebSocket API documentation](/docs/api/websocket).
:::

## Understanding Results

### Statistics Summary

Each batch job includes aggregate statistics:

- **Total molecules**: Count processed
- **Successful/Errors**: Success rate
- **Average scores**: Validation, ML-readiness, QED, SA
- **Pass rates**: Lipinski, safety filters
- **Score distribution**: Histogram of validation scores
- **Alert summary**: Count by catalog (PAINS, BRENK, etc.)
- **Issue summary**: Count by check type
- **Processing time**: Total duration

### Individual Results

Each molecule result includes:

- **SMILES**: Canonical SMILES
- **Name**: Molecule name (if provided)
- **Index**: Original file position
- **Status**: `success` or `error`
- **Validation**: All check results and overall score
- **Alerts**: Matched structural alerts (if screening enabled)
- **Scoring**: ML-readiness, drug-likeness, ADMET scores
- **Standardization**: Cleanup results (if enabled)

## Handling Partial Failures

ChemAudit gracefully handles molecules that fail to parse or validate:

- Failed molecules are marked with `status: "error"`
- Error messages explain the failure reason
- Processing continues for remaining molecules
- Statistics show success/failure breakdown
- Filter by status to review only errors

:::tip Debugging Failures
Export error molecules separately, fix them manually, and re-upload. The batch index helps track which molecules failed.
:::

## Performance Tips

1. **Use SDF when possible**: Faster parsing than CSV
2. **Split very large files**: Process in chunks if near size limits
3. **Monitor worker utilization**: Scale workers for better throughput
4. **Enable caching**: Results are cached by InChIKey for 1 hour
5. **Use filters wisely**: Basic validation is fastest; add options as needed

## Next Steps

- **[Exporting Results](/docs/user-guide/exporting-results)** - Export in multiple formats
- **[Structural Alerts](/docs/user-guide/structural-alerts)** - Understanding alert screening
- **[Scoring](/docs/user-guide/scoring/overview)** - Interpreting molecular scores
- **[WebSocket API](/docs/api/websocket)** - Integrate real-time progress
