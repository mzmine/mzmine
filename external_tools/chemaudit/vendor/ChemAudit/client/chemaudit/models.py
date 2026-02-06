"""
ChemAudit Client Models

Pydantic models for API requests and responses.
"""

from typing import List, Optional, Dict, Any, Literal
from pydantic import BaseModel, Field
from enum import Enum


class Severity(str, Enum):
    """Severity level for validation issues."""

    CRITICAL = "CRITICAL"
    ERROR = "ERROR"
    WARNING = "WARNING"
    INFO = "INFO"


class CheckResult(BaseModel):
    """Result from a single validation check."""

    check_name: str
    passed: bool
    severity: Severity
    message: str
    affected_atoms: List[int] = Field(default_factory=list)
    details: Dict[str, Any] = Field(default_factory=dict)


class MoleculeInfo(BaseModel):
    """Molecule information."""

    input_smiles: str
    canonical_smiles: Optional[str] = None
    inchi: Optional[str] = None
    inchikey: Optional[str] = None
    molecular_formula: Optional[str] = None
    molecular_weight: Optional[float] = None
    num_atoms: Optional[int] = None


class ValidationResult(BaseModel):
    """Complete validation result for a molecule."""

    status: str = "completed"
    molecule_info: MoleculeInfo
    overall_score: int = Field(ge=0, le=100)
    issues: List[CheckResult] = Field(default_factory=list)
    all_checks: List[CheckResult] = Field(default_factory=list)
    execution_time_ms: int


class AlertResult(BaseModel):
    """Structural alert screening result."""

    catalog: str
    smarts_pattern: str
    description: str
    severity: str
    matches: List[List[int]] = Field(default_factory=list)


class AlertScreeningResult(BaseModel):
    """Complete alert screening result."""

    status: str = "completed"
    smiles: str
    catalogs_screened: List[str]
    total_alerts: int
    alerts: List[AlertResult] = Field(default_factory=list)
    execution_time_ms: int
    educational_note: Optional[str] = None


class ScoreResult(BaseModel):
    """ML-readiness and other scoring results."""

    status: str = "completed"
    smiles: str
    scores: Dict[str, Any]
    execution_time_ms: int


class StandardizationResult(BaseModel):
    """Molecule standardization result."""

    status: str = "completed"
    input_smiles: str
    standardized_smiles: str
    changes_made: List[str] = Field(default_factory=list)
    warnings: List[str] = Field(default_factory=list)
    stereochemistry_preserved: bool
    execution_time_ms: int


class BatchJobStatus(str, Enum):
    """Batch job status."""

    PENDING = "pending"
    PROCESSING = "processing"
    COMPLETE = "complete"
    FAILED = "failed"
    CANCELLED = "cancelled"


class BatchJob(BaseModel):
    """Batch job status information."""

    job_id: str
    status: BatchJobStatus
    progress: int = Field(ge=0, le=100)
    processed: int
    total: int
    eta_seconds: Optional[int] = None
    error_message: Optional[str] = None


class BatchResultItem(BaseModel):
    """Single molecule result in batch."""

    smiles: str
    name: Optional[str] = None
    index: int
    status: Literal["success", "error"]
    error: Optional[str] = None
    validation: Optional[Dict[str, Any]] = None
    alerts: Optional[Dict[str, Any]] = None
    scoring: Optional[Dict[str, Any]] = None


class BatchStatistics(BaseModel):
    """Aggregate statistics for batch job."""

    total: int
    successful: int
    errors: int
    avg_validation_score: Optional[float] = None
    avg_ml_readiness_score: Optional[float] = None
    score_distribution: Dict[str, int] = Field(default_factory=dict)
    alert_summary: Dict[str, int] = Field(default_factory=dict)
    processing_time_seconds: Optional[float] = None


class BatchResult(BaseModel):
    """Batch processing results with pagination."""

    job_id: str
    status: BatchJobStatus
    statistics: Optional[BatchStatistics] = None
    results: List[BatchResultItem] = Field(default_factory=list)
    page: int = Field(ge=1)
    page_size: int = Field(ge=1, le=100)
    total_results: int
    total_pages: int


class BatchUploadResponse(BaseModel):
    """Response from batch upload."""

    job_id: str
    status: str = "pending"
    total_molecules: int
    message: str = "Job submitted successfully"
