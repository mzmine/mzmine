"""
Scoring API Routes

Endpoints for molecule scoring including:
- ML-readiness
- NP-likeness
- Scaffold extraction
- Drug-likeness (Lipinski, QED, Veber, Ro3, Ghose, Egan, Muegge)
- Safety filters (PAINS, Brenk, NIH, ZINC, ChEMBL)
- ADMET predictions (SAscore, ESOL, Fsp3, CNS MPO, Pfizer/GSK rules)
- Aggregator likelihood prediction
"""

import time
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Request
from rdkit import Chem
from rdkit.Chem import Descriptors, rdMolDescriptors

from app.core.rate_limit import get_rate_limit_key, limiter
from app.core.security import get_api_key
from app.schemas.scoring import (
    ADMETResultSchema,
    AggregatorLikelihoodSchema,
    BioavailabilitySchema,
    ChEMBLAlertsSchema,
    CNSMPOSchema,
    ComplexitySchema,
    DrugLikenessResultSchema,
    EganSchema,
    FilterAlertSchema,
    GhoseSchema,
    GoldenTriangleSchema,
    GSKRuleSchema,
    LipinskiSchema,
    MLReadinessBreakdownSchema,
    MLReadinessResultSchema,
    MoleculeInfoSchema,
    MueggeSchema,
    NPLikenessResultSchema,
    PfizerRuleSchema,
    QEDSchema,
    RuleOfThreeSchema,
    SafetyFilterResultSchema,
    ScaffoldResultSchema,
    ScoringRequest,
    ScoringResponse,
    SolubilitySchema,
    SyntheticAccessibilitySchema,
    VeberSchema,
)
from app.services.parser.molecule_parser import MoleculeFormat, parse_molecule
from app.services.scoring import (
    calculate_admet,
    calculate_aggregator_likelihood,
    calculate_druglikeness,
    calculate_ml_readiness,
    calculate_np_likeness,
    calculate_safety_filters,
    extract_scaffold,
)

router = APIRouter()


@router.post("/score", response_model=ScoringResponse)
@limiter.limit("10/minute", key_func=get_rate_limit_key)
async def score_molecule(
    request: Request,
    body: ScoringRequest,
    api_key: Optional[str] = Depends(get_api_key),
):
    """
    Calculate scores for a molecule.

    Supported scoring types:
    - ml_readiness: ML-readiness score (0-100) with breakdown
    - np_likeness: Natural product likeness score (-5 to +5)
    - scaffold: Murcko scaffold extraction
    - druglikeness: Drug-likeness filters (Lipinski, QED, Veber, Ro3, etc.)
    - safety_filters: Safety alerts (PAINS, Brenk, NIH, ZINC, ChEMBL)
    - admet: ADMET predictions (SAscore, solubility, Pfizer/GSK rules, etc.)
    - aggregator: Aggregator likelihood prediction

    Args:
        body: ScoringRequest with molecule and options

    Returns:
        ScoringResponse with requested scoring results

    Raises:
        HTTPException: If molecule cannot be parsed
    """
    start_time = time.time()

    # Parse molecule
    format_map = {
        "smiles": MoleculeFormat.SMILES,
        "inchi": MoleculeFormat.INCHI,
        "mol": MoleculeFormat.MOL,
        "auto": None,
    }
    input_format = format_map.get(body.format)

    parse_result = parse_molecule(body.molecule, input_format)

    if not parse_result.success or parse_result.mol is None:
        raise HTTPException(
            status_code=400,
            detail={
                "error": "Failed to parse molecule",
                "errors": parse_result.errors,
                "warnings": parse_result.warnings,
                "format_detected": parse_result.format_detected.value,
            },
        )

    mol = parse_result.mol

    # Extract basic molecule info
    mol_info = _extract_molecule_info(mol, body.molecule)

    # Initialize response components
    ml_readiness_result = None
    np_likeness_result = None
    scaffold_result = None
    druglikeness_result = None
    safety_filters_result = None
    admet_result = None
    aggregator_result = None

    # Calculate requested scores
    if "ml_readiness" in body.include:
        ml_result = calculate_ml_readiness(mol)
        ml_readiness_result = MLReadinessResultSchema(
            score=ml_result.score,
            breakdown=MLReadinessBreakdownSchema(
                descriptors_score=ml_result.breakdown.descriptors_score,
                descriptors_max=ml_result.breakdown.descriptors_max,
                descriptors_successful=ml_result.breakdown.descriptors_successful,
                descriptors_total=ml_result.breakdown.descriptors_total,
                fingerprints_score=ml_result.breakdown.fingerprints_score,
                fingerprints_max=ml_result.breakdown.fingerprints_max,
                fingerprints_successful=ml_result.breakdown.fingerprints_successful,
                fingerprints_failed=ml_result.breakdown.fingerprints_failed,
                size_score=ml_result.breakdown.size_score,
                size_max=ml_result.breakdown.size_max,
                molecular_weight=ml_result.breakdown.molecular_weight,
                num_atoms=ml_result.breakdown.num_atoms,
                size_category=ml_result.breakdown.size_category,
            ),
            interpretation=ml_result.interpretation,
            failed_descriptors=ml_result.failed_descriptors,
        )

    if "np_likeness" in body.include:
        np_result = calculate_np_likeness(mol)
        np_likeness_result = NPLikenessResultSchema(
            score=np_result.score,
            interpretation=np_result.interpretation,
            caveats=np_result.caveats,
            details=np_result.details,
        )

    if "scaffold" in body.include:
        scaffold_res = extract_scaffold(mol)
        scaffold_result = ScaffoldResultSchema(
            scaffold_smiles=scaffold_res.scaffold_smiles,
            generic_scaffold_smiles=scaffold_res.generic_scaffold_smiles,
            has_scaffold=scaffold_res.has_scaffold,
            message=scaffold_res.message,
            details=scaffold_res.details,
        )

    if "druglikeness" in body.include:
        druglikeness_result = _calculate_druglikeness(mol)

    if "safety_filters" in body.include:
        safety_filters_result = _calculate_safety_filters(mol)

    if "admet" in body.include:
        admet_result = _calculate_admet(mol)

    if "aggregator" in body.include:
        aggregator_result = _calculate_aggregator(mol)

    execution_time = int((time.time() - start_time) * 1000)

    return ScoringResponse(
        molecule_info=mol_info,
        ml_readiness=ml_readiness_result,
        np_likeness=np_likeness_result,
        scaffold=scaffold_result,
        druglikeness=druglikeness_result,
        safety_filters=safety_filters_result,
        admet=admet_result,
        aggregator=aggregator_result,
        execution_time_ms=execution_time,
    )


def _calculate_druglikeness(mol: Chem.Mol) -> DrugLikenessResultSchema:
    """Calculate drug-likeness scores and convert to schema."""
    result = calculate_druglikeness(mol, include_extended=True)

    # Build Lipinski schema
    lipinski = LipinskiSchema(
        passed=result.lipinski.passed,
        violations=result.lipinski.violations,
        mw=result.lipinski.mw,
        logp=result.lipinski.logp,
        hbd=result.lipinski.hbd,
        hba=result.lipinski.hba,
        details=result.lipinski.details,
    )

    # Build QED schema
    qed = QEDSchema(
        score=result.qed.score,
        properties=result.qed.properties,
        interpretation=result.qed.interpretation,
    )

    # Build Veber schema
    veber = VeberSchema(
        passed=result.veber.passed,
        rotatable_bonds=result.veber.rotatable_bonds,
        tpsa=result.veber.tpsa,
    )

    # Build Ro3 schema
    ro3 = RuleOfThreeSchema(
        passed=result.ro3.passed,
        violations=result.ro3.violations,
        mw=result.ro3.mw,
        logp=result.ro3.logp,
        hbd=result.ro3.hbd,
        hba=result.ro3.hba,
        rotatable_bonds=result.ro3.rotatable_bonds,
        tpsa=result.ro3.tpsa,
    )

    # Build optional extended filter schemas
    ghose = None
    if result.ghose:
        ghose = GhoseSchema(
            passed=result.ghose.passed,
            violations=result.ghose.violations,
            mw=result.ghose.mw,
            logp=result.ghose.logp,
            atom_count=result.ghose.atom_count,
            molar_refractivity=result.ghose.molar_refractivity,
        )

    egan = None
    if result.egan:
        egan = EganSchema(
            passed=result.egan.passed,
            logp=result.egan.logp,
            tpsa=result.egan.tpsa,
        )

    muegge = None
    if result.muegge:
        muegge = MueggeSchema(
            passed=result.muegge.passed,
            violations=result.muegge.violations,
            details=result.muegge.details,
        )

    return DrugLikenessResultSchema(
        lipinski=lipinski,
        qed=qed,
        veber=veber,
        ro3=ro3,
        ghose=ghose,
        egan=egan,
        muegge=muegge,
        interpretation=result.interpretation,
    )


def _calculate_safety_filters(mol: Chem.Mol) -> SafetyFilterResultSchema:
    """Calculate safety filter results and convert to schema."""
    result = calculate_safety_filters(mol, include_extended=True, include_chembl=True)

    pains = FilterAlertSchema(
        passed=result.pains.passed,
        alerts=result.pains.alerts,
        alert_count=result.pains.alert_count,
    )

    brenk = FilterAlertSchema(
        passed=result.brenk.passed,
        alerts=result.brenk.alerts,
        alert_count=result.brenk.alert_count,
    )

    nih = None
    if result.nih:
        nih = FilterAlertSchema(
            passed=result.nih.passed,
            alerts=result.nih.alerts,
            alert_count=result.nih.alert_count,
        )

    zinc = None
    if result.zinc:
        zinc = FilterAlertSchema(
            passed=result.zinc.passed,
            alerts=result.zinc.alerts,
            alert_count=result.zinc.alert_count,
        )

    # ChEMBL alerts
    chembl = None
    if result.chembl:
        chembl = ChEMBLAlertsSchema(
            passed=result.chembl.passed,
            total_alerts=result.chembl.total_alerts,
            bms=(
                FilterAlertSchema(
                    passed=result.chembl.bms.passed,
                    alerts=result.chembl.bms.alerts,
                    alert_count=result.chembl.bms.alert_count,
                )
                if result.chembl.bms
                else None
            ),
            dundee=(
                FilterAlertSchema(
                    passed=result.chembl.dundee.passed,
                    alerts=result.chembl.dundee.alerts,
                    alert_count=result.chembl.dundee.alert_count,
                )
                if result.chembl.dundee
                else None
            ),
            glaxo=(
                FilterAlertSchema(
                    passed=result.chembl.glaxo.passed,
                    alerts=result.chembl.glaxo.alerts,
                    alert_count=result.chembl.glaxo.alert_count,
                )
                if result.chembl.glaxo
                else None
            ),
            inpharmatica=(
                FilterAlertSchema(
                    passed=result.chembl.inpharmatica.passed,
                    alerts=result.chembl.inpharmatica.alerts,
                    alert_count=result.chembl.inpharmatica.alert_count,
                )
                if result.chembl.inpharmatica
                else None
            ),
            lint=(
                FilterAlertSchema(
                    passed=result.chembl.lint.passed,
                    alerts=result.chembl.lint.alerts,
                    alert_count=result.chembl.lint.alert_count,
                )
                if result.chembl.lint
                else None
            ),
            mlsmr=(
                FilterAlertSchema(
                    passed=result.chembl.mlsmr.passed,
                    alerts=result.chembl.mlsmr.alerts,
                    alert_count=result.chembl.mlsmr.alert_count,
                )
                if result.chembl.mlsmr
                else None
            ),
            schembl=(
                FilterAlertSchema(
                    passed=result.chembl.schembl.passed,
                    alerts=result.chembl.schembl.alerts,
                    alert_count=result.chembl.schembl.alert_count,
                )
                if result.chembl.schembl
                else None
            ),
        )

    return SafetyFilterResultSchema(
        pains=pains,
        brenk=brenk,
        nih=nih,
        zinc=zinc,
        chembl=chembl,
        all_passed=result.all_passed,
        total_alerts=result.total_alerts,
        interpretation=result.interpretation,
    )


def _calculate_admet(mol: Chem.Mol) -> ADMETResultSchema:
    """Calculate ADMET predictions and convert to schema."""
    result = calculate_admet(mol, include_cns_mpo=True, include_rules=True)

    synthetic_accessibility = SyntheticAccessibilitySchema(
        score=result.synthetic_accessibility.score,
        classification=result.synthetic_accessibility.classification,
        interpretation=result.synthetic_accessibility.interpretation,
    )

    solubility = SolubilitySchema(
        log_s=result.solubility.log_s,
        solubility_mg_ml=result.solubility.solubility_mg_ml,
        classification=result.solubility.classification,
        interpretation=result.solubility.interpretation,
    )

    complexity = ComplexitySchema(
        fsp3=result.complexity.fsp3,
        num_stereocenters=result.complexity.num_stereocenters,
        num_rings=result.complexity.num_rings,
        num_aromatic_rings=result.complexity.num_aromatic_rings,
        bertz_ct=result.complexity.bertz_ct,
        classification=result.complexity.classification,
        interpretation=result.complexity.interpretation,
    )

    cns_mpo = None
    if result.cns_mpo:
        cns_mpo = CNSMPOSchema(
            score=result.cns_mpo.score,
            components=result.cns_mpo.components,
            cns_penetrant=result.cns_mpo.cns_penetrant,
            interpretation=result.cns_mpo.interpretation,
        )

    bioavailability = BioavailabilitySchema(
        tpsa=result.bioavailability.tpsa,
        rotatable_bonds=result.bioavailability.rotatable_bonds,
        hbd=result.bioavailability.hbd,
        hba=result.bioavailability.hba,
        mw=result.bioavailability.mw,
        logp=result.bioavailability.logp,
        oral_absorption_likely=result.bioavailability.oral_absorption_likely,
        cns_penetration_likely=result.bioavailability.cns_penetration_likely,
        interpretation=result.bioavailability.interpretation,
    )

    # Pfizer 3/75 Rule
    pfizer_rule = None
    if result.pfizer_rule:
        pfizer_rule = PfizerRuleSchema(
            passed=result.pfizer_rule.passed,
            logp=result.pfizer_rule.logp,
            tpsa=result.pfizer_rule.tpsa,
            interpretation=result.pfizer_rule.interpretation,
        )

    # GSK 4/400 Rule
    gsk_rule = None
    if result.gsk_rule:
        gsk_rule = GSKRuleSchema(
            passed=result.gsk_rule.passed,
            mw=result.gsk_rule.mw,
            logp=result.gsk_rule.logp,
            interpretation=result.gsk_rule.interpretation,
        )

    # Golden Triangle
    golden_triangle = None
    if result.golden_triangle:
        golden_triangle = GoldenTriangleSchema(
            in_golden_triangle=result.golden_triangle.in_golden_triangle,
            mw=result.golden_triangle.mw,
            logd=result.golden_triangle.logd,
            interpretation=result.golden_triangle.interpretation,
        )

    return ADMETResultSchema(
        synthetic_accessibility=synthetic_accessibility,
        solubility=solubility,
        complexity=complexity,
        cns_mpo=cns_mpo,
        bioavailability=bioavailability,
        pfizer_rule=pfizer_rule,
        gsk_rule=gsk_rule,
        golden_triangle=golden_triangle,
        molar_refractivity=result.molar_refractivity,
        interpretation=result.interpretation,
    )


def _calculate_aggregator(mol: Chem.Mol) -> AggregatorLikelihoodSchema:
    """Calculate aggregator likelihood and convert to schema."""
    result = calculate_aggregator_likelihood(mol)

    return AggregatorLikelihoodSchema(
        likelihood=result.likelihood,
        risk_score=result.risk_score,
        logp=result.logp,
        tpsa=result.tpsa,
        mw=result.mw,
        aromatic_rings=result.aromatic_rings,
        risk_factors=result.risk_factors,
        interpretation=result.interpretation,
    )


def _extract_molecule_info(mol: Chem.Mol, input_string: str) -> MoleculeInfoSchema:
    """
    Extract basic molecule information.

    Args:
        mol: RDKit molecule object
        input_string: Original input string

    Returns:
        MoleculeInfoSchema with basic properties
    """
    try:
        canonical = Chem.MolToSmiles(mol)
        formula = rdMolDescriptors.CalcMolFormula(mol)
        mw = Descriptors.MolWt(mol)
    except Exception:
        canonical = None
        formula = None
        mw = None

    return MoleculeInfoSchema(
        input_string=input_string,
        canonical_smiles=canonical,
        molecular_formula=formula,
        molecular_weight=mw,
    )
