---
sidebar_position: 7
title: Exporting Results
description: Export batch processing results in CSV, Excel, SDF, JSON, and PDF formats
---

# Exporting Results

Export your batch processing results in multiple formats for analysis, reporting, and data exchange. ChemAudit supports CSV, Excel, SDF, JSON, and PDF exports with flexible filtering options.

## Available Export Formats

| Format | Extension | Use Case | Features |
|--------|-----------|----------|----------|
| **CSV** | `.csv` | Spreadsheet analysis, data pipelines | Simple tabular data |
| **Excel** | `.xlsx` | Formatted reports, presentations | Conditional coloring, summary sheet |
| **SDF** | `.sdf` | Structure-data exchange | Compatible with chemistry tools |
| **JSON** | `.json` | Programmatic processing | Full data fidelity, nested structures |
| **PDF** | `.pdf` | Professional reports, archival | Charts, molecule images, statistics |

## How to Export

### Web Interface

1. Complete a batch processing job
2. Click the **Export** button
3. Select export format
4. (Optional) Apply filters:
   - Minimum/maximum validation score
   - Success or error status
   - Specific molecule indices
5. Click **Download**

The file downloads automatically with a descriptive filename including job ID and timestamp.

### API - Export All Results

```bash
# Export all results as CSV
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=csv" -o results.csv

# Export as Excel
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=excel" -o results.xlsx

# Export as SDF
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=sdf" -o results.sdf

# Export as JSON
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=json" -o results.json

# Export as PDF report
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=pdf" -o report.pdf
```

### API - Export with Filters

```bash
# Export molecules with validation score >= 80
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=csv&score_min=80" -o high_quality.csv

# Export only successful validations
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=excel&status=success" -o successful.xlsx

# Export specific score range
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=sdf&score_min=70&score_max=90" -o medium_scores.sdf
```

### API - Export Selected Molecules

For small selections (up to 200 indices), use GET with comma-separated indices:

```bash
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=csv&indices=0,1,5,23,42" -o selected.csv
```

For large selections (> 200 indices), use POST with JSON body:

```bash
curl -X POST "http://localhost:8001/api/v1/batch/{job_id}/export?format=json" \
  -H "Content-Type: application/json" \
  -d '{"indices": [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]}' \
  -o selected.json
```

## Format Details

### CSV Export

Simple tabular format with these columns:

- Index
- SMILES
- Name (if provided)
- Validation score
- Status (success/error)
- Number of issues
- ML-readiness score
- QED score
- Safety pass/fail
- Alert counts (PAINS, BRENK, etc.)
- Molecular formula
- Molecular weight

**Pros:**
- Universal compatibility
- Easy to import into spreadsheets or databases
- Small file size

**Cons:**
- No nested data structures
- No formatting or charts
- Limited molecule information

### Excel Export

Formatted spreadsheet with two sheets:

**Results Sheet:**
- All molecule data in table format
- Conditional formatting (red for errors, green for high scores)
- Frozen header row
- Auto-sized columns

**Summary Sheet:**
- Job statistics
- Score distribution
- Alert summary
- Processing metadata

**Pros:**
- Professional appearance
- Ready for presentations
- Includes summary statistics
- Conditional coloring highlights issues

**Cons:**
- Larger file size than CSV
- Requires Excel or compatible software

### SDF Export

Structure-Data File format with molecular structures and properties:

```
Molecule_0
  RDKit          2D

 13 13  0  0  0  0  0  0  0  0999 V2000
    ... coordinates ...
M  END
> <validation_score>
95

> <qed>
0.65

> <ml_readiness_score>
85

$$$$
```

**Pros:**
- Preserves 2D/3D structure coordinates
- Compatible with chemistry software (RDKit, MOE, Schrodinger)
- Can include custom properties

**Cons:**
- Larger file size
- Not human-readable for data analysis

:::tip Chemistry Tools
SDF is the best format for importing into molecular modeling, docking, or cheminformatics tools.
:::

### JSON Export

Complete data in JSON format with nested structures:

```json
[
  {
    "index": 0,
    "smiles": "CCO",
    "name": "Ethanol",
    "status": "success",
    "validation": {
      "overall_score": 95,
      "issues": [],
      "all_checks": [...]
    },
    "alerts": {
      "total_alerts": 0,
      "alerts": []
    },
    "scoring": {
      "ml_readiness": {...},
      "druglikeness": {...},
      "admet": {...}
    }
  }
]
```

**Pros:**
- Complete data preservation
- Nested structures maintained
- Easy to parse programmatically
- Standard format for APIs

**Cons:**
- Larger file size
- Not suitable for spreadsheet analysis

### PDF Report

Professional report document with:

1. **Executive Summary**
   - Job metadata (date, molecule count, processing time)
   - Overall statistics
   - Key findings

2. **Score Distribution Charts**
   - Validation score histogram
   - ML-readiness distribution
   - QED distribution

3. **Molecule Gallery**
   - Structure images (up to 100 molecules)
   - Key properties
   - Validation scores
   - Alert indicators

4. **Failed Molecules Section**
   - Error messages
   - Failure reasons
   - Recommendations

5. **Alert Summary**
   - Alert counts by catalog
   - Most common patterns
   - Risk assessment

6. **Processing Metadata**
   - Configuration used
   - Processing time
   - Worker statistics

**Pros:**
- Professional presentation
- Self-contained (no external dependencies)
- Suitable for archival and sharing
- Includes visualizations

**Cons:**
- Largest file size
- Not suitable for further processing
- Limited to summary information for large batches

:::warning PDF Size Limits
For batches larger than 1,000 molecules, PDF exports include only summary statistics and the first 100 molecules to keep file size manageable.
:::

## Filter Options

### Score Filters

| Parameter | Type | Description |
|-----------|------|-------------|
| `score_min` | integer (0-100) | Minimum validation score |
| `score_max` | integer (0-100) | Maximum validation score |

**Example:**
```bash
# Export only high-quality molecules (score >= 90)
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=csv&score_min=90"
```

### Status Filters

| Parameter | Values | Description |
|-----------|--------|-------------|
| `status` | `success`, `error`, `warning` | Processing outcome |

**Example:**
```bash
# Export only failed molecules for debugging
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=csv&status=error"
```

### Molecule Selection

| Parameter | Type | Description |
|-----------|------|-------------|
| `indices` | string or array | Specific molecule indices |

**GET Example (small selections):**
```bash
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=sdf&indices=0,1,2,3"
```

**POST Example (large selections):**
```bash
curl -X POST "http://localhost:8001/api/v1/batch/{job_id}/export?format=json" \
  -d '{"indices": [0,1,2,3,4,5,6,7,8,9]}'
```

## Combining Filters

Filters can be combined for precise exports:

```bash
# High-quality successful molecules with specific indices
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=excel&status=success&score_min=80&indices=0,5,10,15"
```

## Best Practices

1. **Choose the right format**:
   - CSV for data analysis
   - Excel for reports and presentations
   - SDF for chemistry software
   - JSON for programmatic processing
   - PDF for archival and sharing

2. **Use filters to reduce file size**:
   - Export only relevant molecules
   - Filter by score to focus on quality
   - Separate successes and errors

3. **Export multiple times for different purposes**:
   - CSV for your database
   - Excel for stakeholder reports
   - SDF for molecular modeling
   - PDF for documentation

4. **Preserve raw data**:
   - Keep original batch results for reproducibility
   - Export JSON for complete data preservation

## Next Steps

- **[Batch Processing](/docs/user-guide/batch-processing)** - Process large datasets
- **[API Reference](/docs/api/endpoints)** - Full export API documentation
