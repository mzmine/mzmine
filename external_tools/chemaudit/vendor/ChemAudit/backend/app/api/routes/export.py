"""
Export API Routes

Endpoints for exporting batch results in various formats.
"""

from datetime import datetime
from typing import List, Optional

from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from app.services.batch.progress_tracker import progress_tracker
from app.services.batch.result_aggregator import result_storage
from app.services.export.base import ExporterFactory, ExportFormat

router = APIRouter()

# Chunk size for streaming (1MB)
CHUNK_SIZE = 1024 * 1024


class ExportRequest(BaseModel):
    """Request body for POST export with molecule indices."""

    indices: List[int]


def _parse_indices(indices_str: Optional[str]) -> Optional[List[int]]:
    """
    Parse comma-separated indices string to list of integers.

    Args:
        indices_str: Comma-separated string like "0,1,5,23" or None

    Returns:
        List of integers or None if input is None or invalid
    """
    if not indices_str:
        return None

    try:
        indices_list = [
            int(i.strip()) for i in indices_str.split(",") if i.strip().isdigit()
        ]
        return indices_list if indices_list else None
    except ValueError:
        return None


def _export_results(
    job_id: str,
    format: ExportFormat,
    score_min: Optional[int],
    score_max: Optional[int],
    status: Optional[str],
    indices_list: Optional[List[int]],
) -> StreamingResponse:
    """
    Shared logic for exporting batch results with optional indices filtering.

    Args:
        job_id: Job identifier
        format: Export format
        score_min: Minimum validation score filter
        score_max: Maximum validation score filter
        status: Status filter
        indices_list: List of molecule indices to include, or None for all

    Returns:
        StreamingResponse with exported file
    """
    # Check if job exists
    progress_info = progress_tracker.get_progress(job_id)
    if not progress_info:
        raise HTTPException(status_code=404, detail=f"Job {job_id} not found")

    # Get results with filters
    result_data = result_storage.get_results(
        job_id=job_id,
        page=1,
        page_size=10000,  # Get all results (max batch size is 10K)
        status_filter=status,
        min_score=score_min,
        max_score=score_max,
    )

    results = result_data.get("results", [])

    # Apply indices filter if provided
    if indices_list is not None:
        indices_set = set(indices_list)
        results = [r for r in results if r.get("index") in indices_set]

    # Handle empty results
    if not results:
        raise HTTPException(
            status_code=404,
            detail="No results found matching the specified filters and indices",
        )

    # Create exporter
    try:
        exporter = ExporterFactory.create(format)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))

    # Export results
    try:
        export_buffer = exporter.export(results)
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to export results: {str(e)}",
        )

    # Generate filename
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"batch_{job_id[:8]}_{timestamp}.{exporter.file_extension}"

    # Return streaming response
    def iterfile():
        """Stream file in chunks."""
        export_buffer.seek(0)
        while chunk := export_buffer.read(CHUNK_SIZE):
            yield chunk

    return StreamingResponse(
        iterfile(),
        media_type=exporter.media_type,
        headers={
            "Content-Disposition": f'attachment; filename="{filename}"',
        },
    )


@router.get("/batch/{job_id}/export")
async def export_batch_results(
    job_id: str,
    format: ExportFormat = Query(..., description="Export format"),
    score_min: Optional[int] = Query(
        None, ge=0, le=100, description="Minimum validation score"
    ),
    score_max: Optional[int] = Query(
        None, ge=0, le=100, description="Maximum validation score"
    ),
    status: Optional[str] = Query(
        None, description="Filter by status (success, error, warning)"
    ),
    indices: Optional[str] = Query(
        None,
        description="Comma-separated molecule indices to export (e.g., '0,1,5,23'). If omitted, exports all.",
    ),
):
    """
    Export batch results to specified format.

    - **job_id**: Job identifier from upload response
    - **format**: Export format (csv, excel, sdf, json, pdf)
    - **score_min**: Filter results by minimum score
    - **score_max**: Filter results by maximum score
    - **status**: Filter results by status
    - **indices**: Comma-separated molecule indices to export (optional)

    Returns file download with appropriate Content-Disposition header.

    Supported formats:
    - CSV: Comma-separated values with all validation data
    - Excel: Formatted spreadsheet with conditional coloring and summary sheet
    - SDF: Structure-data file with properties attached to molecules
    - JSON: Full result objects with metadata
    - PDF: Professional report with charts, statistics, and molecule images

    When indices parameter is provided, only molecules with those indices
    (by their index field) are included in the export. Invalid or out-of-range
    indices are silently skipped.
    """
    # Parse indices
    indices_list = _parse_indices(indices)

    # Use shared export logic
    return _export_results(job_id, format, score_min, score_max, status, indices_list)


@router.post("/batch/{job_id}/export")
async def export_batch_results_post(
    job_id: str,
    body: ExportRequest,
    format: ExportFormat = Query(..., description="Export format"),
    score_min: Optional[int] = Query(
        None, ge=0, le=100, description="Minimum validation score"
    ),
    score_max: Optional[int] = Query(
        None, ge=0, le=100, description="Maximum validation score"
    ),
    status: Optional[str] = Query(
        None, description="Filter by status (success, error, warning)"
    ),
):
    """
    Export batch results to specified format (POST version for large selections).

    Use this endpoint when you need to export a large number of specific molecules
    that would exceed URL length limits with the GET endpoint.

    - **job_id**: Job identifier from upload response
    - **body**: JSON with `indices` array of molecule indices
    - **format**: Export format (csv, excel, sdf, json, pdf)
    - **score_min**: Filter results by minimum score
    - **score_max**: Filter results by maximum score
    - **status**: Filter results by status

    Returns file download with appropriate Content-Disposition header.

    The indices array in the request body specifies which molecules to export
    by their index field. Invalid or out-of-range indices are silently skipped.

    Example request body:
    ```json
    {
        "indices": [0, 1, 5, 23, 42]
    }
    ```
    """
    # Use shared export logic with indices from request body
    return _export_results(job_id, format, score_min, score_max, status, body.indices)
