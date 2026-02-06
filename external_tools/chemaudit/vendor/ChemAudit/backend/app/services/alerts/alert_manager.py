"""
AlertManager for Structural Alert Screening

Orchestrates structural alert pattern screening using RDKit FilterCatalog.
Screens molecules against PAINS, BRENK, and other alert patterns.

IMPORTANT: Structural alerts are warnings, not automatic rejections.
- 87 FDA-approved drugs contain PAINS patterns
- Many alerts represent potential issues, not definite problems
- Alerts should prompt investigation, not immediate rejection
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import List, Optional

from rdkit import Chem

from .filter_catalog import get_filter_catalog


class AlertSeverity(str, Enum):
    """Severity level for structural alerts."""

    CRITICAL = "critical"  # Known toxicophores, reactive groups
    WARNING = "warning"  # PAINS, BRENK - investigate but not automatic fail
    INFO = "info"  # Minor concerns, informational


@dataclass
class AlertResult:
    """Result from a structural alert pattern match."""

    pattern_name: str
    description: str
    severity: AlertSeverity
    matched_atoms: List[int]
    catalog_source: str
    smarts: Optional[str] = None  # The matching SMARTS pattern if available


@dataclass
class ScreeningResult:
    """Complete result from screening a molecule."""

    alerts: List[AlertResult] = field(default_factory=list)
    screened_catalogs: List[str] = field(default_factory=list)
    has_critical: bool = False
    has_warning: bool = False

    @property
    def total_alerts(self) -> int:
        """Total number of alerts found."""
        return len(self.alerts)

    @property
    def has_alerts(self) -> bool:
        """Whether any alerts were found."""
        return len(self.alerts) > 0


# Known patterns that appear in approved drugs (for educational context)
APPROVED_DRUG_PATTERNS = {
    "rhodanine": ["Methotrexate"],
    "catechol": ["Dopamine", "Epinephrine", "Norepinephrine", "Levodopa"],
    "quinone": ["Doxorubicin", "Mitomycin C"],
    "michael_acceptor": ["Acrylamide drugs", "Ibrutinib", "Afatinib"],
    "thiocarbonyl": ["Methimazole", "Thiouracil"],
}


def _get_severity_for_pattern(pattern_name: str, catalog_source: str) -> AlertSeverity:
    """
    Determine severity based on pattern name and catalog source.

    Most PAINS and BRENK alerts are WARNING level.
    Some specific patterns (reactive groups, known toxicophores) are CRITICAL.

    Args:
        pattern_name: Name of the matched pattern
        catalog_source: Source catalog (PAINS, BRENK, etc.)

    Returns:
        Appropriate AlertSeverity level
    """
    # Critical patterns (known toxicophores and highly reactive groups)
    critical_patterns = {
        "azide",
        "diazo",
        "nitroso",
        "nitrogen_mustard",
        "epoxide",
        "acyl_halide",
        "acyl_fluoride",
        "acyl_chloride",
        "isocyanate",
        "isothiocyanate",
        "aziridine",
        "beta_lactam",
        "peroxide",
        "aldehyde",
        "phosphorane",
    }

    pattern_lower = pattern_name.lower()

    for critical in critical_patterns:
        if critical in pattern_lower:
            return AlertSeverity.CRITICAL

    # Default to WARNING for PAINS/BRENK
    return AlertSeverity.WARNING


def _extract_matched_atoms(entry, mol: Chem.Mol) -> List[int]:
    """
    Extract matched atom indices from a FilterCatalogEntry.

    Uses entry.GetFilterMatches(mol) to get FilterMatch objects,
    then reads atomPairs where pair[1] is the molecule atom index.

    Args:
        entry: FilterCatalogEntry from catalog.GetMatches()
        mol: RDKit molecule to match against

    Returns:
        List of atom indices involved in the match
    """
    atoms = []
    try:
        filter_matches = entry.GetFilterMatches(mol)
        for fm in filter_matches:
            for pair in fm.atomPairs:
                atoms.append(int(pair[1]))
    except Exception:
        pass

    return sorted(set(atoms)) if atoms else []


class AlertManager:
    """
    Manager for structural alert screening.

    Screens molecules against configurable sets of structural alert patterns.
    Default catalogs are PAINS (A+B+C).
    """

    def __init__(self, default_catalogs: Optional[List[str]] = None):
        """
        Initialize AlertManager.

        Args:
            default_catalogs: Default catalogs to screen against. Default: ["PAINS"]
        """
        self.default_catalogs = default_catalogs or ["PAINS"]

    def screen(
        self,
        mol: Chem.Mol,
        catalogs: Optional[List[str]] = None,
        quick_check: bool = False,
    ) -> ScreeningResult:
        """
        Screen a molecule for structural alerts.

        Args:
            mol: RDKit molecule object
            catalogs: List of catalog types to screen. Uses default if None.
            quick_check: If True, only check HasMatch (faster, no atom details)

        Returns:
            ScreeningResult with all matched alerts
        """
        result = ScreeningResult()

        if mol is None:
            return result

        catalogs_to_use = catalogs or self.default_catalogs

        for catalog_type in catalogs_to_use:
            try:
                catalog_info = get_filter_catalog(catalog_type)
                result.screened_catalogs.append(catalog_type.upper())

                if quick_check:
                    # Just check if any match exists (faster)
                    if catalog_info.catalog.HasMatch(mol):
                        result.alerts.append(
                            AlertResult(
                                pattern_name=f"{catalog_type}_match",
                                description=f"Molecule matches {catalog_type} patterns",
                                severity=AlertSeverity.WARNING,
                                matched_atoms=[],
                                catalog_source=catalog_type.upper(),
                            )
                        )
                else:
                    # Get detailed matches
                    matches = catalog_info.catalog.GetMatches(mol)

                    for entry in matches:
                        # Get pattern information
                        pattern_name = entry.GetDescription() or "unknown_pattern"

                        # Try to get additional info
                        description = pattern_name

                        # Try to get SMARTS if available
                        smarts = None
                        try:
                            smarts = (
                                entry.GetSmarts()
                                if hasattr(entry, "GetSmarts")
                                else None
                            )
                        except Exception:
                            pass

                        # Get severity
                        severity = _get_severity_for_pattern(pattern_name, catalog_type)

                        # Get matched atoms via FilterMatch atom pairs
                        matched_atoms = _extract_matched_atoms(entry, mol)

                        alert = AlertResult(
                            pattern_name=pattern_name,
                            description=description,
                            severity=severity,
                            matched_atoms=matched_atoms,
                            catalog_source=catalog_type.upper(),
                            smarts=smarts,
                        )
                        result.alerts.append(alert)

                        # Track severity levels
                        if severity == AlertSeverity.CRITICAL:
                            result.has_critical = True
                        elif severity == AlertSeverity.WARNING:
                            result.has_warning = True

            except ValueError:
                # Unknown catalog type - skip but could log
                continue

        return result

    def has_alerts(self, mol: Chem.Mol, catalogs: Optional[List[str]] = None) -> bool:
        """
        Quick check if molecule has any alerts (faster than full screen).

        Args:
            mol: RDKit molecule object
            catalogs: Catalogs to check. Uses default if None.

        Returns:
            True if any alerts found
        """
        if mol is None:
            return False

        catalogs_to_use = catalogs or self.default_catalogs

        for catalog_type in catalogs_to_use:
            try:
                catalog_info = get_filter_catalog(catalog_type)
                if catalog_info.catalog.HasMatch(mol):
                    return True
            except ValueError:
                continue

        return False


# Singleton instance for common use
alert_manager = AlertManager(default_catalogs=["PAINS"])
