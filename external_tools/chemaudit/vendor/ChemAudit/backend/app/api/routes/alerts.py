"""
Structural Alert Screening API Routes

Endpoints for screening molecules against PAINS, BRENK, and other
structural alert pattern catalogs.

IMPORTANT: Alerts are warnings for investigation, not automatic rejections.
87 FDA-approved drugs contain PAINS patterns.
"""

import time
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Request
from rdkit import Chem
from rdkit.Chem import rdMolDescriptors

from app.core.rate_limit import get_rate_limit_key, limiter
from app.core.security import get_api_key
from app.schemas.alerts import (
    AlertResultSchema,
    AlertScreenRequest,
    AlertScreenResponse,
    AlertSeverity,
    CatalogInfoSchema,
    CatalogListResponse,
    MoleculeInfoSchema,
)
from app.services.alerts.alert_manager import alert_manager
from app.services.alerts.filter_catalog import AVAILABLE_CATALOGS
from app.services.parser.molecule_parser import MoleculeFormat, parse_molecule

router = APIRouter()


@router.post("/alerts", response_model=AlertScreenResponse)
@limiter.limit("10/minute", key_func=get_rate_limit_key)
async def screen_alerts(
    request: Request,
    body: AlertScreenRequest,
    api_key: Optional[str] = Depends(get_api_key),
):
    """
    Screen a molecule for structural alerts.

    Screens the input molecule against specified pattern catalogs
    (default: PAINS) and returns matched alerts with atom indices
    for highlighting.

    Args:
        body: Alert screening request with molecule and catalog selection

    Returns:
        AlertScreenResponse with matched alerts and screening metadata

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

    # Screen for alerts
    screening_result = alert_manager.screen(mol, catalogs=body.catalogs)

    # Convert to schema
    alerts = [
        AlertResultSchema(
            pattern_name=alert.pattern_name,
            description=alert.description,
            severity=AlertSeverity(alert.severity.value),
            matched_atoms=alert.matched_atoms,
            catalog_source=alert.catalog_source,
            smarts=alert.smarts,
        )
        for alert in screening_result.alerts
    ]

    execution_time = int((time.time() - start_time) * 1000)

    return AlertScreenResponse(
        status="completed",
        molecule_info=mol_info,
        alerts=alerts,
        total_alerts=screening_result.total_alerts,
        screened_catalogs=screening_result.screened_catalogs,
        has_critical=screening_result.has_critical,
        has_warning=screening_result.has_warning,
        execution_time_ms=execution_time,
    )


@router.get("/alerts/catalogs", response_model=CatalogListResponse)
@limiter.limit("10/minute", key_func=get_rate_limit_key)
async def list_catalogs(
    request: Request, api_key: Optional[str] = Depends(get_api_key)
):
    """
    List available structural alert catalogs.

    Returns information about all available pattern catalogs
    including PAINS (A/B/C), BRENK, NIH, and ZINC.

    Returns:
        CatalogListResponse with available catalogs and their descriptions
    """
    catalogs = {}
    for cat_type, cat_info in AVAILABLE_CATALOGS.items():
        catalogs[cat_type] = CatalogInfoSchema(
            name=cat_info["name"],
            description=cat_info["description"],
            pattern_count=cat_info["pattern_count"],
            severity=cat_info["severity"],
            note=cat_info.get("note"),
        )

    return CatalogListResponse(
        catalogs=catalogs,
        default_catalogs=["PAINS"],
    )


@router.post("/alerts/quick-check")
@limiter.limit("10/minute", key_func=get_rate_limit_key)
async def quick_check_alerts(
    request: Request,
    body: AlertScreenRequest,
    api_key: Optional[str] = Depends(get_api_key),
):
    """
    Quick check if molecule has any structural alerts.

    Faster than full screening - only checks for presence of alerts,
    not specific patterns or atom indices.

    Args:
        body: Alert screening request with molecule and catalog selection

    Returns:
        Dictionary with has_alerts boolean and checked catalogs
    """
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
            },
        )

    has_alerts = alert_manager.has_alerts(parse_result.mol, catalogs=body.catalogs)

    return {
        "has_alerts": has_alerts,
        "checked_catalogs": body.catalogs,
    }


def extract_molecule_info(mol: Chem.Mol, input_string: str) -> MoleculeInfoSchema:
    """
    Extract molecule properties.

    Args:
        mol: RDKit molecule object
        input_string: Original input string

    Returns:
        MoleculeInfoSchema with molecular properties
    """
    try:
        canonical = Chem.MolToSmiles(mol)
        formula = rdMolDescriptors.CalcMolFormula(mol)
        num_atoms = mol.GetNumAtoms()
    except Exception:
        canonical = None
        formula = None
        num_atoms = None

    return MoleculeInfoSchema(
        input_string=input_string,
        canonical_smiles=canonical,
        molecular_formula=formula,
        num_atoms=num_atoms,
    )
