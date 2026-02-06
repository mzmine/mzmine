"""
Common schemas shared across the application.

Includes severity levels, error responses, and health check responses.
"""

from enum import Enum
from typing import Any, Dict, Optional

from pydantic import BaseModel, Field


class Severity(str, Enum):
    """Severity level for validation results."""

    CRITICAL = "critical"
    ERROR = "error"
    WARNING = "warning"
    INFO = "info"
    PASS = "pass"


class ErrorResponse(BaseModel):
    """Standard error response format."""

    error: str = Field(..., description="Error message")
    details: Dict[str, Any] = Field(
        default_factory=dict, description="Additional error details"
    )


class HealthResponse(BaseModel):
    """Health check response with system information."""

    status: str = Field(..., description="Health status")
    app_name: str = Field(..., description="Application name")
    app_version: str = Field(..., description="Application version")
    rdkit_version: Optional[str] = Field(None, description="RDKit library version")
