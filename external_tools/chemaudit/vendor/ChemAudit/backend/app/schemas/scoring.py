"""
Scoring Schemas

Pydantic schemas for scoring requests and responses.
Includes drug-likeness, safety filters, and ADMET predictions.
"""

from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field, field_validator

# =============================================================================
# Drug-likeness Schemas
# =============================================================================


class LipinskiSchema(BaseModel):
    """Lipinski's Rule of Five results."""

    passed: bool = Field(description="Whether molecule passes Ro5 (<=1 violation)")
    violations: int = Field(ge=0, le=4, description="Number of rule violations")
    mw: float = Field(description="Molecular weight")
    logp: float = Field(description="Calculated LogP")
    hbd: int = Field(description="Hydrogen bond donors")
    hba: int = Field(description="Hydrogen bond acceptors")
    details: Dict[str, bool] = Field(
        default_factory=dict, description="Individual rule pass/fail"
    )


class QEDSchema(BaseModel):
    """QED score results."""

    score: float = Field(ge=0, le=1, description="QED score (0-1)")
    properties: Dict[str, Any] = Field(
        default_factory=dict, description="Component property values"
    )
    interpretation: str = Field(description="Human-readable interpretation")


class VeberSchema(BaseModel):
    """Veber rules results."""

    passed: bool = Field(description="Whether molecule passes Veber rules")
    rotatable_bonds: int = Field(description="Number of rotatable bonds")
    tpsa: float = Field(description="Topological polar surface area")


class RuleOfThreeSchema(BaseModel):
    """Rule of Three (fragment-likeness) results."""

    passed: bool = Field(description="Whether molecule passes Ro3")
    violations: int = Field(description="Number of rule violations")
    mw: float = Field(description="Molecular weight")
    logp: float = Field(description="Calculated LogP")
    hbd: int = Field(description="Hydrogen bond donors")
    hba: int = Field(description="Hydrogen bond acceptors")
    rotatable_bonds: int = Field(description="Number of rotatable bonds")
    tpsa: float = Field(description="Topological polar surface area")


class GhoseSchema(BaseModel):
    """Ghose filter results."""

    passed: bool = Field(description="Whether molecule passes Ghose filter")
    violations: int = Field(description="Number of violations")
    mw: float = Field(description="Molecular weight")
    logp: float = Field(description="Calculated LogP")
    atom_count: int = Field(description="Heavy atom count")
    molar_refractivity: float = Field(description="Molar refractivity")


class EganSchema(BaseModel):
    """Egan filter results."""

    passed: bool = Field(description="Whether molecule passes Egan filter")
    logp: float = Field(description="Calculated LogP")
    tpsa: float = Field(description="Topological polar surface area")


class MueggeSchema(BaseModel):
    """Muegge filter results."""

    passed: bool = Field(description="Whether molecule passes Muegge filter")
    violations: int = Field(description="Number of violations")
    details: Dict[str, bool] = Field(
        default_factory=dict, description="Individual criterion pass/fail"
    )


class DrugLikenessResultSchema(BaseModel):
    """Complete drug-likeness scoring results."""

    lipinski: LipinskiSchema = Field(description="Lipinski's Rule of Five")
    qed: QEDSchema = Field(description="QED score")
    veber: VeberSchema = Field(description="Veber rules")
    ro3: RuleOfThreeSchema = Field(description="Rule of Three (fragment-likeness)")
    ghose: Optional[GhoseSchema] = Field(None, description="Ghose filter")
    egan: Optional[EganSchema] = Field(None, description="Egan filter")
    muegge: Optional[MueggeSchema] = Field(None, description="Muegge filter")
    interpretation: str = Field(description="Overall drug-likeness interpretation")


# =============================================================================
# Safety Filters Schemas
# =============================================================================


class FilterAlertSchema(BaseModel):
    """Result for a single filter category."""

    passed: bool = Field(description="Whether molecule passed this filter")
    alerts: List[str] = Field(default_factory=list, description="List of alert names")
    alert_count: int = Field(default=0, description="Number of alerts triggered")


class ChEMBLAlertsSchema(BaseModel):
    """ChEMBL structural alerts results."""

    passed: bool = Field(description="Whether all ChEMBL filters passed")
    total_alerts: int = Field(default=0, description="Total ChEMBL alerts")
    bms: Optional[FilterAlertSchema] = Field(None, description="BMS filter results")
    dundee: Optional[FilterAlertSchema] = Field(
        None, description="Dundee filter results"
    )
    glaxo: Optional[FilterAlertSchema] = Field(None, description="Glaxo filter results")
    inpharmatica: Optional[FilterAlertSchema] = Field(
        None, description="Inpharmatica filter results"
    )
    lint: Optional[FilterAlertSchema] = Field(None, description="LINT filter results")
    mlsmr: Optional[FilterAlertSchema] = Field(None, description="MLSMR filter results")
    schembl: Optional[FilterAlertSchema] = Field(
        None, description="SureChEMBL filter results"
    )


class SafetyFilterResultSchema(BaseModel):
    """Complete safety filter results."""

    pains: FilterAlertSchema = Field(description="PAINS filter results")
    brenk: FilterAlertSchema = Field(description="Brenk filter results")
    nih: Optional[FilterAlertSchema] = Field(None, description="NIH filter results")
    zinc: Optional[FilterAlertSchema] = Field(None, description="ZINC filter results")
    chembl: Optional[ChEMBLAlertsSchema] = Field(
        None, description="ChEMBL structural alerts"
    )
    all_passed: bool = Field(description="Whether all filters passed")
    total_alerts: int = Field(default=0, description="Total number of alerts")
    interpretation: str = Field(description="Safety assessment interpretation")


# =============================================================================
# ADMET Schemas
# =============================================================================


class SyntheticAccessibilitySchema(BaseModel):
    """Synthetic accessibility score result."""

    score: float = Field(ge=1, le=10, description="SA score (1=easy, 10=difficult)")
    classification: str = Field(description="easy, moderate, or difficult")
    interpretation: str = Field(description="Human-readable interpretation")


class SolubilitySchema(BaseModel):
    """ESOL solubility prediction result."""

    log_s: float = Field(description="Log solubility (mol/L)")
    solubility_mg_ml: float = Field(description="Solubility in mg/mL")
    classification: str = Field(
        description="highly_soluble, soluble, moderate, poor, or insoluble"
    )
    interpretation: str = Field(description="Human-readable interpretation")


class ComplexitySchema(BaseModel):
    """Molecular complexity metrics."""

    fsp3: float = Field(ge=0, le=1, description="Fraction of sp3 carbons")
    num_stereocenters: int = Field(description="Number of stereocenters")
    num_rings: int = Field(description="Total number of rings")
    num_aromatic_rings: int = Field(description="Number of aromatic rings")
    bertz_ct: float = Field(description="Bertz complexity index")
    classification: str = Field(description="flat, moderate, or 3d")
    interpretation: str = Field(description="Human-readable interpretation")


class CNSMPOSchema(BaseModel):
    """CNS MPO score result."""

    score: float = Field(ge=0, le=6, description="CNS MPO score (0-6)")
    components: Dict[str, float] = Field(
        default_factory=dict, description="Individual component scores"
    )
    cns_penetrant: bool = Field(description="Predicted CNS penetration")
    interpretation: str = Field(description="Human-readable interpretation")


class BioavailabilitySchema(BaseModel):
    """Bioavailability indicators."""

    tpsa: float = Field(description="Topological polar surface area")
    rotatable_bonds: int = Field(description="Number of rotatable bonds")
    hbd: int = Field(description="Hydrogen bond donors")
    hba: int = Field(description="Hydrogen bond acceptors")
    mw: float = Field(description="Molecular weight")
    logp: float = Field(description="Calculated LogP")
    oral_absorption_likely: bool = Field(description="Predicted oral absorption")
    cns_penetration_likely: bool = Field(description="Predicted CNS penetration")
    interpretation: str = Field(description="Human-readable interpretation")


class PfizerRuleSchema(BaseModel):
    """Pfizer 3/75 Rule result."""

    passed: bool = Field(description="Whether molecule passes Pfizer 3/75 rule")
    logp: float = Field(description="Calculated LogP")
    tpsa: float = Field(description="Topological polar surface area")
    interpretation: str = Field(description="Rule interpretation")


class GSKRuleSchema(BaseModel):
    """GSK 4/400 Rule result."""

    passed: bool = Field(description="Whether molecule passes GSK 4/400 rule")
    mw: float = Field(description="Molecular weight")
    logp: float = Field(description="Calculated LogP")
    interpretation: str = Field(description="Rule interpretation")


class GoldenTriangleSchema(BaseModel):
    """Golden Triangle (Abbott) analysis."""

    in_golden_triangle: bool = Field(
        description="Whether molecule is in Golden Triangle"
    )
    mw: float = Field(description="Molecular weight")
    logd: float = Field(description="LogD (using LogP as proxy)")
    interpretation: str = Field(description="Analysis interpretation")


class ADMETResultSchema(BaseModel):
    """Complete ADMET prediction results."""

    synthetic_accessibility: SyntheticAccessibilitySchema = Field(
        description="Synthetic accessibility score"
    )
    solubility: SolubilitySchema = Field(description="Solubility prediction")
    complexity: ComplexitySchema = Field(description="Molecular complexity metrics")
    cns_mpo: Optional[CNSMPOSchema] = Field(None, description="CNS MPO score")
    bioavailability: BioavailabilitySchema = Field(
        description="Bioavailability indicators"
    )
    pfizer_rule: Optional[PfizerRuleSchema] = Field(
        None, description="Pfizer 3/75 rule assessment"
    )
    gsk_rule: Optional[GSKRuleSchema] = Field(
        None, description="GSK 4/400 rule assessment"
    )
    golden_triangle: Optional[GoldenTriangleSchema] = Field(
        None, description="Golden Triangle analysis"
    )
    molar_refractivity: Optional[float] = Field(None, description="Molar refractivity")
    interpretation: str = Field(description="Overall ADMET interpretation")


# =============================================================================
# Aggregator Likelihood Schema
# =============================================================================


class AggregatorLikelihoodSchema(BaseModel):
    """Aggregator likelihood prediction result."""

    likelihood: str = Field(description="Aggregation likelihood: low, moderate, high")
    risk_score: float = Field(ge=0, le=1, description="Risk score (0-1)")
    logp: float = Field(description="Calculated LogP")
    tpsa: float = Field(description="Topological polar surface area")
    mw: float = Field(description="Molecular weight")
    aromatic_rings: int = Field(description="Number of aromatic rings")
    risk_factors: List[str] = Field(
        default_factory=list, description="Identified risk factors"
    )
    interpretation: str = Field(description="Interpretation of aggregation risk")


# =============================================================================
# ML-Readiness Schemas (existing)
# =============================================================================


class MLReadinessBreakdownSchema(BaseModel):
    """Breakdown of ML-readiness score components."""

    descriptors_score: float = Field(
        description="Score for descriptor calculability (0-40)"
    )
    descriptors_max: float = Field(default=40.0, description="Maximum descriptor score")
    descriptors_successful: int = Field(
        description="Number of successfully calculated descriptors"
    )
    descriptors_total: int = Field(description="Total number of descriptors attempted")

    fingerprints_score: float = Field(
        description="Score for fingerprint generation (0-40)"
    )
    fingerprints_max: float = Field(
        default=40.0, description="Maximum fingerprint score"
    )
    fingerprints_successful: List[str] = Field(
        description="Successfully generated fingerprint types"
    )
    fingerprints_failed: List[str] = Field(
        default_factory=list, description="Failed fingerprint types"
    )

    size_score: float = Field(description="Score for size constraints (0-20)")
    size_max: float = Field(default=20.0, description="Maximum size score")
    molecular_weight: Optional[float] = Field(None, description="Molecular weight")
    num_atoms: Optional[int] = Field(None, description="Number of atoms")
    size_category: str = Field(
        description="Size category: optimal, acceptable, or out_of_range"
    )


class MLReadinessResultSchema(BaseModel):
    """Result of ML-readiness scoring."""

    score: int = Field(ge=0, le=100, description="Overall ML-readiness score (0-100)")
    breakdown: MLReadinessBreakdownSchema = Field(
        description="Score breakdown by component"
    )
    interpretation: str = Field(
        description="Human-readable interpretation of the score"
    )
    failed_descriptors: List[str] = Field(
        default_factory=list, description="List of failed descriptor names"
    )


class NPLikenessResultSchema(BaseModel):
    """Result of NP-likeness scoring."""

    score: float = Field(description="NP-likeness score (typically -5 to +5)")
    interpretation: str = Field(description="Human-readable interpretation")
    caveats: List[str] = Field(
        default_factory=list, description="Warnings or limitations"
    )
    details: Dict[str, Any] = Field(
        default_factory=dict, description="Additional scoring details"
    )


class ScaffoldResultSchema(BaseModel):
    """Result of scaffold extraction."""

    scaffold_smiles: str = Field(description="SMILES of the Murcko scaffold")
    generic_scaffold_smiles: str = Field(
        description="SMILES of the generic (framework) scaffold"
    )
    has_scaffold: bool = Field(
        description="Whether a scaffold was found (False for acyclic molecules)"
    )
    message: str = Field(description="Status message about the extraction")
    details: Dict[str, Any] = Field(
        default_factory=dict, description="Additional extraction details"
    )


class ScoringRequest(BaseModel):
    """Request for molecule scoring."""

    molecule: str = Field(
        ...,
        min_length=1,
        max_length=10000,
        description="Molecule string (SMILES, InChI, or MOL block)",
    )
    format: str = Field(
        default="auto", pattern="^(auto|smiles|inchi|mol)$", description="Input format"
    )
    include: List[str] = Field(
        default=[
            "ml_readiness",
            "np_likeness",
            "scaffold",
            "druglikeness",
            "safety_filters",
            "admet",
            "aggregator",
        ],
        description="Scoring types to include",
    )

    @field_validator("molecule")
    @classmethod
    def sanitize_molecule_input(cls, v: str) -> str:
        """Sanitize molecule input."""
        dangerous = ["<", ">", "&", ";", "|", "$", "`"]
        if any(c in v for c in dangerous):
            raise ValueError("Invalid characters in molecule string")
        return v.strip()

    @field_validator("include")
    @classmethod
    def validate_include(cls, v: List[str]) -> List[str]:
        """Validate include list."""
        valid_types = {
            "ml_readiness",
            "np_likeness",
            "scaffold",
            "druglikeness",
            "safety_filters",
            "admet",
            "aggregator",
        }
        for item in v:
            if item not in valid_types:
                raise ValueError(
                    f"Invalid scoring type: {item}. Valid types: {valid_types}"
                )
        return v


class MoleculeInfoSchema(BaseModel):
    """Basic molecule information."""

    input_string: str = Field(description="Original input string")
    canonical_smiles: Optional[str] = Field(None, description="Canonical SMILES")
    molecular_formula: Optional[str] = Field(None, description="Molecular formula")
    molecular_weight: Optional[float] = Field(None, description="Molecular weight")


class ScoringResponse(BaseModel):
    """Response containing scoring results."""

    status: str = Field(default="completed", description="Request status")
    molecule_info: MoleculeInfoSchema = Field(description="Basic molecule information")
    ml_readiness: Optional[MLReadinessResultSchema] = Field(
        None, description="ML-readiness scoring results"
    )
    np_likeness: Optional[NPLikenessResultSchema] = Field(
        None, description="NP-likeness scoring results"
    )
    scaffold: Optional[ScaffoldResultSchema] = Field(
        None, description="Scaffold extraction results"
    )
    druglikeness: Optional[DrugLikenessResultSchema] = Field(
        None, description="Drug-likeness scoring results"
    )
    safety_filters: Optional[SafetyFilterResultSchema] = Field(
        None, description="Safety filter results"
    )
    admet: Optional[ADMETResultSchema] = Field(
        None, description="ADMET prediction results"
    )
    aggregator: Optional[AggregatorLikelihoodSchema] = Field(
        None, description="Aggregator likelihood prediction"
    )
    execution_time_ms: int = Field(description="Execution time in milliseconds")
