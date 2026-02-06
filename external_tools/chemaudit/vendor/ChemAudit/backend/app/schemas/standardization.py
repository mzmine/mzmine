"""
Standardization Schemas

Pydantic schemas for standardization requests and responses.
"""

from typing import List, Optional

from pydantic import BaseModel, Field

from app.schemas.validation import MoleculeInfo


class StandardizationOptions(BaseModel):
    """Options for the standardization pipeline."""

    include_tautomer: bool = Field(
        default=False,
        description="Include tautomer canonicalization (WARNING: may lose E/Z stereochemistry)",
    )
    preserve_stereo: bool = Field(
        default=True,
        description="Attempt to preserve stereochemistry during standardization",
    )


class StandardizationStep(BaseModel):
    """A single step in the standardization pipeline."""

    step_name: str = Field(..., description="Name of the pipeline step")
    applied: bool = Field(..., description="Whether the step was successfully applied")
    description: str = Field(
        ..., description="Human-readable description of what this step does"
    )
    changes: str = Field(
        default="", description="Description of changes made by this step"
    )


class StereoComparison(BaseModel):
    """Comparison of stereochemistry before and after standardization."""

    before_count: int = Field(
        default=0, description="Number of defined stereocenters before"
    )
    after_count: int = Field(
        default=0, description="Number of defined stereocenters after"
    )
    lost: int = Field(default=0, description="Number of stereocenters lost")
    gained: int = Field(default=0, description="Number of stereocenters gained")
    double_bond_stereo_lost: int = Field(
        default=0, description="Number of E/Z stereo bonds lost"
    )
    warning: Optional[str] = Field(
        None, description="Warning message if stereochemistry was lost"
    )


class StructureComparisonSchema(BaseModel):
    """Comparison between original and standardized structures."""

    original_atom_count: int = Field(default=0)
    standardized_atom_count: int = Field(default=0)
    original_formula: Optional[str] = None
    standardized_formula: Optional[str] = None
    original_mw: Optional[float] = None
    standardized_mw: Optional[float] = None
    mass_change_percent: float = Field(default=0.0)
    is_identical: bool = Field(default=False)
    diff_summary: List[str] = Field(default_factory=list)


class CheckerIssue(BaseModel):
    """An issue found by the ChEMBL structure checker."""

    penalty_score: int = Field(
        ..., description="Penalty score for this issue (higher = worse)"
    )
    message: str = Field(..., description="Description of the issue")


class StandardizationResult(BaseModel):
    """Result of the standardization pipeline."""

    original_smiles: str = Field(..., description="Original input SMILES")
    standardized_smiles: Optional[str] = Field(None, description="Standardized SMILES")

    success: bool = Field(
        default=False, description="Whether standardization was successful"
    )
    error_message: Optional[str] = Field(None, description="Error message if failed")

    steps_applied: List[StandardizationStep] = Field(
        default_factory=list, description="List of pipeline steps and their results"
    )

    checker_issues: List[CheckerIssue] = Field(
        default_factory=list, description="Issues found by ChEMBL structure checker"
    )

    excluded_fragments: List[str] = Field(
        default_factory=list,
        description="SMILES of fragments removed (salts, solvents)",
    )

    stereo_comparison: Optional[StereoComparison] = Field(
        None, description="Stereochemistry comparison before/after"
    )

    structure_comparison: Optional[StructureComparisonSchema] = Field(
        None, description="Structure comparison before/after"
    )

    mass_change_percent: float = Field(
        default=0.0, description="Percentage change in molecular weight"
    )


class StandardizeRequest(BaseModel):
    """Request to standardize a molecule."""

    molecule: str = Field(
        ...,
        min_length=1,
        max_length=10000,
        description="Molecule string (SMILES, InChI, or MOL block)",
    )
    format: str = Field(
        default="auto", pattern="^(auto|smiles|inchi|mol)$", description="Input format"
    )
    options: StandardizationOptions = Field(
        default_factory=StandardizationOptions, description="Standardization options"
    )


class StandardizeResponse(BaseModel):
    """Response from standardization endpoint."""

    molecule_info: MoleculeInfo = Field(
        ..., description="Information about the original molecule"
    )
    result: StandardizationResult = Field(..., description="Standardization result")
    execution_time_ms: int = Field(..., description="Execution time in milliseconds")
