"""
ChemAudit Python Client

Official Python client for the ChemAudit API.
"""

from .client import ChemAuditClient
from .models import (
    ValidationResult,
    AlertScreeningResult,
    ScoreResult,
    StandardizationResult,
    BatchUploadResponse,
    BatchJob,
    BatchResult,
    BatchResultItem,
    BatchStatistics,
    BatchJobStatus,
    CheckResult,
    AlertResult,
    MoleculeInfo,
    Severity,
)
from .exceptions import (
    ChemAuditError,
    APIError,
    RateLimitError,
    AuthenticationError,
    ValidationError,
    BatchJobNotFoundError,
    TimeoutError,
)

__version__ = "1.0.0"
__all__ = [
    "ChemAuditClient",
    "ValidationResult",
    "AlertScreeningResult",
    "ScoreResult",
    "StandardizationResult",
    "BatchUploadResponse",
    "BatchJob",
    "BatchResult",
    "BatchResultItem",
    "BatchStatistics",
    "BatchJobStatus",
    "CheckResult",
    "AlertResult",
    "MoleculeInfo",
    "Severity",
    "ChemAuditError",
    "APIError",
    "RateLimitError",
    "AuthenticationError",
    "ValidationError",
    "BatchJobNotFoundError",
    "TimeoutError",
]
