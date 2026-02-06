---
sidebar_position: 3
title: Structural Alerts
description: Screen molecules for problematic substructures using industry-standard alert catalogs
---

# Structural Alerts

Structural alerts identify potentially problematic substructures in molecules. ChemAudit screens against over 1,500 patterns from industry-standard catalogs including PAINS, BRENK, NIH, ZINC, and ChEMBL filters.

## Available Alert Catalogs

### PAINS (Pan-Assay Interference Compounds)

| Patterns | Severity | Description |
|----------|----------|-------------|
| ~480 | Warning | Compounds that interfere with multiple assay types |

PAINS includes three subcatalogs:
- **PAINS_A**: Most frequent interference patterns
- **PAINS_B**: Moderate interference patterns
- **PAINS_C**: Less frequent but still problematic patterns

:::info FDA-Approved Drugs
87 FDA-approved drugs contain PAINS patterns. Alerts are warnings for investigation, not automatic rejections.
:::

### BRENK

| Patterns | Severity | Description |
|----------|----------|-------------|
| ~105 | Warning | Unwanted chemical moieties |

Filters for functional groups and substructures associated with poor pharmacokinetics, toxicity, or reactivity.

### NIH

| Patterns | Severity | Description |
|----------|----------|-------------|
| ~180 | Warning | NIH MLSMR excluded structures |

Patterns excluded from the NIH Molecular Libraries Small Molecule Repository screening collection.

### ZINC

| Patterns | Severity | Description |
|----------|----------|-------------|
| ~95 | Warning | ZINC database filters |

Filters used by the ZINC database to identify problematic compounds.

### ChEMBL Pharma Filters

| Patterns | Severity | Description |
|----------|----------|-------------|
| ~700+ | Warning | Pharmaceutical company filters |

Includes filters from:
- **BMS**: Bristol-Myers Squibb filters
- **Dundee**: University of Dundee filters
- **Glaxo**: GlaxoSmithKline filters
- **Inpharmatica**: Inpharmatica filters
- **Lint**: General medicinal chemistry filters
- **MLSMR**: Additional MLSMR patterns
- **SureChEMBL**: Patent-derived filters

## How to Screen

### Web Interface

1. Enter your molecule on the Single Validation page
2. Navigate to the **Structural Alerts** tab
3. View matched alerts with:
   - Pattern name and description
   - Severity level
   - Matched atoms (highlighted in structure)
   - Catalog source

### API - Full Screening

Get detailed information about all matched alerts:

```bash
curl -X POST http://localhost:8001/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "c1ccc2c(c1)nc(n2)Sc3nnnn3C",
    "catalogs": ["PAINS", "BRENK"]
  }'
```

Response:
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
  "screened_catalogs": ["PAINS_A", "PAINS_B", "PAINS_C", "BRENK"],
  "has_critical": false,
  "has_warning": true
}
```

### API - Quick Check

Fast yes/no check without detailed results:

```bash
curl -X POST http://localhost:8001/api/v1/alerts/quick-check \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "c1ccc2c(c1)nc(n2)Sc3nnnn3C",
    "catalogs": ["PAINS"]
  }'
```

Response:
```json
{
  "has_alerts": true,
  "checked_catalogs": ["PAINS"]
}
```

:::tip Performance
Use quick-check for large-scale filtering, then run full screening on flagged molecules for details.
:::

## Interpreting Results

### Severity Levels

| Severity | Meaning | Action |
|----------|---------|--------|
| **Warning** | Potentially problematic | Investigate further, review literature |
| **Critical** | Definitively problematic | Avoid or have strong justification |

Most structural alerts are classified as warnings because context matters. A PAINS alert in a screening library compound warrants investigation, but the same pattern in a clinical candidate with supporting data may be acceptable.

### Matched Atoms

The `matched_atoms` field indicates which atoms in the molecule match the alert pattern. In the web interface, these atoms are highlighted in the structure viewer.

This helps you:
- Understand which part of the molecule triggered the alert
- Identify structural modifications to avoid the alert
- Assess whether the match is a false positive

### False Positives

Structural alert matching is based on SMARTS patterns, which can sometimes match unintended substructures. Always review matched atoms to verify the alert is relevant.

## Using Alerts in Your Workflow

### Early-Stage Screening

Screen compound libraries before purchasing or synthesizing:

```bash
# Batch process with alerts enabled
curl -X POST http://localhost:8001/api/v1/batch/upload \
  -F "file=@library.sdf" \
  -F "include_extended_safety=true" \
  -F "include_chembl_alerts=true"
```

Filter results to exclude compounds with alerts, or flag them for manual review.

### Compound Prioritization

Use alert results to prioritize compounds:

1. **No alerts**: High priority for screening
2. **PAINS only**: Medium priority, review assay compatibility
3. **Multiple catalogs**: Low priority, requires justification

### Hit Validation

When validating screening hits, check for alerts:

- Hits with PAINS alerts may be assay artifacts
- Structural modifications that remove alerts often improve hit quality
- Literature search for the alert pattern provides context

## Alert Statistics in Batch Results

Batch processing provides alert summary statistics:

```json
{
  "alert_summary": {
    "PAINS": 12,
    "BRENK": 8,
    "NIH": 3,
    "ZINC": 5,
    "ChEMBL": 15
  }
}
```

Use this to:
- Assess overall library quality
- Identify prevalent problematic patterns
- Guide library cleanup efforts

## Catalog Selection

### Default Screening

By default, ChemAudit screens against PAINS only. This provides a good balance of speed and coverage.

### Extended Safety

Enable extended safety to include NIH and ZINC filters. Use for:
- Compound library curation
- Pre-purchase screening
- Academic screening collections

### ChEMBL Alerts

Enable ChEMBL alerts for comprehensive pharma-industry filters. Use for:
- Drug discovery projects
- Lead optimization
- Industrial screening

:::warning Performance Impact
Screening all catalogs takes longer. For large batches, start with PAINS, then re-screen flagged compounds with full catalogs.
:::

## Next Steps

- **[Scoring Overview](/docs/user-guide/scoring/overview)** - Comprehensive molecular scoring
- **[Safety Filters](/docs/user-guide/scoring/safety-filters)** - Safety scoring vs. alerts
- **[Batch Processing](/docs/user-guide/batch-processing)** - Screen large libraries
- **[API Reference](/docs/api/endpoints)** - Full alerts API documentation
