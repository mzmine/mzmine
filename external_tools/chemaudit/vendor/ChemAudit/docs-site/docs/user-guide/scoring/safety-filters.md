---
sidebar_position: 4
title: Safety Filters
description: Screen for problematic substructures using PAINS, BRENK, NIH, ZINC, and ChEMBL filters
---

# Safety Filter Scoring

Safety filter scoring evaluates molecules against structural alert databases to identify potentially problematic compounds. This is a scored summary of the detailed [Structural Alerts](/docs/user-guide/structural-alerts) screening.

## What It Measures

Safety filter scoring tests molecules against multiple catalogs:

| Catalog | Patterns | Focus |
|---------|----------|-------|
| **PAINS** | ~480 | Pan-assay interference compounds |
| **BRENK** | ~105 | Unwanted chemical moieties |
| **NIH** | ~180 | NIH MLSMR excluded structures |
| **ZINC** | ~95 | ZINC database filters |
| **ChEMBL** | ~700+ | Pharma company filters (BMS, Dundee, Glaxo, etc.) |

## Scoring Output

The safety filter score provides a pass/fail summary:

```json
{
  "safety_filters": {
    "pains": {
      "passed": true,
      "alerts": [],
      "alert_count": 0
    },
    "brenk": {
      "passed": true,
      "alerts": [],
      "alert_count": 0
    },
    "nih": {
      "passed": true,
      "alerts": [],
      "alert_count": 0
    },
    "zinc": {
      "passed": true,
      "alerts": [],
      "alert_count": 0
    },
    "chembl": {
      "passed": true,
      "total_alerts": 0
    },
    "all_passed": true,
    "total_alerts": 0,
    "interpretation": "No safety alerts detected"
  }
}
```

## Interpretation

| Result | Interpretation | Recommendation |
|--------|---------------|----------------|
| **All passed** | No alerts found | Good safety profile for screening |
| **PAINS only** | Assay interference risk | Review assay compatibility before screening |
| **Multiple catalogs** | Multiple issues | Investigate further, consider excluding |

## Safety Filter vs. Structural Alerts

**Safety Filter Scoring (this page):**
- Part of comprehensive scoring API
- Pass/fail summary only
- Quick screening
- Included in batch processing

**[Structural Alerts](/docs/user-guide/structural-alerts):**
- Detailed alert information
- Matched atoms identified
- Pattern names and descriptions
- Dedicated API endpoint

Use safety filter scoring for quick filtering, then use detailed structural alerts for investigation.

## Use in Batch Processing

In batch mode, safety filter pass rate is reported:

```json
{
  "statistics": {
    "safety_pass_rate": 0.91
  }
}
```

This indicates 91% of molecules pass all safety filters.

## Best Practices

1. **Use for prioritization**: Not absolute rejection criteria
2. **Review alerts**: Always investigate specific alerts for hits
3. **Consider context**: 87 FDA-approved drugs match PAINS patterns
4. **Check assay type**: PAINS alerts are assay-specific
5. **Document decisions**: Record why you accepted/rejected flagged molecules

## Next Steps

- **[Structural Alerts](/docs/user-guide/structural-alerts)** - Detailed alert information
- **[Scoring Overview](/docs/user-guide/scoring/overview)** - All scoring systems
- **[API Reference](/docs/api/endpoints)** - Full API documentation
