---
sidebar_position: 6
title: NP Likeness
description: Evaluate natural product likeness vs. synthetic-like character
---

# NP-Likeness Scoring

NP-likeness (Natural Product likeness) scoring evaluates whether a molecule resembles known natural products or synthetic compounds.

## What It Measures

The NP-likeness score is based on structural fragments and their frequency in:
- Natural product databases (COCONUT, Dictionary of Natural Products)
- Synthetic molecule databases (PubChem, ZINC)

Higher scores indicate natural product-like character, lower scores indicate synthetic-like character.

## Score Range and Interpretation

| Score Range | Classification | Typical Examples |
|-------------|---------------|------------------|
| **> 1.0** | Natural product-like | Alkaloids, terpenes, polyketides |
| **-1.0 to 1.0** | Intermediate | Simple aromatic compounds, modified NPs |
| **< -1.0** | Synthetic-like | Drug-like molecules, screening compounds |

## Typical Scores

**Natural products:**
- Morphine: +2.5
- Taxol: +3.8
- Quinine: +2.1

**Synthetic drugs:**
- Ibuprofen: -1.8
- Aspirin: -0.9
- Lipitor: -2.3

## API Usage

```bash
curl -X POST http://localhost:8001/api/v1/score \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CN1CCC23C4C1CC5=C2C(=C(C=C5)O)OC3C(C=C4)O",
    "include": ["np_likeness"]
  }'
```

Response:
```json
{
  "np_likeness": {
    "score": 2.1,
    "interpretation": "Natural product-like molecule",
    "caveats": []
  }
}
```

## Caveats and Limitations

**Molecule size effects:**

For very large molecules (>50 heavy atoms), scores may be less reliable. The model is trained on typical drug-sized molecules.

**Fragment-based:**

The score is based on structural fragments. Molecules with unusual fragment combinations may have unexpected scores.

**Training data bias:**

Scores reflect the training data composition. Rare natural product classes may score as synthetic if underrepresented in training data.

:::info Caveat Reporting
When limitations apply, they're reported in the `caveats` array for transparency.
:::

## Use Cases

### Natural Product Research

Identify natural product-like molecules in screening libraries:

```
np_likeness_score > 1.0
```

### Diversity Analysis

Assess chemical diversity by NP-likeness distribution:

- Wide range (-3 to +3): High diversity
- Narrow range: Homogeneous library
- Skewed distribution: Biased toward NP or synthetic space

### Scaffold Analysis

Combine with scaffold analysis to identify natural product-inspired scaffolds:

```
np_likeness_score > 1.0 AND
has_scaffold = true
```

### Library Design

Balance natural product and synthetic character:

- NP-focused library: Score > 0
- Synthetic-focused library: Score < 0
- Balanced library: Score -1 to +1

## Interpretation Guidelines

**High positive score (> 2.0):**
- Likely contains natural product fragments
- May have complex ring systems
- Often higher molecular weight
- Potentially harder to synthesize

**Near zero (-1.0 to +1.0):**
- Intermediate character
- May be NP-inspired synthetic molecules
- Simple aromatic compounds
- Modified natural products

**High negative score (< -2.0):**
- Synthetic-like fragments dominant
- Simpler structure
- Drug-like properties common
- Easier to synthesize

## Relationship to Other Scores

**NP-likeness vs Drug-likeness:**
- Natural products often violate Lipinski
- Higher complexity (more rings, stereocenters)
- Often larger molecular weight

**NP-likeness vs Synthetic Accessibility:**
- Positive correlation: NP-like → harder to synthesize
- Not absolute: some NPs are easy to synthesize

**NP-likeness vs Scaffold Complexity:**
- NP-like molecules often have complex scaffolds
- Multiple fused ring systems
- More stereocenters

## Best Practices

1. **Use as a guide**: Not a definitive natural product classifier
2. **Combine with other data**: Check COCONUT database for confirmation
3. **Consider therapeutic area**: Some drug classes are NP-like (antibiotics)
4. **Track during optimization**: Monitor changes in NP character
5. **Balance with drug-likeness**: High NP-likeness may sacrifice drug-likeness

## Next Steps

- **[Database Integrations](/docs/user-guide/database-integrations)** - Search COCONUT database
- **[Scaffold Analysis](/docs/user-guide/scoring/scaffold-analysis)** - Analyze ring systems
- **[Scoring Overview](/docs/user-guide/scoring/overview)** - All scoring systems
