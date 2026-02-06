"""
Standardization API Routes

Endpoints for molecule standardization using ChEMBL-compatible pipeline.
"""

import time
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Request

from app.api.routes.validation import extract_molecule_info
from app.core.rate_limit import get_rate_limit_key, limiter
from app.core.security import get_api_key
from app.schemas.standardization import (
    CheckerIssue,
    StandardizationResult,
    StandardizationStep,
    StandardizeRequest,
    StandardizeResponse,
    StereoComparison,
    StructureComparisonSchema,
)
from app.services.parser.molecule_parser import MoleculeFormat, parse_molecule
from app.services.standardization.chembl_pipeline import (
    StandardizationOptions,
    StandardizationPipeline,
)

router = APIRouter()

# Singleton pipeline instance
_pipeline = StandardizationPipeline()


@router.post("/standardize", response_model=StandardizeResponse)
@limiter.limit("10/minute", key_func=get_rate_limit_key)
async def standardize_molecule(
    request: Request,
    body: StandardizeRequest,
    api_key: Optional[str] = Depends(get_api_key),
):
    """
    Standardize a molecule using ChEMBL-compatible pipeline.

    The pipeline includes:
    1. **Checker**: Detect structural issues
    2. **Standardizer**: Fix common issues (nitro groups, metals, etc.)
    3. **GetParent**: Extract parent molecule, remove salts/solvents
    4. **Tautomer** (optional): Canonicalize tautomers (WARNING: may lose E/Z stereo)

    Args:
        body: StandardizeRequest with molecule and options

    Returns:
        StandardizeResponse with original and standardized structures

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

    # Extract molecule info
    mol_info = extract_molecule_info(mol, body.molecule)

    # Convert request options to internal options
    options = StandardizationOptions(
        include_tautomer=body.options.include_tautomer,
        preserve_stereo=body.options.preserve_stereo,
    )

    # Run standardization pipeline
    pipeline_result = _pipeline.standardize(mol, options)

    # Convert internal result to schema
    result = _convert_pipeline_result(pipeline_result)

    execution_time = int((time.time() - start_time) * 1000)

    return StandardizeResponse(
        molecule_info=mol_info, result=result, execution_time_ms=execution_time
    )


@router.get("/standardize/options")
async def get_standardization_options():
    """
    Get available standardization options with descriptions.

    Returns:
        Dictionary of options with descriptions and default values
    """
    return {
        "options": {
            "include_tautomer": {
                "type": "boolean",
                "default": False,
                "description": "Include tautomer canonicalization",
                "warning": "May lose E/Z double bond stereochemistry",
            },
            "preserve_stereo": {
                "type": "boolean",
                "default": True,
                "description": "Attempt to preserve stereochemistry during standardization",
            },
        },
        "pipeline_steps": [
            {
                "name": "checker",
                "description": "Detect structural issues before standardization",
                "always_run": True,
            },
            {
                "name": "standardizer",
                "description": "Fix common issues (nitro groups, metals, sulphoxides, etc.)",
                "always_run": True,
            },
            {
                "name": "get_parent",
                "description": "Extract parent molecule, remove salts and solvents",
                "always_run": True,
            },
            {
                "name": "tautomer_canonicalization",
                "description": "Canonicalize tautomers",
                "always_run": False,
                "requires_option": "include_tautomer",
            },
        ],
    }


def _convert_pipeline_result(pipeline_result) -> StandardizationResult:
    """
    Convert internal pipeline result to API schema.

    Args:
        pipeline_result: Internal StandardizationResult from chembl_pipeline

    Returns:
        StandardizationResult schema for API response
    """
    # Convert steps
    steps = [
        StandardizationStep(
            step_name=step.step_name,
            applied=step.applied,
            description=step.description,
            changes=step.changes,
        )
        for step in pipeline_result.steps_applied
    ]

    # Convert checker issues
    checker_issues = [
        CheckerIssue(penalty_score=score, message=msg)
        for score, msg in pipeline_result.checker_issues
    ]

    # Convert stereo comparison
    stereo_comparison = None
    if pipeline_result.stereo_comparison:
        sc = pipeline_result.stereo_comparison
        stereo_comparison = StereoComparison(
            before_count=sc.before.defined_stereocenters,
            after_count=sc.after.defined_stereocenters,
            lost=sc.stereocenters_lost,
            gained=sc.stereocenters_gained,
            double_bond_stereo_lost=sc.double_bond_stereo_lost,
            warning=sc.warning,
        )

    # Convert structure comparison
    structure_comparison = None
    if pipeline_result.structure_comparison:
        scomp = pipeline_result.structure_comparison
        structure_comparison = StructureComparisonSchema(
            original_atom_count=scomp.original_atom_count,
            standardized_atom_count=scomp.standardized_atom_count,
            original_formula=scomp.original_formula,
            standardized_formula=scomp.standardized_formula,
            original_mw=scomp.original_mw,
            standardized_mw=scomp.standardized_mw,
            mass_change_percent=scomp.mass_change_percent,
            is_identical=scomp.is_identical,
            diff_summary=scomp.diff_summary,
        )

    return StandardizationResult(
        original_smiles=pipeline_result.original_smiles,
        standardized_smiles=pipeline_result.standardized_smiles,
        success=pipeline_result.success,
        error_message=pipeline_result.error_message,
        steps_applied=steps,
        checker_issues=checker_issues,
        excluded_fragments=pipeline_result.excluded_fragments,
        stereo_comparison=stereo_comparison,
        structure_comparison=structure_comparison,
        mass_change_percent=pipeline_result.mass_change_percent,
    )
