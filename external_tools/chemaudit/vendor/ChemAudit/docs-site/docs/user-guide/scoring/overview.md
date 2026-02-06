---
sidebar_position: 1
title: Scoring Overview
description: Comprehensive molecular scoring across ML-readiness, drug-likeness, ADMET, and more
---

# Scoring Overview

ChemAudit provides comprehensive molecular scoring across multiple dimensions to help you assess compound quality, drug-likeness, and suitability for various applications.

## Available Scoring Systems

| Score Type | What It Measures | Range | Use Case |
|------------|------------------|-------|----------|
| **[ML-Readiness](/docs/user-guide/scoring/ml-readiness)** | Suitability for machine learning | 0-100 | Dataset curation, model training |
| **[Drug-likeness](/docs/user-guide/scoring/drug-likeness)** | Compliance with drug-like rules | Multiple filters | Drug discovery, lead identification |
| **[Safety Filters](/docs/user-guide/scoring/safety-filters)** | Structural alert screening | Pass/Fail | Compound library filtering |
| **[ADMET](/docs/user-guide/scoring/admet)** | Absorption, Distribution, Metabolism, Excretion, Toxicity | Various | Lead optimization, candidate selection |
| **[NP-Likeness](/docs/user-guide/scoring/np-likeness)** | Natural product similarity | -5 to +5 | Natural product research, diversity analysis |
| **[Scaffold Analysis](/docs/user-guide/scoring/scaffold-analysis)** | Murcko scaffold extraction | N/A | SAR analysis, scaffold hopping |
| **[Aggregator Likelihood](/docs/user-guide/scoring/aggregator-likelihood)** | Colloidal aggregation risk | Low/Medium/High | Assay design, hit validation |

## How to Use Scoring

### Web Interface

Scoring results appear automatically on the **Scoring** tab after validation:

1. Enter and validate a molecule
2. Navigate to the **Scoring** tab
3. Review all scores with interpretations
4. Click score details for more information

### API - All Scores

Request all available scores:

```bash
curl -X POST http://localhost:8001/api/v1/score \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CCO",
    "format": "smiles"
  }'
```

### API - Specific Scores

Request only specific score types:

```bash
curl -X POST http://localhost:8001/api/v1/score \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CCO",
    "include": ["ml_readiness", "druglikeness", "admet"]
  }'
```

## Interpreting Scores Together

Different scores provide complementary information. Here's how to interpret them together:

### High-Quality Drug Discovery Candidate

- **ML-Readiness**: 80-100 (descriptors and fingerprints work)
- **Drug-likeness**: Passes Lipinski and Veber, QED > 0.5
- **Safety**: No PAINS, BRENK, or NIH alerts
- **ADMET**: Good solubility, low SA score, passes Pfizer/GSK rules
- **Aggregator**: Low risk

### Machine Learning Dataset Molecule

- **ML-Readiness**: 80-100 (high priority)
- **Validation**: 90-100 (structure quality)
- **Drug-likeness**: Not critical, but useful for diversity
- **ADMET**: Not critical for training data

### Natural Product

- **NP-Likeness**: > 1.0 (natural product-like)
- **Drug-likeness**: May fail Lipinski (NPs often larger)
- **Scaffold**: Complex ring systems common
- **ADMET**: SAscore often high (hard to synthesize)

### Screening Hit for Validation

- **Safety**: Check PAINS (hit could be artifact)
- **Aggregator**: Check aggregation risk
- **Drug-likeness**: QED and Lipinski for early assessment
- **ADMET**: Initial ADMET profile

## Score Caveats

### Context Matters

Scores are guidelines, not absolutes:

- **Lipinski failures**: Many drugs violate Lipinski (antibiotics, natural products)
- **PAINS alerts**: 87 FDA-approved drugs match PAINS patterns
- **High SAscore**: Doesn't mean impossible to synthesize
- **Low QED**: Doesn't mean it won't be a good drug

### Molecule Size Effects

Very small or very large molecules may have unexpected scores:

- **Small molecules** (< 10 heavy atoms): May score poorly on drug-likeness
- **Large molecules** (> 50 heavy atoms): May fail size-based filters
- **Macrocycles**: Often fail standard drug-likeness rules

### Prediction Limitations

ADMET and other predictions are estimates:

- Based on computational models, not experimental data
- May not account for rare functional groups
- Best used for prioritization, not as absolute truth
- Validate experimentally for critical decisions

## Score Combinations for Filtering

### Strict Drug-like Filter

```
validation_score >= 90 AND
lipinski_passed = true AND
veber_passed = true AND
qed >= 0.5 AND
pains_passed = true AND
aggregator_likelihood = "Low"
```

### ML Training Data Filter

```
ml_readiness_score >= 80 AND
validation_score >= 90 AND
(No critical validation errors)
```

### Natural Product Filter

```
np_likeness_score > 1.0 AND
validation_score >= 70 AND
has_scaffold = true
```

## Batch Processing with Scoring

In batch mode, scoring statistics are aggregated:

- **Average scores**: Mean across all molecules
- **Pass rates**: Percentage passing each filter
- **Distribution**: Score histograms
- **Outliers**: Molecules with unusual score patterns

Use these statistics to:
- Assess overall library quality
- Identify problematic subsets
- Guide curation efforts
- Track improvements over time

## Next Steps

Explore individual scoring systems in detail:

- **[ML-Readiness](/docs/user-guide/scoring/ml-readiness)** - Descriptor and fingerprint calculability
- **[Drug-likeness](/docs/user-guide/scoring/drug-likeness)** - Lipinski, QED, Veber, and more
- **[Safety Filters](/docs/user-guide/scoring/safety-filters)** - PAINS, BRENK, NIH, ZINC, ChEMBL
- **[ADMET](/docs/user-guide/scoring/admet)** - Synthetic accessibility, solubility, bioavailability
- **[NP-Likeness](/docs/user-guide/scoring/np-likeness)** - Natural product classification
- **[Scaffold Analysis](/docs/user-guide/scoring/scaffold-analysis)** - Murcko scaffold extraction
- **[Aggregator Likelihood](/docs/user-guide/scoring/aggregator-likelihood)** - Colloidal aggregation risk
