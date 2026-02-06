---
sidebar_position: 3
title: First Validation
description: Validate your first molecule using the web interface or API
---

# Your First Validation

Let's validate your first molecule using ChemAudit. We'll walk through validating Aspirin using both the web interface and the API.

## Using the Web Interface

### Step 1: Open ChemAudit

Navigate to the web interface:

- **Development**: http://localhost:3002
- **Production**: http://localhost

### Step 2: Enter a Molecule

On the Single Validation page, enter this SMILES string for Aspirin:

```
CC(=O)Oc1ccccc1C(=O)O
```

ChemAudit auto-detects the input format (SMILES, InChI, or MOL block), so you can paste any supported format.

### Step 3: Run Validation

Click the **Validate** button. Within seconds, you'll see comprehensive results across multiple tabs:

- **Validation**: Structural checks and overall score
- **Alerts**: Structural alert screening (PAINS, BRENK, etc.)
- **Scoring**: ML-readiness, drug-likeness, ADMET, and more
- **Standardization**: ChEMBL-compatible structure cleanup
- **Database Lookup**: Cross-references to PubChem, ChEMBL, COCONUT

### Step 4: Interpret Results

Look for these key indicators:

**Validation Score**: A number from 0-100 indicating overall structure quality.

| Score Range | Quality | Recommendation |
|-------------|---------|----------------|
| **90-100** | Excellent | Ready for use |
| **70-89** | Good | Minor issues, review recommended |
| **50-69** | Fair | Needs attention |
| **0-49** | Poor | Significant issues |

**Check Results**: Each validation check shows pass/fail status with severity:

| Severity | Meaning |
|----------|---------|
| **Critical** | Must be fixed - structure is invalid |
| **Warning** | Should be reviewed - may affect results |
| **Info** | Informational - no action required |

## Using the API

### Basic Validation Request

```bash
curl -X POST http://localhost:8001/api/v1/validate \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CC(=O)Oc1ccccc1C(=O)O",
    "format": "auto"
  }'
```

### Example Response

```json
{
  "status": "completed",
  "molecule_info": {
    "input_smiles": "CC(=O)Oc1ccccc1C(=O)O",
    "canonical_smiles": "CC(=O)OC1=CC=CC=C1C(O)=O",
    "inchi": "InChI=1S/C9H8O4/c1-6(10)13-8-5-3-2-4-7(8)9(11)12/h2-5H,1H3,(H,11,12)",
    "inchikey": "BSYNRYMUTXBXSQ-UHFFFAOYSA-N",
    "molecular_formula": "C9H8O4",
    "molecular_weight": 180.16,
    "num_atoms": 13
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

### Using Python

```python
import requests

response = requests.post(
    "http://localhost:8001/api/v1/validate",
    json={
        "molecule": "CC(=O)Oc1ccccc1C(=O)O",
        "format": "auto"
    }
)

result = response.json()
print(f"Score: {result['overall_score']}")
print(f"Issues: {len(result['issues'])}")
print(f"Formula: {result['molecule_info']['molecular_formula']}")
```

## Understanding Validation Checks

ChemAudit runs multiple validation checks grouped by category:

### Critical Checks

These must pass for the structure to be valid:

| Check | What It Validates |
|-------|------------------|
| **Valence** | All atoms have correct number of bonds |
| **Kekulization** | Aromatic rings can be kekulized |
| **Sanitization** | Molecule passes RDKit sanitization |

### Warning Checks

These indicate potential issues:

| Check | What It Detects |
|-------|----------------|
| **Undefined Stereo** | Stereocenters without R/S specification |
| **Stereo Consistency** | Conflicting stereochemistry specifications |

### Info Checks

Informational only, no action required:

| Check | What It Reports |
|-------|----------------|
| **SMILES Length** | Unusually long SMILES strings |
| **InChI Generation** | Whether InChI can be generated |

## Sample Molecules to Try

| Name | SMILES | Description |
|------|--------|-------------|
| Aspirin | `CC(=O)Oc1ccccc1C(=O)O` | Common pain reliever |
| Caffeine | `Cn1cnc2c1c(=O)n(c(=O)n2C)C` | Stimulant |
| Ibuprofen | `CC(C)Cc1ccc(cc1)C(C)C(=O)O` | Anti-inflammatory |
| Penicillin G | `CC1(C)SC2C(NC(=O)Cc3ccccc3)C(=O)N2C1C(=O)O` | Antibiotic |
| Morphine | `CN1CCC23C4C1CC5=C2C(=C(C=C5)O)OC3C(C=C4)O` | Analgesic |

:::tip Try Invalid Structures
To see how ChemAudit handles errors, try validating an invalid SMILES like `C1CCC1` (unclosed ring) or `CC(C)(C)(C)C` (too many substituents).
:::

## Next Steps

Now that you've completed your first validation, explore more features:

- **[Single Molecule Validation](/docs/user-guide/single-validation)** - Detailed guide to all validation features
- **[Batch Processing](/docs/user-guide/batch-processing)** - Process thousands of molecules at once
- **[Structural Alerts](/docs/user-guide/structural-alerts)** - Screen for problematic substructures
- **[Scoring](/docs/user-guide/scoring/overview)** - Comprehensive molecular scoring
- **[API Reference](/docs/api/overview)** - Full API documentation
