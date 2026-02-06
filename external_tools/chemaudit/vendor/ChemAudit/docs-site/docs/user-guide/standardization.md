---
sidebar_position: 5
title: Standardization
description: Standardize chemical structures using the ChEMBL-compatible pipeline
---

# Standardization

ChemAudit's standardization pipeline cleans and normalizes chemical structures using a workflow compatible with ChEMBL's curation process. This ensures consistent structure representation across your datasets.

## Pipeline Steps

The standardization pipeline consists of four steps, three of which always run:

| Step | Description | Always Runs |
|------|-------------|-------------|
| **Checker** | Detect structural issues before standardization | Yes |
| **Standardizer** | Fix common issues (nitro groups, metals, sulphoxides) | Yes |
| **Get Parent** | Extract parent molecule, remove salts and solvents | Yes |
| **Tautomer** | Canonicalize tautomers | No (opt-in) |

### Checker

Identifies potential problems in the input structure:

- Invalid valences
- Unusual bond types
- Suspicious stereo markings
- Radical electrons

Issues are reported but don't stop standardization.

### Standardizer

Applies ChEMBL-compatible fixes:

- Normalizes nitro groups
- Standardizes aromatic systems
- Fixes metal disconnections
- Normalizes sulfoxides and related groups
- Fixes rare bond types

### Get Parent

Removes salts, solvents, and counterions:

- Identifies and removes common salts (NaCl, HCl, etc.)
- Removes solvents (water, DMSO, etc.)
- Extracts the largest fragment as the parent molecule
- Tracks what was removed

### Tautomer Canonicalization (Optional)

Converts tautomers to canonical form:

- Uses ChEMBL's tautomer rules
- Ensures consistent representation
- **Warning**: May lose E/Z double bond stereochemistry

:::warning Stereo Loss
Tautomer canonicalization can change double bond stereochemistry. Only enable if tautomer consistency is more important than preserving E/Z stereo.
:::

## How to Standardize

### Web Interface

1. Enter your molecule on the Single Validation page
2. Navigate to the **Standardization** tab
3. View before/after comparison showing:
   - Original and standardized SMILES
   - Steps applied with changes
   - Removed fragments (salts/solvents)
   - Stereo comparison (if changes occurred)
   - Structure comparison (atom count, formula, mass)

### API

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

Response:
```json
{
  "result": {
    "original_smiles": "CC(=O)Oc1ccccc1C(=O)[O-].[Na+]",
    "standardized_smiles": "CC(=O)OC1=CC=CC=C1C(O)=O",
    "success": true,
    "steps_applied": [
      {
        "step_name": "standardizer",
        "applied": true,
        "description": "Applied standardization rules",
        "changes": []
      },
      {
        "step_name": "get_parent",
        "applied": true,
        "description": "Extracted parent molecule",
        "changes": ["Removed fragment: [Na+]"]
      }
    ],
    "excluded_fragments": ["[Na+]"],
    "structure_comparison": {
      "original_atom_count": 14,
      "standardized_atom_count": 13,
      "original_formula": "C9H7NaO4",
      "standardized_formula": "C9H8O4",
      "mass_change_percent": -10.87,
      "diff_summary": "Salt removed"
    }
  }
}
```

## Standardization Options

### include_tautomer

**Type**: Boolean
**Default**: `false`
**Description**: Enable tautomer canonicalization

When enabled, converts tautomers to canonical form using ChEMBL's rules.

:::danger Stereo Warning
May lose E/Z double bond stereochemistry. Only enable if tautomer consistency is critical.
:::

### preserve_stereo

**Type**: Boolean
**Default**: `true`
**Description**: Attempt to preserve stereochemistry during standardization

When enabled, the standardizer tries to maintain R/S and E/Z stereo specifications. However, some transformations may still affect stereochemistry.

## Understanding Results

### Steps Applied

Each step reports whether it was applied and what changes occurred:

```json
{
  "step_name": "get_parent",
  "applied": true,
  "description": "Extracted parent molecule",
  "changes": ["Removed fragment: [Na+]", "Removed fragment: Cl"]
}
```

If a step makes no changes, it still reports `applied: true` but with an empty `changes` array.

### Checker Issues

Problems detected by the checker are reported separately:

```json
{
  "checker_issues": [
    {
      "issue": "unusual_valence",
      "description": "Atom 5 has unusual valence",
      "severity": "warning"
    }
  ]
}
```

### Excluded Fragments

Lists all salts, solvents, and counterions removed:

```json
{
  "excluded_fragments": ["[Na+]", "Cl", "O"]
}
```

Common excluded fragments:
- Salts: Na+, K+, Cl-, Br-, I-
- Solvents: H2O, DMSO, MeOH, EtOH
- Counterions: TFA-, acetate, formate

### Stereo Comparison

If stereochemistry changes, a detailed comparison is provided:

```json
{
  "stereo_comparison": {
    "original_chiral_centers": 2,
    "standardized_chiral_centers": 2,
    "stereo_preserved": true,
    "changes": []
  }
}
```

### Structure Comparison

Shows structural differences between original and standardized:

```json
{
  "structure_comparison": {
    "original_atom_count": 25,
    "standardized_atom_count": 20,
    "original_formula": "C15H20N2O3.HCl",
    "standardized_formula": "C15H20N2O3",
    "original_mw": 312.78,
    "standardized_mw": 276.33,
    "mass_change_percent": -11.65,
    "is_identical": false,
    "diff_summary": "Salt removed"
  }
}
```

## Use Cases

### Database Curation

Standardize structures before adding to databases:

```bash
# Batch standardize an entire library
curl -X POST http://localhost:8001/api/v1/batch/upload \
  -F "file=@library.sdf" \
  -F "include_standardization=true"
```

### Structure Deduplication

Standardization helps identify duplicates:

1. Standardize all structures
2. Compare canonical SMILES
3. Group by InChIKey for exact matches
4. Group by Murcko scaffold for structural similarity

### ChEMBL Compatibility

If you're comparing against ChEMBL data, standardize using the same pipeline:

```bash
curl -X POST http://localhost:8001/api/v1/standardize \
  -d '{"molecule": "...", "options": {"include_tautomer": true}}'
```

:::info ChEMBL Uses Tautomers
ChEMBL enables tautomer canonicalization by default. Enable it if matching against ChEMBL structures.
:::

## Best Practices

1. **Always review excluded fragments**: Ensure you're not losing important counterions
2. **Preserve original structures**: Keep both original and standardized for traceability
3. **Document options**: Record whether tautomer canonicalization was used
4. **Check stereo changes**: Review stereo comparisons for critical compounds
5. **Validate after standardization**: Re-validate standardized structures

## Limitations

- **Salts**: Uncommon salts may not be recognized
- **Tautomers**: Rare tautomeric forms may not be handled correctly
- **Stereochemistry**: Complex stereo specifications may not preserve perfectly
- **Metal complexes**: Metal-organic complexes may be over-simplified

:::tip Complex Structures
For complex molecules (natural products, metal complexes, macrocycles), always manually review standardization results.
:::

## Next Steps

- **[Database Integrations](/docs/user-guide/database-integrations)** - Cross-reference standardized structures
- **[Batch Processing](/docs/user-guide/batch-processing)** - Standardize large datasets
- **[API Reference](/docs/api/endpoints)** - Full standardization API documentation
