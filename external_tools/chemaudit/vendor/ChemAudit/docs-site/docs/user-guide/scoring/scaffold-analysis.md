---
sidebar_position: 7
title: Scaffold Analysis
description: Extract Murcko scaffolds for structure-activity relationship analysis
---

# Scaffold Analysis

Scaffold analysis extracts the core ring system from molecules using the Murcko framework method, enabling structure-activity relationship (SAR) analysis and scaffold hopping.

## What Is a Murcko Scaffold?

A Murcko scaffold is the core ring system and linkers of a molecule with all side chains removed.

**Example (Ibuprofen):**
- **Full structure**: `CC(C)Cc1ccc(cc1)C(C)C(=O)O`
- **Murcko scaffold**: `c1ccc(cc1)C`
- **Generic scaffold**: `C1CCC(CC1)C`

## Types of Scaffolds

### Murcko Scaffold

Preserves atom types in the ring system and linkers:

- Aromatic vs. aliphatic rings distinguished
- Heteroatoms preserved
- Useful for detailed SAR analysis

### Generic Scaffold

Replaces all atoms with carbon and all bonds with single bonds:

- Focuses on topology only
- Groups molecules by ring framework
- Useful for broad scaffold classification

## Output

Scaffold analysis returns:

| Field | Description |
|-------|-------------|
| `scaffold_smiles` | SMILES of Murcko scaffold |
| `generic_scaffold_smiles` | SMILES of generic scaffold |
| `has_scaffold` | Whether molecule contains ring systems |
| `message` | Explanation if no scaffold found |

## API Usage

```bash
curl -X POST http://localhost:8001/api/v1/score \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CC(C)Cc1ccc(cc1)C(C)C(=O)O",
    "include": ["scaffold"]
  }'
```

Response (with scaffold):
```json
{
  "scaffold": {
    "scaffold_smiles": "c1ccc(cc1)C",
    "generic_scaffold_smiles": "C1CCC(CC1)C",
    "has_scaffold": true,
    "message": "Scaffold extracted successfully"
  }
}
```

Response (no scaffold):
```json
{
  "scaffold": {
    "scaffold_smiles": null,
    "generic_scaffold_smiles": null,
    "has_scaffold": false,
    "message": "No ring system found"
  }
}
```

## Use Cases

### SAR Analysis

Group molecules by scaffold to analyze structure-activity relationships:

1. Extract scaffolds from active compounds
2. Group by scaffold SMILES
3. Analyze R-group effects on activity
4. Identify optimal substitution patterns

### Scaffold Hopping

Find alternative scaffolds with similar properties:

1. Extract scaffold from lead compound
2. Search for molecules with different scaffolds
3. Test for similar biological activity
4. Expand chemical space explored

### Library Diversity

Assess scaffold diversity in compound libraries:

1. Extract generic scaffolds from all molecules
2. Count unique scaffolds
3. Calculate scaffold diversity metrics
4. Identify over-represented scaffolds

### Hit Clustering

Cluster screening hits by scaffold:

1. Extract scaffolds from all hits
2. Group by identical scaffolds
3. Prioritize diverse scaffolds
4. Avoid redundant follow-up

## Molecules Without Scaffolds

Molecules without ring systems return `has_scaffold: false`:

**Examples:**
- Linear molecules (ethanol, hexane)
- Branched aliphatics
- Simple derivatives without rings

These molecules have no Murcko scaffold by definition.

## Scaffold Statistics in Batch Processing

In batch mode, scaffold diversity is reported:

```json
{
  "scaffold_statistics": {
    "total_molecules": 1000,
    "molecules_with_scaffolds": 850,
    "unique_scaffolds": 45,
    "unique_generic_scaffolds": 38,
    "scaffold_diversity": 0.053
  }
}
```

**Scaffold diversity** = unique scaffolds / total molecules

- Higher diversity = more varied scaffolds
- Lower diversity = repeated scaffold use

## Combining with Other Scores

**Scaffold + NP-likeness:**

Natural products often have complex scaffolds:

```
has_scaffold = true AND
np_likeness_score > 1.0
```

**Scaffold + Drug-likeness:**

Drug-like scaffolds typically:

```
has_scaffold = true AND
lipinski_passed = true AND
num_rings <= 5
```

## Best Practices

1. **Use for grouping**: Scaffold is ideal for clustering molecules
2. **Generic for broad classes**: Use generic scaffold for high-level classification
3. **Murcko for SAR**: Use Murcko scaffold for detailed SAR
4. **Track diversity**: Monitor scaffold diversity in libraries
5. **Combine with clustering**: Use with fingerprint clustering for comprehensive analysis

## Limitations

**Scaffold definition:**
- Only captures ring systems
- Doesn't account for stereochemistry
- Linker atoms may vary between definitions

**Edge cases:**
- Spiro compounds: Complex scaffold extraction
- Macrocycles: Entire molecule may be the scaffold
- Fused systems: Multiple valid scaffold interpretations

:::tip Complex Ring Systems
For molecules with complex or unusual ring systems (spiro, bridged, cage), manually review scaffold extraction results.
:::

## Next Steps

- **[NP-Likeness](/docs/user-guide/scoring/np-likeness)** - Natural product similarity
- **[Batch Processing](/docs/user-guide/batch-processing)** - Analyze scaffold diversity
- **[Scoring Overview](/docs/user-guide/scoring/overview)** - All scoring systems
