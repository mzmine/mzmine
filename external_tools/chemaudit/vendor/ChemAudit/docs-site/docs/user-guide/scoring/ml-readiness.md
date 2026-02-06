---
sidebar_position: 2
title: ML Readiness
description: Evaluate molecular suitability for machine learning applications
---

# ML-Readiness Scoring

The ML-readiness score evaluates how suitable a molecule is for machine learning applications by testing descriptor calculability, fingerprint generation, and size constraints.

## Score Components

The ML-readiness score is calculated from three weighted components totaling 100 points:

| Component | Max Points | What It Measures |
|-----------|------------|------------------|
| **Descriptors** | 30 | Can standard RDKit descriptors be computed? |
| **Fingerprints** | 40 | Can molecular fingerprints be generated? |
| **Size/Complexity** | 30 | Is size within ML-trainable range? |

## Descriptor Score (30 points)

Tests whether 200+ RDKit descriptors can be calculated successfully:

- **30 points**: All descriptors calculate successfully
- **15-29 points**: Some descriptors fail (proportional to success rate)
- **0 points**: Most or all descriptors fail

**Common descriptor failures:**
- Very large molecules (>100 atoms)
- Molecules with unusual elements
- Invalid structures
- Extremely complex ring systems

## Fingerprint Score (40 points)

Tests whether these fingerprint types can be generated:

| Fingerprint Type | Points | Description |
|-----------------|--------|-------------|
| **Morgan** | 10 | Circular fingerprints (ECFP-like) |
| **RDKit** | 10 | Daylight-like fingerprints |
| **MACCS** | 10 | 166-bit MACCS keys |
| **Topological** | 10 | Topological torsion fingerprints |

- Each successful fingerprint adds 10 points
- **40 points**: All 4 fingerprint types work
- **0 points**: No fingerprints can be generated

**Common fingerprint failures:**
- Invalid aromatic systems
- Disconnected fragments
- Very large molecules
- Molecules with metals or unusual bonding

## Size/Complexity Score (30 points)

Evaluates whether molecular size is within typical ML training range:

| Category | MW Range | Atom Range | Points | Interpretation |
|----------|----------|------------|--------|----------------|
| **Optimal** | 200-500 | 15-40 | 30 | Ideal for most ML models |
| **Good** | 100-200 or 500-700 | 8-15 or 40-60 | 20 | Acceptable, may need adjustment |
| **Fair** | 50-100 or 700-900 | 4-8 or 60-80 | 10 | Edge of typical training data |
| **Poor** | Less than 50 or greater than 900 | Less than 4 or greater than 80 | 0 | Outside typical ML range |

## Overall Score Interpretation

| Score | Category | Interpretation | Recommendation |
|-------|----------|---------------|----------------|
| **80-100** | Excellent | Ideal ML candidate | Use directly in training/prediction |
| **60-79** | Good | Minor caveats | Review failed descriptors, likely usable |
| **40-59** | Fair | Significant issues | May need preprocessing or exclusion |
| **0-39** | Poor | Major problems | Likely unsuitable for standard ML |

## API Usage

```bash
curl -X POST http://localhost:8001/api/v1/score \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CCO",
    "include": ["ml_readiness"]
  }'
```

Response:
```json
{
  "ml_readiness": {
    "score": 85,
    "breakdown": {
      "descriptors_score": 30,
      "descriptors_successful": 200,
      "descriptors_total": 200,
      "fingerprints_score": 40,
      "fingerprints_successful": ["morgan", "rdkit", "maccs", "topological"],
      "fingerprints_failed": [],
      "size_score": 15,
      "molecular_weight": 46.07,
      "num_atoms": 3,
      "size_category": "small"
    },
    "interpretation": "Good ML candidate",
    "failed_descriptors": []
  }
}
```

## Use Cases

### Dataset Curation for ML

Filter molecules before creating training datasets:

```
ml_readiness_score >= 80 AND
validation_score >= 90
```

This ensures:
- All required descriptors calculate
- All fingerprints generate successfully
- Size is within trainable range
- Structure is valid

### Model Applicability Domain

Use ML-readiness to define applicability domain:

- Train models only on molecules with score >= 80
- Flag predictions on molecules with score < 80 as uncertain
- Exclude molecules with score < 60 from predictions

### Descriptor Selection

The `failed_descriptors` list identifies which descriptors don't work:

```json
{
  "failed_descriptors": ["BCUT2D_MWHI", "BCUT2D_MWLOW"]
}
```

Use this to:
- Exclude problematic descriptors from feature sets
- Identify molecules requiring special handling
- Debug descriptor calculation issues

## Limitations

**Does not test:**
- Descriptor quality or relevance
- Model-specific requirements
- Chemical space coverage
- Experimental measurability

**Assumes:**
- Standard RDKit descriptors are sufficient
- Common fingerprint types are appropriate
- Size range is from typical drug-like datasets

:::tip Custom Requirements
ML-readiness tests standard descriptors and fingerprints. If your model uses custom features, you'll need additional validation.
:::

## Best Practices

1. **Set minimum thresholds**: Require score >= 80 for training data
2. **Review failures**: Investigate low-scoring molecules to understand why
3. **Track over time**: Monitor ML-readiness as you curate datasets
4. **Document exclusions**: Record why molecules were excluded based on ML-readiness
5. **Test predictions**: Always validate predictions on low-scoring molecules

## Next Steps

- **[Scoring Overview](/docs/user-guide/scoring/overview)** - All scoring systems
- **[Batch Processing](/docs/user-guide/batch-processing)** - Score large datasets
- **[API Reference](/docs/api/endpoints)** - Full scoring API
