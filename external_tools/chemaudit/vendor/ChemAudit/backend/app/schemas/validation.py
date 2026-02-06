"""
Validation Schemas

Pydantic schemas for validation requests and responses.
"""

from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field, field_validator

from app.schemas.common import Severity


class CheckResultSchema(BaseModel):
    """Schema for a single validation check result."""

    check_name: str
    passed: bool
    severity: Severity
    message: str
    affected_atoms: List[int] = Field(default_factory=list)
    details: Dict[str, Any] = Field(default_factory=dict)


class MoleculeInfo(BaseModel):
    """Schema for molecule information."""

    input_smiles: str
    canonical_smiles: Optional[str] = None
    inchi: Optional[str] = None
    inchikey: Optional[str] = None
    molecular_formula: Optional[str] = None
    molecular_weight: Optional[float] = None
    num_atoms: Optional[int] = None


class ValidationRequest(BaseModel):
    """Schema for validation request."""

    molecule: str = Field(
        ...,
        min_length=1,
        max_length=10000,
        description="Molecule string (SMILES, InChI, or MOL block)",
    )
    format: str = Field(
        default="auto", pattern="^(auto|smiles|inchi|mol)$", description="Input format"
    )
    checks: List[str] = Field(default=["all"], description="List of checks to run")
    preserve_aromatic: bool = Field(
        default=False,
        description="Preserve aromatic notation in canonical SMILES output",
    )

    @field_validator("molecule")
    @classmethod
    def sanitize_molecule_input(cls, v: str) -> str:
        """Sanitize molecule input to prevent injection attacks."""
        dangerous = ["<", ">", "&", ";", "|", "$", "`"]
        if any(c in v for c in dangerous):
            raise ValueError("Invalid characters in molecule string")
        return v.strip()


class ValidationResponse(BaseModel):
    """Schema for validation response."""

    status: str = "completed"
    molecule_info: MoleculeInfo
    overall_score: int = Field(
        ge=0, le=100, description="Overall validation score (0-100)"
    )
    issues: List[CheckResultSchema] = Field(
        default_factory=list, description="Failed checks"
    )
    all_checks: List[CheckResultSchema] = Field(
        default_factory=list, description="All check results"
    )
    execution_time_ms: int = Field(description="Execution time in milliseconds")
    cached: bool = Field(
        default=False, description="Whether result was served from cache"
    )
