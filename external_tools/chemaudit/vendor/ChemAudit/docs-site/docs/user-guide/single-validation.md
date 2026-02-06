---
sidebar_position: 1
title: Single Validation
description: Validate individual molecules with comprehensive structural checks
---

# Single Molecule Validation

ChemAudit's single molecule validation provides comprehensive structural analysis for individual molecules. This feature is ideal for quick checks, exploring new compounds, or validating structures before batch processing.

## Supported Input Formats

ChemAudit automatically detects and supports multiple input formats:

| Format | Example | Auto-Detected |
|--------|---------|---------------|
| **SMILES** | `CCO` | Yes |
| **InChI** | `InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3` | Yes |
| **MOL Block** | V2000/V3000 format | Yes |

:::tip Auto-Detection
Simply paste your molecule in any format. ChemAudit will automatically detect whether it's a SMILES string, InChI, or MOL block.
:::

## How to Validate

### Using the Web Interface

1. Navigate to the **Single Validation** page (home)
2. Enter or paste your molecule in the input field
3. Click **Validate**
4. Review results across all tabs:
   - **Validation**: Structural checks and overall score
   - **Alerts**: PAINS, BRENK, NIH, ZINC, ChEMBL screening
   - **Scoring**: ML-readiness, drug-likeness, ADMET, NP-likeness
   - **Standardization**: ChEMBL-compatible cleanup
   - **Database Lookup**: PubChem, ChEMBL, COCONUT cross-references

### Using the API

```bash
curl -X POST http://localhost:8001/api/v1/validate \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CCO",
    "format": "auto"
  }'
```

## Validation Checks Explained

ChemAudit performs multiple validation checks grouped by severity and category.

### Critical Checks

These checks must pass for the structure to be considered valid:

| Check | Description | Common Causes of Failure |
|-------|-------------|-------------------------|
| **Valence** | All atoms have valid bond counts | Typos in SMILES, incorrect charges, too many bonds |
| **Kekulization** | Aromatic rings can be kekulized | Invalid aromatic systems, wrong electron count |
| **Sanitization** | Molecule passes RDKit sanitization | Structural inconsistencies, invalid atom types |

:::danger Critical Failures
If a critical check fails, the molecule structure is invalid and cannot be used for further analysis. Fix the structure before proceeding.
:::

### Warning Checks

These checks indicate potential issues that should be reviewed:

| Check | Description | Recommendation |
|-------|-------------|----------------|
| **Undefined Stereo** | Stereocenters without R/S specification detected | Specify stereochemistry if intended, or note as racemic |
| **Stereo Consistency** | Stereochemistry specifications are consistent | Verify intended stereochemistry, remove contradictory specs |

:::warning Review Warnings
Warning checks don't invalidate the structure, but they may affect downstream analysis or experimental results.
:::

### Info Checks

Informational checks that don't require action:

| Check | Description |
|-------|-------------|
| **SMILES Length** | Reports if SMILES is unusually long (>200 characters) |
| **InChI Generation** | Confirms InChI can be generated from the structure |

## Validation Options

### Preserve Aromatic Notation

By default, ChemAudit outputs canonical SMILES in kekulized form (explicit single/double bonds). Enable this option to preserve aromatic notation:

**Input:**
```
c1ccccc1
```

**Output (default - kekulized):**
```
C1=CC=CC=C1
```

**Output (preserve_aromatic=true):**
```
c1ccccc1
```

## Understanding Results

### Overall Score

The overall validation score ranges from 0-100 and indicates structure quality:

| Score Range | Quality | Interpretation |
|-------------|---------|---------------|
| **90-100** | Excellent | Structure is valid and ready for use |
| **70-89** | Good | Minor issues detected, review recommended |
| **50-69** | Fair | Significant issues need attention |
| **0-49** | Poor | Critical problems, structure likely invalid |

### Molecule Information

Every validated molecule returns comprehensive information:

- **Input SMILES**: Original input
- **Canonical SMILES**: Standardized SMILES representation
- **InChI**: International Chemical Identifier
- **InChIKey**: Hashed InChI for database lookups
- **Molecular Formula**: Element composition
- **Molecular Weight**: Exact mass
- **Atom Count**: Number of heavy atoms

### Issue Details

Failed checks appear in the issues list with:

- **Check name**: Which validation failed
- **Severity**: Critical, Warning, or Info
- **Message**: Human-readable description
- **Affected atoms**: Atom indices involved (if applicable)
- **Details**: Additional technical information

## Common Validation Errors

### Valence Errors

**Problem**: Atom has incorrect number of bonds

**Examples:**

| Invalid | Valid | Explanation |
|---------|-------|-------------|
| `CC(C)(C)(C)C` | `CC(C)(C)C` | Carbon can have max 4 bonds |
| `CN` (with 4 bonds to N) | `C[N+]` | Quaternary nitrogen needs charge |

### Kekulization Failures

**Problem**: Aromatic ring doesn't follow Hückel's rule (4n+2 electrons)

**Examples:**

| Invalid | Valid | Explanation |
|---------|-------|-------------|
| `c1cccc1` | `c1ccccc1` | Benzene needs 6 carbons (6 pi electrons) |
| `c1cccccccc1` | `C1=CC=CC=CC=CC=C1` | 8 pi electrons - not aromatic |

### Unclosed Rings

**Problem**: Ring opening doesn't have matching closing

**Examples:**

| Invalid | Valid | Explanation |
|---------|-------|-------------|
| `C1CCC` | `C1CCCC1` | Ring 1 not closed |
| `C1CCC2` | `C1CCC2CC2C1` | Rings 1 and 2 must both close |

## Best Practices

1. **Validate early**: Check structures before starting experiments
2. **Review warnings**: Don't ignore stereo or representation warnings
3. **Use canonical forms**: Work with canonical SMILES for consistency
4. **Cross-check databases**: Use database lookup to verify compound identity
5. **Document issues**: Record any validation warnings in your data

## Next Steps

- **[Batch Processing](/docs/user-guide/batch-processing)** - Validate thousands of molecules at once
- **[Structural Alerts](/docs/user-guide/structural-alerts)** - Screen for problematic patterns
- **[Scoring](/docs/user-guide/scoring/overview)** - Comprehensive molecular scoring
- **[API Reference](/docs/api/endpoints)** - Detailed API documentation
