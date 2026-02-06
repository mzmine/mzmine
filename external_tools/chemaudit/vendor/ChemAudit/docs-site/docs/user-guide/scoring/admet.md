---
sidebar_position: 5
title: ADMET
description: Predict Absorption, Distribution, Metabolism, Excretion, and Toxicity properties
---

# ADMET Predictions

ADMET scoring predicts key pharmacokinetic and physicochemical properties: Absorption, Distribution, Metabolism, Excretion, and Toxicity.

## Available Predictions

| Property | Method | Output | Interpretation |
|----------|--------|--------|----------------|
| **Synthetic Accessibility** | SAscore | 1-10 (1=easy, 10=hard) | Ease of synthesis |
| **Aqueous Solubility** | ESOL (Delaney) | LogS, mg/mL, classification | Water solubility |
| **Complexity** | Fsp3, stereocenters, rings, Bertz CT | Various metrics | Molecular complexity |
| **CNS MPO** | Multi-parameter optimization | Score 0-6 | CNS penetration likelihood |
| **Bioavailability** | TPSA, rotatable bonds, Lipinski | Predictions | Oral absorption, CNS flags |
| **Pfizer 3/75 Rule** | LogP < 3, TPSA > 75 | Pass/fail | Toxicity risk reduction |
| **GSK 4/400 Rule** | MW ≤ 400, LogP ≤ 4 | Pass/fail | Lead-like properties |
| **Golden Triangle** | MW vs LogD plot | In/out | Optimal drug-like space |

## Synthetic Accessibility (SAscore)

Estimates how difficult a molecule is to synthesize:

| Score | Classification | Interpretation |
|-------|---------------|----------------|
| **1-3** | Easy | Simple synthesis, few steps |
| **4-6** | Moderate | Standard synthetic methods |
| **7-9** | Difficult | Complex, many steps |
| **10** | Very Difficult | Extremely challenging |

Based on fragment contributions and complexity penalties.

## Aqueous Solubility (ESOL)

Predicts water solubility using the Delaney ESOL model:

| LogS | mg/mL | Classification |
|------|-------|---------------|
| **> -2** | > 0.01 | Highly soluble |
| **-2 to -4** | 0.0001-0.01 | Soluble |
| **-4 to -6** | 0.000001-0.0001 | Moderately soluble |
| **< -6** | < 0.000001 | Poorly soluble |

Good aqueous solubility (LogS > -4) is favorable for oral drugs.

## Molecular Complexity

Multiple complexity metrics:

**Fsp3** (Fraction sp3 carbons):
- Higher Fsp3 = more saturated, less flat
- Target: > 0.42 for drug-likeness

**Stereocenters:**
- Count of chiral centers
- More stereocenters = higher synthetic complexity

**Ring systems:**
- Total rings and aromatic rings
- Complex scaffolds have many rings

**Bertz CT** (Complexity index):
- Higher values = more complex
- Accounts for branching and symmetry

## CNS MPO (Multiparameter Optimization)

Predicts CNS penetration using 6 properties:

- LogP (optimal: 2-3)
- LogD at pH 7.4
- TPSA (optimal: < 90 Å²)
- HBD (optimal: ≤ 2)
- pKa
- MW (optimal: < 360)

| CNS MPO Score | Interpretation |
|---------------|---------------|
| **5-6** | Highly likely CNS penetrant |
| **3-4** | Possible CNS penetrant |
| **0-2** | Unlikely CNS penetrant |

## Bioavailability Predictions

**Oral Absorption:**
- Based on TPSA (≤ 140 Å²) and Lipinski compliance
- `true` if likely orally bioavailable

**CNS Penetration:**
- Based on TPSA (< 90 Å²) and molecular weight
- `true` if blood-brain barrier penetration likely

## Pfizer 3/75 Rule

Reduces toxicity risk:

- LogP < 3
- TPSA > 75 Å²

Developed from analysis of toxic compounds. Passing reduces risk of non-specific binding and toxicity.

## GSK 4/400 Rule

Lead-like properties:

- MW ≤ 400 Da
- LogP ≤ 4

Provides room for optimization while maintaining drug-likeness.

## Golden Triangle

Plots MW vs LogD to define optimal space:

- MW: 200-500 Da
- LogD: -2 to 5

Molecules in the "golden triangle" have favorable balance of potency and ADMET properties.

## API Usage

```bash
curl -X POST http://localhost:8001/api/v1/score \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CC(=O)Oc1ccccc1C(=O)O",
    "include": ["admet"]
  }'
```

Response:
```json
{
  "admet": {
    "synthetic_accessibility": {
      "score": 1.5,
      "classification": "Easy",
      "interpretation": "Very easy to synthesize"
    },
    "solubility": {
      "log_s": -2.1,
      "solubility_mg_ml": 1.43,
      "classification": "Soluble",
      "interpretation": "Good aqueous solubility"
    },
    "complexity": {
      "fsp3": 0.11,
      "num_stereocenters": 0,
      "num_rings": 1,
      "num_aromatic_rings": 1,
      "bertz_ct": 245.2,
      "classification": "Low complexity"
    },
    "cns_mpo": {
      "score": 3.8,
      "cns_penetrant": false
    },
    "bioavailability": {
      "oral_absorption_likely": true,
      "cns_penetration_likely": false
    },
    "pfizer_rule": {
      "passed": true,
      "logp": 1.19,
      "tpsa": 63.6
    },
    "gsk_rule": {
      "passed": true,
      "mw": 180.16,
      "logp": 1.19
    },
    "golden_triangle": {
      "in_golden_triangle": true,
      "mw": 180.16,
      "logd": 1.19
    }
  }
}
```

## Interpretation Guidelines

**Good ADMET Profile:**
- SAscore < 5 (easy to synthesize)
- LogS > -4 (good solubility)
- Fsp3 > 0.4 (sufficient saturation)
- Oral absorption likely
- Passes Pfizer and GSK rules
- In golden triangle

**Poor ADMET Profile:**
- SAscore > 7 (hard to synthesize)
- LogS < -6 (poorly soluble)
- Very complex (many stereocenters, rings)
- CNS penetration when not desired
- Fails multiple rules

## Limitations

All predictions are computational estimates:

- Based on training data (mostly drug-like molecules)
- May not generalize to unusual scaffolds
- Don't replace experimental measurements
- Use for prioritization, not absolute decisions

:::warning Experimental Validation Required
ADMET predictions guide early decisions but must be validated experimentally for lead candidates.
:::

## Use Cases

### Lead Optimization

Track ADMET during optimization:

- Monitor solubility changes
- Avoid increasing synthetic complexity
- Maintain favorable bioavailability
- Stay in golden triangle

### Compound Prioritization

Rank compounds by ADMET profile:

```
SAscore < 5 AND
LogS > -4 AND
oral_absorption_likely = true AND
pfizer_rule_passed = true
```

### Library Design

Design screening libraries with good ADMET:

- Target SAscore < 5
- Ensure solubility > -4
- Maintain Fsp3 > 0.4
- Stay in golden triangle

## Next Steps

- **[Drug-likeness](/docs/user-guide/scoring/drug-likeness)** - Lipinski, QED, Veber rules
- **[Scoring Overview](/docs/user-guide/scoring/overview)** - All scoring systems
- **[API Reference](/docs/api/endpoints)** - Full scoring API
