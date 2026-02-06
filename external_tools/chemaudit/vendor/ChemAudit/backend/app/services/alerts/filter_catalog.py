"""
FilterCatalog Singleton for Structural Alert Screening

Provides cached access to RDKit FilterCatalog for PAINS, BRENK, and other
structural alert pattern sets. Uses lru_cache for singleton behavior.

PAINS (Pan-Assay INterference compoundS):
- PAINS_A: 16 patterns (most severe)
- PAINS_B: 55 patterns
- PAINS_C: 409 patterns
- Total: 480 patterns

BRENK: 105 patterns for known problematic functional groups

Note: Structural alerts are warnings, not automatic rejections.
87 FDA-approved drugs contain PAINS patterns.
"""

from dataclasses import dataclass
from functools import lru_cache
from typing import Dict

from rdkit.Chem.FilterCatalog import FilterCatalog, FilterCatalogParams

# Available catalog types and their descriptions
AVAILABLE_CATALOGS: Dict[str, Dict[str, str]] = {
    "PAINS": {
        "name": "PAINS (Pan-Assay INterference compoundS)",
        "description": "480 patterns for frequent hitters in high-throughput screens",
        "pattern_count": "480",
        "severity": "warning",
        "note": "87 FDA-approved drugs contain PAINS patterns",
    },
    "PAINS_A": {
        "name": "PAINS Class A",
        "description": "16 most severe PAINS patterns",
        "pattern_count": "16",
        "severity": "warning",
    },
    "PAINS_B": {
        "name": "PAINS Class B",
        "description": "55 moderate PAINS patterns",
        "pattern_count": "55",
        "severity": "warning",
    },
    "PAINS_C": {
        "name": "PAINS Class C",
        "description": "409 mild PAINS patterns",
        "pattern_count": "409",
        "severity": "warning",
    },
    "BRENK": {
        "name": "Brenk Structural Alerts",
        "description": "105 patterns for potentially problematic functional groups",
        "pattern_count": "105",
        "severity": "warning",
    },
    "NIH": {
        "name": "NIH Filters",
        "description": "NIH/NCGC structural alert patterns",
        "pattern_count": "variable",
        "severity": "warning",
    },
    "ZINC": {
        "name": "ZINC Patterns",
        "description": "ZINC database structural alerts",
        "pattern_count": "variable",
        "severity": "warning",
    },
    "CHEMBL_BMS": {
        "name": "ChEMBL BMS Alerts",
        "description": "Bristol-Myers Squibb HTS deck filters",
        "pattern_count": "variable",
        "severity": "warning",
    },
    "CHEMBL_DUNDEE": {
        "name": "ChEMBL Dundee Alerts",
        "description": "University of Dundee NTD screening filters",
        "pattern_count": "variable",
        "severity": "warning",
    },
    "CHEMBL_GLAXO": {
        "name": "ChEMBL Glaxo Alerts",
        "description": "GlaxoSmithKline hard and undesirable compound filters",
        "pattern_count": "variable",
        "severity": "warning",
    },
    "CHEMBL_INPHARMATICA": {
        "name": "ChEMBL Inpharmatica Alerts",
        "description": "Inpharmatica unwanted fragment filters",
        "pattern_count": "variable",
        "severity": "warning",
    },
    "CHEMBL_LINT": {
        "name": "ChEMBL LINT Alerts",
        "description": "Lilly MedChem rules for compound quality",
        "pattern_count": "variable",
        "severity": "warning",
    },
    "CHEMBL_MLSMR": {
        "name": "ChEMBL MLSMR Alerts",
        "description": "NIH MLSMR excluded functionality filters",
        "pattern_count": "variable",
        "severity": "warning",
    },
    "CHEMBL_SURECHEMBL": {
        "name": "ChEMBL SureChEMBL Alerts",
        "description": "SureChEMBL patent-derived structural alerts",
        "pattern_count": "variable",
        "severity": "warning",
    },
    "ALL": {
        "name": "All Catalogs Combined",
        "description": "All available structural alert patterns",
        "pattern_count": "variable",
        "severity": "warning",
    },
}


@dataclass
class CatalogInfo:
    """Information about a loaded FilterCatalog."""

    catalog: FilterCatalog
    catalog_type: str
    num_entries: int


def _build_catalog_params(catalog_type: str) -> FilterCatalogParams:
    """
    Build FilterCatalogParams for the specified catalog type.

    Args:
        catalog_type: One of PAINS, PAINS_A, PAINS_B, PAINS_C, BRENK, NIH, ZINC, ALL

    Returns:
        Configured FilterCatalogParams
    """
    params = FilterCatalogParams()
    catalog_type_upper = catalog_type.upper()

    if catalog_type_upper == "PAINS":
        # All PAINS classes (A + B + C)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.PAINS_A)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.PAINS_B)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.PAINS_C)
    elif catalog_type_upper == "PAINS_A":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.PAINS_A)
    elif catalog_type_upper == "PAINS_B":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.PAINS_B)
    elif catalog_type_upper == "PAINS_C":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.PAINS_C)
    elif catalog_type_upper == "BRENK":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.BRENK)
    elif catalog_type_upper == "NIH":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.NIH)
    elif catalog_type_upper == "ZINC":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.ZINC)
    elif catalog_type_upper == "CHEMBL_BMS":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_BMS)
    elif catalog_type_upper == "CHEMBL_DUNDEE":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_Dundee)
    elif catalog_type_upper == "CHEMBL_GLAXO":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_Glaxo)
    elif catalog_type_upper == "CHEMBL_INPHARMATICA":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_Inpharmatica)
    elif catalog_type_upper == "CHEMBL_LINT":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_LINT)
    elif catalog_type_upper == "CHEMBL_MLSMR":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_MLSMR)
    elif catalog_type_upper == "CHEMBL_SURECHEMBL":
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_SureChEMBL)
    elif catalog_type_upper == "ALL":
        # Add all available catalogs
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.PAINS_A)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.PAINS_B)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.PAINS_C)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.BRENK)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.NIH)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.ZINC)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_BMS)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_Dundee)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_Glaxo)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_Inpharmatica)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_LINT)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_MLSMR)
        params.AddCatalog(FilterCatalogParams.FilterCatalogs.CHEMBL_SureChEMBL)
    else:
        raise ValueError(
            f"Unknown catalog type: {catalog_type}. Available: {list(AVAILABLE_CATALOGS.keys())}"
        )

    return params


@lru_cache(maxsize=16)
def get_filter_catalog(catalog_type: str = "PAINS") -> CatalogInfo:
    """
    Get a cached FilterCatalog for the specified catalog type.

    Uses lru_cache for singleton-like behavior per catalog type.
    First call initializes the catalog; subsequent calls return cached instance.

    Args:
        catalog_type: Catalog type (PAINS, PAINS_A, PAINS_B, PAINS_C, BRENK, NIH, ZINC, ALL)

    Returns:
        CatalogInfo with initialized FilterCatalog

    Raises:
        ValueError: If catalog_type is unknown
    """
    catalog_type_upper = catalog_type.upper()

    if catalog_type_upper not in AVAILABLE_CATALOGS:
        raise ValueError(
            f"Unknown catalog type: {catalog_type}. Available: {list(AVAILABLE_CATALOGS.keys())}"
        )

    params = _build_catalog_params(catalog_type_upper)
    catalog = FilterCatalog(params)

    return CatalogInfo(
        catalog=catalog,
        catalog_type=catalog_type_upper,
        num_entries=catalog.GetNumEntries(),
    )


def list_available_catalogs() -> Dict[str, Dict[str, str]]:
    """
    List all available filter catalogs with their descriptions.

    Returns:
        Dictionary mapping catalog type to description info
    """
    return AVAILABLE_CATALOGS.copy()


def get_catalog_stats() -> Dict[str, int]:
    """
    Get pattern counts for each catalog type.

    Note: This loads each catalog which may be slow on first call.

    Returns:
        Dictionary mapping catalog type to pattern count
    """
    stats = {}
    for catalog_type in AVAILABLE_CATALOGS:
        if catalog_type != "ALL":  # Skip ALL to avoid counting duplicates
            info = get_filter_catalog(catalog_type)
            stats[catalog_type] = info.num_entries
    return stats
