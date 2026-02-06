"""
Safety Filters Scorer

Identifies potentially problematic compounds using structural alert filters:
- PAINS (Pan Assay Interference Compounds)
- Brenk Filters (toxicity/unfavorable PK alerts)
- NIH/MLSMR Filters
- ZINC Filters
- ChEMBL Alerts (BMS, Dundee, Glaxo, Inpharmatica, LINT, SureChEMBL)
"""

from dataclasses import dataclass, field
from typing import List, Optional

from rdkit import Chem
from rdkit.Chem.FilterCatalog import FilterCatalog, FilterCatalogParams


@dataclass
class FilterResult:
    """Result for a single filter category."""

    passed: bool
    alerts: List[str] = field(default_factory=list)
    alert_count: int = 0


@dataclass
class ChEMBLAlertsResult:
    """ChEMBL structural alerts results."""

    passed: bool
    total_alerts: int = 0
    bms: Optional[FilterResult] = None
    dundee: Optional[FilterResult] = None
    glaxo: Optional[FilterResult] = None
    inpharmatica: Optional[FilterResult] = None
    lint: Optional[FilterResult] = None
    mlsmr: Optional[FilterResult] = None
    schembl: Optional[FilterResult] = None


@dataclass
class SafetyFilterResult:
    """Complete safety filter results."""

    pains: FilterResult
    brenk: FilterResult
    nih: Optional[FilterResult] = None
    zinc: Optional[FilterResult] = None
    chembl: Optional[ChEMBLAlertsResult] = None
    all_passed: bool = True
    total_alerts: int = 0
    interpretation: str = ""


class SafetyFilterScorer:
    """
    Identifies potentially problematic compounds using established structural filters.

    Implements:
    - PAINS: 480 substructures causing assay interference
    - Brenk: Structural alerts for toxicity and unfavorable pharmacokinetics
    - NIH: NIH Molecular Libraries structural alerts
    - ZINC: Drug-likeness and reactivity filters from ZINC database
    - ChEMBL Alerts: BMS, Dundee, Glaxo, Inpharmatica, LINT, MLSMR, SureChEMBL
    """

    def __init__(self):
        """Initialize filter catalogs."""
        self._pains_catalog = None
        self._brenk_catalog = None
        self._nih_catalog = None
        self._zinc_catalog = None
        # ChEMBL catalogs
        self._chembl_bms_catalog = None
        self._chembl_dundee_catalog = None
        self._chembl_glaxo_catalog = None
        self._chembl_inpharmatica_catalog = None
        self._chembl_lint_catalog = None
        self._chembl_mlsmr_catalog = None
        self._chembl_schembl_catalog = None
        self._catalogs_initialized = False

    def _initialize_catalogs(self) -> None:
        """Lazy initialization of filter catalogs."""
        if self._catalogs_initialized:
            return

        try:
            # PAINS filters (A, B, C combined)
            pains_params = FilterCatalogParams()
            pains_params.AddCatalog(FilterCatalogParams.FilterCatalogs.PAINS_A)
            pains_params.AddCatalog(FilterCatalogParams.FilterCatalogs.PAINS_B)
            pains_params.AddCatalog(FilterCatalogParams.FilterCatalogs.PAINS_C)
            self._pains_catalog = FilterCatalog(pains_params)

            # Brenk filters
            brenk_params = FilterCatalogParams()
            brenk_params.AddCatalog(FilterCatalogParams.FilterCatalogs.BRENK)
            self._brenk_catalog = FilterCatalog(brenk_params)

            # NIH filters
            nih_params = FilterCatalogParams()
            nih_params.AddCatalog(FilterCatalogParams.FilterCatalogs.NIH)
            self._nih_catalog = FilterCatalog(nih_params)

            # ZINC filters
            zinc_params = FilterCatalogParams()
            zinc_params.AddCatalog(FilterCatalogParams.FilterCatalogs.ZINC)
            self._zinc_catalog = FilterCatalog(zinc_params)

        except Exception:
            # Some filter catalogs may not be available in all RDKit versions
            pass

        # Initialize ChEMBL catalogs
        self._initialize_chembl_catalogs()

        self._catalogs_initialized = True

    def _create_catalog(self, catalog_type) -> Optional[FilterCatalog]:
        """Create a filter catalog from the given type, returning None if unavailable."""
        try:
            params = FilterCatalogParams()
            params.AddCatalog(catalog_type)
            return FilterCatalog(params)
        except (AttributeError, ValueError):
            return None

    def _initialize_chembl_catalogs(self) -> None:
        """Initialize ChEMBL structural alert catalogs."""
        catalogs = FilterCatalogParams.FilterCatalogs
        self._chembl_bms_catalog = self._create_catalog(catalogs.CHEMBL_BMS)
        self._chembl_dundee_catalog = self._create_catalog(catalogs.CHEMBL_Dundee)
        self._chembl_glaxo_catalog = self._create_catalog(catalogs.CHEMBL_Glaxo)
        self._chembl_inpharmatica_catalog = self._create_catalog(
            catalogs.CHEMBL_Inpharmatica
        )
        self._chembl_lint_catalog = self._create_catalog(catalogs.CHEMBL_LINT)
        self._chembl_mlsmr_catalog = self._create_catalog(catalogs.CHEMBL_MLSMR)
        self._chembl_schembl_catalog = self._create_catalog(catalogs.CHEMBL_SureChEMBL)

    def score(
        self, mol: Chem.Mol, include_extended: bool = True, include_chembl: bool = True
    ) -> SafetyFilterResult:
        """
        Run safety filters on a molecule.

        Args:
            mol: RDKit molecule object
            include_extended: Include NIH and ZINC filters
            include_chembl: Include ChEMBL structural alerts

        Returns:
            SafetyFilterResult with all filter results
        """
        self._initialize_catalogs()

        # Run core filters
        pains = self._run_filter(mol, self._pains_catalog, "PAINS")
        brenk = self._run_filter(mol, self._brenk_catalog, "Brenk")

        # Run extended filters
        nih = None
        zinc = None
        if include_extended:
            nih = self._run_filter(mol, self._nih_catalog, "NIH")
            zinc = self._run_filter(mol, self._zinc_catalog, "ZINC")

        # Run ChEMBL filters
        chembl = None
        if include_chembl:
            chembl = self._run_chembl_filters(mol)

        # Calculate totals
        total_alerts = pains.alert_count + brenk.alert_count
        if nih:
            total_alerts += nih.alert_count
        if zinc:
            total_alerts += zinc.alert_count
        if chembl:
            total_alerts += chembl.total_alerts

        all_passed = pains.passed and brenk.passed
        if include_extended:
            all_passed = all_passed and (nih is None or nih.passed)
            all_passed = all_passed and (zinc is None or zinc.passed)
        if include_chembl and chembl:
            all_passed = all_passed and chembl.passed

        # Generate interpretation
        interpretation = self._get_interpretation(pains, brenk, nih, zinc, chembl)

        return SafetyFilterResult(
            pains=pains,
            brenk=brenk,
            nih=nih,
            zinc=zinc,
            chembl=chembl,
            all_passed=all_passed,
            total_alerts=total_alerts,
            interpretation=interpretation,
        )

    def _run_chembl_filters(self, mol: Chem.Mol) -> ChEMBLAlertsResult:
        """Run all ChEMBL structural alert filters."""
        filters = {
            "bms": self._run_filter(mol, self._chembl_bms_catalog, "BMS"),
            "dundee": self._run_filter(mol, self._chembl_dundee_catalog, "Dundee"),
            "glaxo": self._run_filter(mol, self._chembl_glaxo_catalog, "Glaxo"),
            "inpharmatica": self._run_filter(
                mol, self._chembl_inpharmatica_catalog, "Inpharmatica"
            ),
            "lint": self._run_filter(mol, self._chembl_lint_catalog, "LINT"),
            "mlsmr": self._run_filter(mol, self._chembl_mlsmr_catalog, "MLSMR"),
            "schembl": self._run_filter(
                mol, self._chembl_schembl_catalog, "SureChEMBL"
            ),
        }

        total_alerts = sum(f.alert_count for f in filters.values())
        all_passed = all(f.passed for f in filters.values())

        def include_if_alerts(result: FilterResult) -> Optional[FilterResult]:
            return result if result.alert_count > 0 or not result.passed else None

        return ChEMBLAlertsResult(
            passed=all_passed,
            total_alerts=total_alerts,
            bms=include_if_alerts(filters["bms"]),
            dundee=include_if_alerts(filters["dundee"]),
            glaxo=include_if_alerts(filters["glaxo"]),
            inpharmatica=include_if_alerts(filters["inpharmatica"]),
            lint=include_if_alerts(filters["lint"]),
            mlsmr=include_if_alerts(filters["mlsmr"]),
            schembl=include_if_alerts(filters["schembl"]),
        )

    def _run_filter(
        self, mol: Chem.Mol, catalog: Optional[FilterCatalog], filter_name: str
    ) -> FilterResult:
        """Run a single filter catalog on a molecule."""
        if catalog is None:
            return FilterResult(passed=True, alerts=[], alert_count=0)

        try:
            entries = catalog.GetMatches(mol)
            alerts = []

            for entry in entries:
                description = entry.GetDescription()
                if description:
                    alerts.append(description)

            return FilterResult(
                passed=len(alerts) == 0,
                alerts=alerts,
                alert_count=len(alerts),
            )
        except Exception as e:
            return FilterResult(
                passed=True,
                alerts=[f"Filter error: {str(e)}"],
                alert_count=0,
            )

    def _get_interpretation(
        self,
        pains: FilterResult,
        brenk: FilterResult,
        nih: Optional[FilterResult],
        zinc: Optional[FilterResult],
        chembl: Optional[ChEMBLAlertsResult] = None,
    ) -> str:
        """Generate interpretation of safety filter results."""
        parts = []

        # Overall assessment
        all_clear = pains.passed and brenk.passed
        if nih:
            all_clear = all_clear and nih.passed
        if zinc:
            all_clear = all_clear and zinc.passed
        if chembl:
            all_clear = all_clear and chembl.passed

        if all_clear:
            parts.append("No structural alerts detected.")
        else:
            parts.append("Structural alerts detected - review recommended.")

        # PAINS assessment
        if not pains.passed:
            parts.append(
                f"PAINS alerts ({pains.alert_count}): May cause assay interference. "
                f"Alerts: {', '.join(pains.alerts[:3])}"
                + ("..." if len(pains.alerts) > 3 else "")
            )

        # Brenk assessment
        if not brenk.passed:
            parts.append(
                f"Brenk alerts ({brenk.alert_count}): Potential toxicity or PK issues. "
                f"Alerts: {', '.join(brenk.alerts[:3])}"
                + ("..." if len(brenk.alerts) > 3 else "")
            )

        # NIH assessment
        if nih and not nih.passed:
            parts.append(f"NIH alerts ({nih.alert_count}): HTS interference risk.")

        # ZINC assessment
        if zinc and not zinc.passed:
            parts.append(f"ZINC alerts ({zinc.alert_count}): Reactivity concerns.")

        # ChEMBL assessment
        if chembl and not chembl.passed:
            chembl_details = []
            if chembl.bms and not chembl.bms.passed:
                chembl_details.append(f"BMS({chembl.bms.alert_count})")
            if chembl.dundee and not chembl.dundee.passed:
                chembl_details.append(f"Dundee({chembl.dundee.alert_count})")
            if chembl.glaxo and not chembl.glaxo.passed:
                chembl_details.append(f"Glaxo({chembl.glaxo.alert_count})")
            if chembl.inpharmatica and not chembl.inpharmatica.passed:
                chembl_details.append(
                    f"Inpharmatica({chembl.inpharmatica.alert_count})"
                )
            if chembl.lint and not chembl.lint.passed:
                chembl_details.append(f"LINT({chembl.lint.alert_count})")
            if chembl.schembl and not chembl.schembl.passed:
                chembl_details.append(f"SureChEMBL({chembl.schembl.alert_count})")
            if chembl_details:
                parts.append(
                    f"ChEMBL alerts ({chembl.total_alerts}): {', '.join(chembl_details)}."
                )

        return " ".join(parts)

    def get_pains_alerts(self, mol: Chem.Mol) -> List[str]:
        """Get PAINS alerts only."""
        self._initialize_catalogs()
        result = self._run_filter(mol, self._pains_catalog, "PAINS")
        return result.alerts

    def get_brenk_alerts(self, mol: Chem.Mol) -> List[str]:
        """Get Brenk alerts only."""
        self._initialize_catalogs()
        result = self._run_filter(mol, self._brenk_catalog, "Brenk")
        return result.alerts

    def is_pains_clean(self, mol: Chem.Mol) -> bool:
        """Check if molecule passes PAINS filters."""
        self._initialize_catalogs()
        result = self._run_filter(mol, self._pains_catalog, "PAINS")
        return result.passed


# Module-level convenience functions
_scorer = SafetyFilterScorer()


def calculate_safety_filters(
    mol: Chem.Mol, include_extended: bool = True, include_chembl: bool = True
) -> SafetyFilterResult:
    """
    Run safety filters on a molecule.

    Args:
        mol: RDKit molecule object
        include_extended: Include NIH and ZINC filters
        include_chembl: Include ChEMBL structural alerts

    Returns:
        SafetyFilterResult with all filter results
    """
    return _scorer.score(mol, include_extended, include_chembl)


def get_pains_alerts(mol: Chem.Mol) -> List[str]:
    """Get PAINS alerts for a molecule."""
    return _scorer.get_pains_alerts(mol)


def is_pains_clean(mol: Chem.Mol) -> bool:
    """Check if molecule passes PAINS filters."""
    return _scorer.is_pains_clean(mol)
