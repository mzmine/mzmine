"""
Batch Processing API Routes

Endpoints for batch file upload, job status, and results retrieval.
"""

import uuid
from typing import Optional

from fastapi import (
    APIRouter,
    Depends,
    File,
    Form,
    HTTPException,
    Query,
    Request,
    UploadFile,
)

from app.core.config import settings
from app.core.rate_limit import get_rate_limit_key, limiter
from app.core.security import get_api_key
from app.schemas.batch import (
    BatchJobStatus,
    BatchResultItem,
    BatchResultsResponse,
    BatchStatistics,
    BatchUploadResponse,
    CSVColumnsResponse,
)
from app.services.batch.file_parser import (
    detect_csv_columns,
    parse_csv,
    parse_sdf,
    validate_file_content_type,
)
from app.services.batch.progress_tracker import progress_tracker
from app.services.batch.result_aggregator import result_storage
from app.services.batch.tasks import cancel_batch_job, process_batch_job

router = APIRouter()


@router.post("/batch/upload", response_model=BatchUploadResponse)
@limiter.limit("10/minute", key_func=get_rate_limit_key)
async def upload_batch(
    request: Request,
    file: UploadFile = File(..., description="SDF or CSV file to process"),
    smiles_column: Optional[str] = Form(
        default="SMILES",
        description="Column name containing SMILES (for CSV files)",
    ),
    name_column: Optional[str] = Form(
        default=None,
        description="Column name containing molecule names/IDs (for CSV files)",
    ),
    include_extended_safety: bool = Form(
        default=False,
        description="Include NIH and ZINC structural alert filters",
    ),
    include_chembl_alerts: bool = Form(
        default=False,
        description="Include ChEMBL pharma company structural alert filters",
    ),
    include_standardization: bool = Form(
        default=False,
        description="Run ChEMBL standardization pipeline on each molecule",
    ),
    api_key: Optional[str] = Depends(get_api_key),
):
    """
    Upload a file for batch processing.

    Accepts SDF or CSV files with up to 1,000,000 molecules.
    Returns a job_id for tracking progress and retrieving results.

    - **file**: SDF (.sdf) or CSV (.csv) file
    - **smiles_column**: For CSV files, the column containing SMILES strings
    - **name_column**: For CSV files, the column containing molecule names/IDs

    Returns job_id immediately. Use `/batch/{job_id}` to check status
    and `/ws/batch/{job_id}` for real-time progress via WebSocket.
    """
    # Validate file extension
    filename = file.filename or ""
    filename_lower = filename.lower()

    # Supported text-based formats: .csv, .tsv, .txt (all parsed as delimited text)
    text_formats = (".csv", ".tsv", ".txt")
    if not (filename_lower.endswith(".sdf") or filename_lower.endswith(text_formats)):
        raise HTTPException(
            status_code=400,
            detail="Invalid file type. Supported formats: .sdf, .csv, .tsv, .txt",
        )

    # Read file content
    try:
        content = await file.read()
    except Exception:
        raise HTTPException(
            status_code=400,
            detail="Failed to read file",
        )

    # Security: Check file size
    file_size_mb = len(content) / (1024 * 1024)
    if file_size_mb > settings.MAX_FILE_SIZE_MB:
        raise HTTPException(
            status_code=400,
            detail=f"File too large: {file_size_mb:.1f}MB exceeds limit of {settings.MAX_FILE_SIZE_MB}MB",
        )

    # Security: Validate file content matches extension
    # .csv, .tsv, and .txt are all treated as delimited text files
    expected_type = "sdf" if filename_lower.endswith(".sdf") else "csv"
    is_valid, error_msg = validate_file_content_type(content, expected_type, filename)
    if not is_valid:
        raise HTTPException(
            status_code=400,
            detail=error_msg or "Invalid file content",
        )

    # Parse file
    try:
        if filename_lower.endswith(".sdf"):
            molecules = parse_sdf(content, max_file_size_mb=settings.MAX_FILE_SIZE_MB)
        else:  # .csv, .tsv, .txt - all parsed as delimited text
            molecules = parse_csv(
                content,
                smiles_column=smiles_column or "SMILES",
                name_column=name_column,
                max_file_size_mb=settings.MAX_FILE_SIZE_MB,
            )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception:
        raise HTTPException(
            status_code=400,
            detail="Failed to parse file. Please check the file format.",
        )

    # Validate molecule count
    if len(molecules) == 0:
        raise HTTPException(
            status_code=400,
            detail="No valid molecules found in file",
        )

    if len(molecules) > settings.MAX_BATCH_SIZE:
        raise HTTPException(
            status_code=400,
            detail=f"File contains {len(molecules)} molecules. "
            f"Maximum allowed is {settings.MAX_BATCH_SIZE}.",
        )

    # Generate job ID and start processing
    job_id = str(uuid.uuid4())

    # Convert MoleculeData objects to dicts for Celery serialization
    mol_dicts = [
        {
            "smiles": m.smiles,
            "name": m.name,
            "index": m.index,
            "properties": m.properties,
            "parse_error": m.parse_error,
        }
        for m in molecules
    ]

    # Build safety screening options
    safety_options = {
        "include_extended": include_extended_safety,
        "include_chembl": include_chembl_alerts,
        "include_standardization": include_standardization,
    }

    # Start batch processing
    process_batch_job(job_id, mol_dicts, safety_options=safety_options)

    return BatchUploadResponse(
        job_id=job_id,
        status="pending",
        total_molecules=len(molecules),
        message=f"Job submitted. Processing {len(molecules)} molecules.",
    )


@router.get("/batch/{job_id}", response_model=BatchResultsResponse)
@limiter.limit("60/minute", key_func=get_rate_limit_key)
async def get_batch_results(
    request: Request,
    job_id: str,
    page: int = Query(default=1, ge=1, description="Page number"),
    page_size: int = Query(default=50, ge=1, le=100, description="Results per page"),
    status_filter: Optional[str] = Query(
        default=None,
        description="Filter by status (success, error)",
    ),
    min_score: Optional[int] = Query(
        default=None, ge=0, le=100, description="Minimum validation score"
    ),
    max_score: Optional[int] = Query(
        default=None, ge=0, le=100, description="Maximum validation score"
    ),
    sort_by: Optional[str] = Query(
        default=None,
        description="Sort field (index, name, smiles, score, qed, safety, status, issues)",
    ),
    sort_dir: Optional[str] = Query(
        default=None,
        description="Sort direction (asc, desc)",
    ),
    api_key: Optional[str] = Depends(get_api_key),
):
    """
    Get batch job status and results.

    - **job_id**: Job identifier from upload response
    - **page**: Page number for results (1-indexed)
    - **page_size**: Results per page (max 100)
    - **status_filter**: Filter results by 'success' or 'error'
    - **min_score**: Filter by minimum validation score
    - **max_score**: Filter by maximum validation score

    Returns job status and paginated results with statistics.
    """
    # Get job status
    progress_info = progress_tracker.get_progress(job_id)

    if not progress_info:
        raise HTTPException(status_code=404, detail=f"Job {job_id} not found")

    # Get statistics
    stats_data = result_storage.get_statistics(job_id)
    statistics = None
    if stats_data:
        statistics = BatchStatistics(
            total=stats_data.total,
            successful=stats_data.successful,
            errors=stats_data.errors,
            avg_validation_score=stats_data.avg_validation_score,
            avg_ml_readiness_score=stats_data.avg_ml_readiness_score,
            avg_qed_score=stats_data.avg_qed_score,
            avg_sa_score=stats_data.avg_sa_score,
            lipinski_pass_rate=stats_data.lipinski_pass_rate,
            safety_pass_rate=stats_data.safety_pass_rate,
            score_distribution=stats_data.score_distribution,
            alert_summary=stats_data.alert_summary,
            issue_summary=stats_data.issue_summary,
            processing_time_seconds=stats_data.processing_time_seconds,
        )

    # Get paginated results
    result_data = result_storage.get_results(
        job_id=job_id,
        page=page,
        page_size=page_size,
        status_filter=status_filter,
        min_score=min_score,
        max_score=max_score,
        sort_by=sort_by,
        sort_dir=sort_dir,
    )

    # Convert to response model
    results = [
        BatchResultItem(
            smiles=r.get("smiles", ""),
            name=r.get("name"),
            index=r.get("index", 0),
            status=r.get("status", "error"),
            error=r.get("error"),
            validation=r.get("validation"),
            alerts=r.get("alerts"),
            scoring=r.get("scoring"),
            standardization=r.get("standardization"),
        )
        for r in result_data.get("results", [])
    ]

    return BatchResultsResponse(
        job_id=job_id,
        status=progress_info.status,
        statistics=statistics,
        results=results,
        page=result_data.get("page", page),
        page_size=result_data.get("page_size", page_size),
        total_results=result_data.get("total_results", 0),
        total_pages=result_data.get("total_pages", 0),
    )


@router.get("/batch/{job_id}/status", response_model=BatchJobStatus)
@limiter.limit("10/minute", key_func=get_rate_limit_key)
async def get_batch_status(
    request: Request, job_id: str, api_key: Optional[str] = Depends(get_api_key)
):
    """
    Get current status of a batch job.

    Returns progress information without results data.
    Lighter endpoint for polling status.
    """
    progress_info = progress_tracker.get_progress(job_id)

    if not progress_info:
        raise HTTPException(status_code=404, detail=f"Job {job_id} not found")

    return BatchJobStatus(
        job_id=progress_info.job_id,
        status=progress_info.status,
        progress=progress_info.progress,
        processed=progress_info.processed,
        total=progress_info.total,
        eta_seconds=progress_info.eta_seconds,
        error_message=progress_info.error_message,
    )


@router.get("/batch/{job_id}/stats", response_model=BatchStatistics)
@limiter.limit("10/minute", key_func=get_rate_limit_key)
async def get_batch_stats(
    request: Request, job_id: str, api_key: Optional[str] = Depends(get_api_key)
):
    """
    Get statistics for a completed batch job.

    Returns aggregate statistics without individual results.
    """
    stats_data = result_storage.get_statistics(job_id)

    if not stats_data:
        raise HTTPException(
            status_code=404,
            detail=f"Statistics not found for job {job_id}. Job may still be processing.",
        )

    return BatchStatistics(
        total=stats_data.total,
        successful=stats_data.successful,
        errors=stats_data.errors,
        avg_validation_score=stats_data.avg_validation_score,
        avg_ml_readiness_score=stats_data.avg_ml_readiness_score,
        avg_qed_score=stats_data.avg_qed_score,
        avg_sa_score=stats_data.avg_sa_score,
        lipinski_pass_rate=stats_data.lipinski_pass_rate,
        safety_pass_rate=stats_data.safety_pass_rate,
        score_distribution=stats_data.score_distribution,
        alert_summary=stats_data.alert_summary,
        issue_summary=stats_data.issue_summary,
        processing_time_seconds=stats_data.processing_time_seconds,
    )


@router.delete("/batch/{job_id}")
@limiter.limit("10/minute", key_func=get_rate_limit_key)
async def cancel_batch(
    request: Request, job_id: str, api_key: Optional[str] = Depends(get_api_key)
):
    """
    Cancel a batch job.

    Stops processing and marks job as cancelled.
    Already completed results are retained.
    """
    progress_info = progress_tracker.get_progress(job_id)

    if not progress_info:
        raise HTTPException(status_code=404, detail=f"Job {job_id} not found")

    if progress_info.status in ("complete", "cancelled", "failed"):
        return {
            "job_id": job_id,
            "status": progress_info.status,
            "message": f"Job already {progress_info.status}",
        }

    # Cancel the job
    cancel_batch_job.delay(job_id)

    return {
        "job_id": job_id,
        "status": "cancelling",
        "message": "Job cancellation requested",
    }


@router.post("/batch/detect-columns", response_model=CSVColumnsResponse)
@limiter.limit("10/minute", key_func=get_rate_limit_key)
async def detect_columns(
    request: Request,
    file: UploadFile = File(..., description="CSV file to analyze"),
    api_key: Optional[str] = Depends(get_api_key),
):
    """
    Detect columns in a delimited text file for SMILES and Name/ID selection.

    Returns list of column names, suggested columns, and sample values.
    Accepts .csv, .tsv, and .txt files.
    """
    filename = file.filename or ""
    text_formats = (".csv", ".tsv", ".txt")
    if not filename.lower().endswith(text_formats):
        raise HTTPException(
            status_code=400,
            detail="This endpoint only accepts delimited text files (.csv, .tsv, .txt)",
        )

    try:
        content = await file.read()

        # Security: Check file size before processing
        file_size_mb = len(content) / (1024 * 1024)
        if file_size_mb > settings.MAX_FILE_SIZE_MB:
            raise HTTPException(
                status_code=400,
                detail=f"File too large: {file_size_mb:.1f}MB exceeds limit",
            )

        result = detect_csv_columns(content, max_file_size_mb=settings.MAX_FILE_SIZE_MB)
        return CSVColumnsResponse(
            columns=result["columns"],
            suggested_smiles=result.get("suggested_smiles"),
            suggested_name=result.get("suggested_name"),
            column_samples=result.get("column_samples", {}),
            row_count_estimate=result.get("row_count_estimate", 0),
            file_size_mb=result.get("file_size_mb", 0),
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(
            status_code=400,
            detail=f"Failed to analyze CSV: {str(e)}",
        )
