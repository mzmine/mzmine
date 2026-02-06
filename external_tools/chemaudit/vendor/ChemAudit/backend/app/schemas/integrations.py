"""
Schemas for external integration requests and responses.

Covers COCONUT, PubChem, and ChEMBL integrations.
"""

from typing import List, Optional

from pydantic import BaseModel, Field


# COCONUT Natural Products Database
class COCONUTRequest(BaseModel):
    """Request for COCONUT lookup."""

    smiles: Optional[str] = None
    inchikey: Optional[str] = None


class COCONUTResult(BaseModel):
    """COCONUT natural product result."""

    found: bool
    coconut_id: Optional[str] = None
    name: Optional[str] = None
    smiles: Optional[str] = None
    inchikey: Optional[str] = None
    molecular_formula: Optional[str] = None
    molecular_weight: Optional[float] = None
    organism: Optional[str] = None
    organism_type: Optional[str] = None
    nplikeness: Optional[float] = None
    url: Optional[str] = None


# PubChem Cross-Reference
class PubChemRequest(BaseModel):
    """Request for PubChem lookup."""

    smiles: Optional[str] = None
    inchikey: Optional[str] = None


class PubChemResult(BaseModel):
    """PubChem compound result."""

    found: bool
    cid: Optional[int] = None
    iupac_name: Optional[str] = None
    molecular_formula: Optional[str] = None
    molecular_weight: Optional[float] = None
    canonical_smiles: Optional[str] = None
    inchi: Optional[str] = None
    inchikey: Optional[str] = None
    synonyms: Optional[List[str]] = None
    url: Optional[str] = None


# ChEMBL Bioactivity Data
class ChEMBLRequest(BaseModel):
    """Request for ChEMBL bioactivity lookup."""

    smiles: Optional[str] = None
    inchikey: Optional[str] = None


class BioactivityData(BaseModel):
    """ChEMBL bioactivity record."""

    target_chembl_id: str
    target_name: Optional[str] = None
    target_type: Optional[str] = None
    activity_type: str
    activity_value: Optional[float] = None
    activity_unit: Optional[str] = None
    assay_chembl_id: str
    document_chembl_id: Optional[str] = None


class ChEMBLResult(BaseModel):
    """ChEMBL molecule and bioactivity result."""

    found: bool
    chembl_id: Optional[str] = None
    pref_name: Optional[str] = None
    molecule_type: Optional[str] = None
    max_phase: Optional[int] = None
    molecular_formula: Optional[str] = None
    molecular_weight: Optional[float] = None
    bioactivities: List[BioactivityData] = Field(default_factory=list)
    bioactivity_count: int = 0
    url: Optional[str] = None
