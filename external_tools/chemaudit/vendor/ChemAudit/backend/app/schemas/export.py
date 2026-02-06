"""
Export Schemas

Pydantic models for export requests and metadata.
"""

from typing import Literal, Optional

from pydantic import BaseModel, Field

from app.services.export.base import ExportFormat


class ExportRequest(BaseModel):
    """Request parameters for exporting batch results."""

    job_id: str = Field(..., description="Batch job ID to export")
    format: ExportFormat = Field(..., description="Export format")
    score_min: Optional[int] = Field(
        None, ge=0, le=100, description="Minimum score filter"
    )
    score_max: Optional[int] = Field(
        None, ge=0, le=100, description="Maximum score filter"
    )
    status: Optional[Literal["success", "error", "warning"]] = Field(
        None, description="Filter by status"
    )


class ExportMetadata(BaseModel):
    """Metadata about an export operation."""

    format: ExportFormat
    filename: str
    size_bytes: int
    molecule_count: int
