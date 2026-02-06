<div align="center">

<img src="assets/logo.png" alt="ChemAudit" width="80" />

# User Guide

### Complete Guide to ChemAudit Features

</div>

---

## Table of Contents

- [Single Molecule Validation](#single-molecule-validation)
- [Batch Processing](#batch-processing)
- [Structural Alerts](#structural-alerts)
- [Scoring](#scoring)
  - [ML-Readiness](#ml-readiness-scoring)
  - [Drug-likeness](#drug-likeness)
  - [Safety Filters](#safety-filters)
  - [ADMET Predictions](#admet-predictions)
  - [NP-Likeness](#np-likeness)
  - [Scaffold Analysis](#scaffold-analysis)
  - [Aggregator Likelihood](#aggregator-likelihood)
- [Standardization](#standardization)
- [Database Integrations](#database-integrations)
- [Exporting Results](#exporting-results)

---

## Single Molecule Validation

### Supported Input Formats

| Format | Example | Auto-Detected |
|--------|---------|---------------|
| **SMILES** | `CCO` | Yes |
| **InChI** | `InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3` | Yes |
| **MOL Block** | V2000/V3000 format | Yes |

### How to Validate

**Web Interface:**
1. Navigate to the **Single Validation** page (home)
2. Enter or paste your molecule
3. Click **Validate**
4. Review results across all tabs: Validation, Alerts, Scoring, Standardization, Database Lookup

**API:**
```bash
curl -X POST http://localhost:8001/api/v1/validate \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CCO",
    "format": "auto"
  }'
```

### Validation Checks Explained

<details>
<summary><b>Critical Checks</b></summary>

| Check | Description | Common Causes |
|-------|-------------|---------------|
| **Valence** | Atoms must have valid bond counts | Typos in SMILES, incorrect charges |
| **Kekulization** | Aromatic rings must be resolvable | Invalid aromatic systems |
| **Sanitization** | Molecule must pass RDKit sanitization | Structural inconsistencies |

</details>

<details>
<summary><b>Warning Checks</b></summary>

| Check | Description | Recommendation |
|-------|-------------|----------------|
| **Undefined Stereo** | Undefined stereocenters detected | Specify R/S or E/Z if intended |
| **Stereo Consistency** | Stereo specifications are consistent | Verify intended stereochemistry |

</details>

<details>
<summary><b>Info Checks</b></summary>

| Check | Description |
|-------|-------------|
| **SMILES Length** | Reports if SMILES is unusually long |
| **InChI Generation** | Confirms InChI can be generated |

</details>

### Options

| Option | Description |
|--------|-------------|
| **preserve_aromatic** | Output SMILES with aromatic notation (lowercase atoms like `c1ccccc1`) instead of kekulized form (`C1=CC=CC=C1`) |

---

## Batch Processing

Process large datasets with real-time progress tracking.

### Specifications

| Feature | Details |
|---------|---------|
| **Max Molecules** | Configurable per deployment profile (1K - 1M) |
| **Max File Size** | Configurable per deployment profile (100MB - 1GB) |
| **Supported Formats** | SDF, CSV, TSV, TXT |
| **Progress Updates** | Real-time via WebSocket |
| **Worker Queues** | Separate default and priority queues |

### CSV Format Requirements

Your CSV must have a column containing SMILES strings:

```csv
Name,SMILES,Activity
Aspirin,CC(=O)Oc1ccccc1C(=O)O,Active
Caffeine,Cn1cnc2c1c(=O)n(c(=O)n2C)C,Active
Ethanol,CCO,Inactive
```

TSV and TXT files with tab-separated columns are also supported.

### How to Process Batch Files

**Web Interface:**
1. Navigate to **Batch Processing** page
2. Drag & drop your file or click to browse
3. For CSV/TSV files, select the SMILES column and optional Name column
4. Toggle extended safety filters, ChEMBL alerts, or standardization if desired
5. Click **Upload and Process**
6. Monitor real-time progress via WebSocket
7. View results with sorting, filtering, and pagination
8. Export results in your preferred format

**API:**
```bash
# Upload file
curl -X POST http://localhost:8001/api/v1/batch/upload \
  -F "file=@molecules.sdf"

# Response: {"job_id": "abc123", "status": "pending", "total_molecules": 1000}

# Check progress
curl http://localhost:8001/api/v1/batch/abc123/status

# Get results (paginated)
curl "http://localhost:8001/api/v1/batch/abc123?page=1&page_size=50"

# Get statistics only
curl http://localhost:8001/api/v1/batch/abc123/stats
```

### Filtering and Sorting Results

| Filter | Description |
|--------|-------------|
| **Status** | `success` or `error` |
| **Min/Max Score** | Validation score range (0-100) |
| **Sort By** | index, name, smiles, score, qed, safety, status, issues |
| **Sort Direction** | ascending or descending |

---

## Structural Alerts

Screen molecules against known problematic substructures.

### Available Alert Catalogs

| Catalog | Description | Patterns |
|---------|-------------|----------|
| **PAINS** | Pan-Assay Interference Compounds (A/B/C) | ~480 |
| **BRENK** | Unwanted chemical moieties | ~105 |
| **NIH** | NIH MLSMR excluded structures | ~180 |
| **ZINC** | ZINC database filters | ~95 |
| **ChEMBL** | Pharma company filters (BMS, Dundee, Glaxo, Inpharmatica, Lint, MLSMR, SureChEMBL) | ~700+ |

**Note:** 87 FDA-approved drugs contain PAINS patterns. Alerts are warnings for investigation, not automatic rejections.

### How to Screen

**Web Interface:**
1. Enter your molecule
2. Navigate to **Structural Alerts** tab
3. View matched alerts with severity and matched atoms

**API:**
```bash
# Full screening
curl -X POST http://localhost:8001/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "c1ccc2c(c1)nc(n2)Sc3nnnn3C",
    "catalogs": ["PAINS", "BRENK"]
  }'

# Quick check (faster, yes/no only)
curl -X POST http://localhost:8001/api/v1/alerts/quick-check \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "c1ccc2c(c1)nc(n2)Sc3nnnn3C",
    "catalogs": ["PAINS"]
  }'
```

---

## Scoring

ChemAudit provides comprehensive molecular scoring across multiple dimensions.

### ML-Readiness Scoring

Evaluate how suitable a molecule is for machine learning applications.

| Component | Weight | What It Measures |
|-----------|--------|------------------|
| **Descriptor Calculability** | 30pts | Can standard RDKit descriptors be computed? |
| **Fingerprint Generation** | 40pts | Can Morgan, RDKit, MACCS, Topological fingerprints be generated? |
| **Size/Complexity** | 30pts | Is molecular weight/atom count within ML-trainable range? |

| Score | Interpretation |
|-------|----------------|
| **80-100** | Excellent ML candidate |
| **60-79** | Good with minor caveats |
| **40-59** | May need preprocessing |
| **0-39** | Significant challenges |

### Drug-likeness

Evaluate compliance with established drug-likeness rules.

| Filter | Criteria |
|--------|----------|
| **Lipinski (Ro5)** | MW ≤ 500, LogP ≤ 5, HBD ≤ 5, HBA ≤ 10 |
| **QED** | Quantitative Estimate of Drug-likeness (0-1 score) |
| **Veber** | Rotatable bonds ≤ 10, TPSA ≤ 140 |
| **Rule of Three** | MW ≤ 300, LogP ≤ 3, HBD ≤ 3, HBA ≤ 3 |
| **Ghose** | MW 160-480, LogP -0.4-5.6, atoms 20-70, MR 40-130 |
| **Egan** | LogP ≤ 5.88, TPSA ≤ 131.6 |
| **Muegge** | Multiple criteria for druglike chemical space |

### Safety Filters

Screen against structural alert databases used in drug discovery.

| Filter | Source | Alerts |
|--------|--------|--------|
| **PAINS** | Baell & Holloway (2010) | ~480 |
| **BRENK** | Brenk et al. (2008) | ~105 |
| **NIH** | NIH MLSMR program | ~180 |
| **ZINC** | ZINC database | ~95 |
| **ChEMBL** | Pharma companies (BMS, Dundee, Glaxo, etc.) | ~700+ |

### ADMET Predictions

Absorption, Distribution, Metabolism, Excretion, and Toxicity predictions.

| Property | Method | Output |
|----------|--------|--------|
| **Synthetic Accessibility** | SAscore (1=easy, 10=hard) | Score + classification |
| **Aqueous Solubility** | ESOL (Delaney) | LogS + mg/mL + classification |
| **Complexity** | Fsp3, stereocenters, rings, Bertz CT | Classification |
| **CNS MPO** | Multi-parameter optimization score | Score (0-6) + penetrant flag |
| **Bioavailability** | TPSA, rotatable bonds, Lipinski | Oral absorption + CNS flags |
| **Pfizer 3/75 Rule** | LogP < 3, TPSA > 75 | Pass/fail + interpretation |
| **GSK 4/400 Rule** | MW ≤ 400, LogP ≤ 4 | Pass/fail + interpretation |
| **Golden Triangle** | MW 200-500, LogD -2 to 5 | In/out + interpretation |

### NP-Likeness

Natural product likeness scoring.

| Score Range | Interpretation |
|-------------|----------------|
| **> 1.0** | Natural product-like |
| **-1.0 to 1.0** | Intermediate |
| **< -1.0** | Synthetic-like |

### Scaffold Analysis

Murcko scaffold extraction for structure-activity analysis.

Returns:
- **Murcko scaffold SMILES** - Ring systems with linkers
- **Generic scaffold SMILES** - Simplified ring framework
- **Has scaffold** - Whether the molecule contains ring systems

### Aggregator Likelihood

Predicts whether a molecule is likely to form colloidal aggregates in assays.

Based on: LogP, TPSA, molecular weight, aromatic ring count, and known aggregator patterns.

**How to Get Scores (API):**
```bash
curl -X POST http://localhost:8001/api/v1/score \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CCO",
    "include": ["ml_readiness", "druglikeness", "safety_filters", "admet", "np_likeness", "scaffold", "aggregator"]
  }'
```

---

## Standardization

Standardize molecules using a ChEMBL-compatible pipeline.

### Pipeline Steps

| Step | Description | Always Runs |
|------|-------------|-------------|
| **Checker** | Detect structural issues before standardization | Yes |
| **Standardizer** | Fix common issues (nitro groups, metals, sulphoxides) | Yes |
| **Get Parent** | Extract parent molecule, remove salts and solvents | Yes |
| **Tautomer** | Canonicalize tautomers | No (opt-in) |

### Options

| Option | Default | Description |
|--------|---------|-------------|
| **include_tautomer** | `false` | Enable tautomer canonicalization (may lose E/Z stereo) |
| **preserve_stereo** | `true` | Attempt to preserve stereochemistry |

### How to Standardize

**API:**
```bash
curl -X POST http://localhost:8001/api/v1/standardize \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CC(=O)Oc1ccccc1C(=O)[O-].[Na+]",
    "options": {
      "include_tautomer": false,
      "preserve_stereo": true
    }
  }'
```

The response includes:
- Original and standardized SMILES
- Steps applied with changes
- Checker issues found
- Excluded fragments (salts, solvents)
- Stereo comparison (if stereochemistry changed)
- Structure comparison (atom count, formula, mass change)

---

## Database Integrations

Look up molecules in major chemical databases.

### Supported Databases

| Database | Data Available | Rate Limit |
|----------|----------------|------------|
| **PubChem** | Properties, synonyms, IUPAC name | 30 req/min |
| **ChEMBL** | Bioactivity, targets, clinical phase | 30 req/min |
| **COCONUT** | Natural product data, organism source | 30 req/min |

### How to Search

**Web Interface:**
1. Enter your molecule on the Single Validation page
2. Navigate to **Database Lookup** tab
3. View cross-references from PubChem, ChEMBL, and COCONUT

**API:**
```bash
# Search PubChem
curl -X POST http://localhost:8001/api/v1/integrations/pubchem/lookup \
  -H "Content-Type: application/json" \
  -d '{"molecule": "CCO", "format": "smiles"}'

# Search ChEMBL
curl -X POST http://localhost:8001/api/v1/integrations/chembl/bioactivity \
  -H "Content-Type: application/json" \
  -d '{"molecule": "CCO", "format": "smiles"}'

# Search COCONUT
curl -X POST http://localhost:8001/api/v1/integrations/coconut/lookup \
  -H "Content-Type: application/json" \
  -d '{"molecule": "CCO", "format": "smiles"}'
```

---

## Exporting Results

### Available Export Formats

| Format | Extension | Use Case |
|--------|-----------|----------|
| **CSV** | `.csv` | Spreadsheet analysis, data pipelines |
| **Excel** | `.xlsx` | Formatted report with conditional coloring and summary sheet |
| **SDF** | `.sdf` | Structure-data exchange with other chemistry tools |
| **JSON** | `.json` | Programmatic processing, full data fidelity |
| **PDF** | `.pdf` | Professional reports with charts and molecule images |

### How to Export

**Web Interface:**
1. Complete a batch processing job
2. Click the **Export** button
3. Select format and optional filters (score range, status, specific molecules)
4. Download the file

**API:**
```bash
# Export all results as CSV
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=csv" -o results.csv

# Export filtered results as Excel
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=excel&score_min=80" -o results.xlsx

# Export specific molecules as SDF
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=sdf&indices=0,1,5,23" -o results.sdf

# Export as PDF report
curl "http://localhost:8001/api/v1/batch/{job_id}/export?format=pdf" -o report.pdf

# Export large selections via POST
curl -X POST "http://localhost:8001/api/v1/batch/{job_id}/export?format=json" \
  -H "Content-Type: application/json" \
  -d '{"indices": [0, 1, 2, 3, 4, 5]}' -o results.json
```

### PDF Report Contents

- Executive summary with statistics
- Score distribution charts
- Molecule images with annotations
- Failed molecules with error details
- Alert summary by catalog
- Processing metadata

---

<div align="center">

**Need more help?** Check the [API Reference](API_REFERENCE.md) or [Troubleshooting Guide](TROUBLESHOOTING.md)

</div>
