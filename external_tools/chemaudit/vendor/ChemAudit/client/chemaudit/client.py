"""
ChemAudit API Client

Synchronous Python client for the ChemAudit API.
"""

import time
from typing import Optional, List, Dict, Any, Iterator
from pathlib import Path
import httpx

from .models import (
    ValidationResult,
    AlertScreeningResult,
    ScoreResult,
    StandardizationResult,
    BatchUploadResponse,
    BatchJob,
    BatchResult,
    BatchStatistics,
    BatchResultItem,
    BatchJobStatus,
)
from .exceptions import (
    APIError,
    RateLimitError,
    AuthenticationError,
    ValidationError,
    BatchJobNotFoundError,
    TimeoutError,
)


class ChemAuditClient:
    """
    Synchronous client for the ChemAudit API.

    This client is intentionally synchronous-only using httpx.Client.
    For async usage, wrap calls with asyncio.to_thread() or use httpx directly.

    Args:
        base_url: API base URL (default: http://localhost:8000)
        api_key: Optional API key for authentication (increases rate limits)
        timeout: Request timeout in seconds (default: 30)
        max_retries: Maximum retries for rate limit errors (default: 3)

    Example:
        >>> client = ChemAuditClient(api_key="your-key")
        >>> result = client.validate("CCO")
        >>> print(result.overall_score)
    """

    def __init__(
        self,
        base_url: str = "http://localhost:8000",
        api_key: Optional[str] = None,
        timeout: float = 30.0,
        max_retries: int = 3,
    ):
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key
        self.timeout = timeout
        self.max_retries = max_retries

        # Build headers
        headers = {"Content-Type": "application/json"}
        if api_key:
            headers["X-API-Key"] = api_key

        # Create synchronous HTTP client
        self._client = httpx.Client(
            base_url=self.base_url,
            headers=headers,
            timeout=timeout,
        )

    def __enter__(self):
        """Context manager entry."""
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit - close client."""
        self.close()

    def close(self):
        """Close the HTTP client."""
        self._client.close()

    def _request(
        self,
        method: str,
        path: str,
        retry_count: int = 0,
        **kwargs,
    ) -> httpx.Response:
        """
        Make HTTP request with rate limit retry logic.

        Args:
            method: HTTP method (GET, POST, etc.)
            path: API path (e.g., "/api/v1/validate")
            retry_count: Current retry count (internal use)
            **kwargs: Additional arguments to pass to httpx.request

        Returns:
            HTTP response object

        Raises:
            RateLimitError: When rate limit exceeded and retries exhausted
            AuthenticationError: When API key is invalid
            APIError: For other API errors
        """
        try:
            response = self._client.request(method, path, **kwargs)

            # Handle rate limiting with retry
            if response.status_code == 429:
                retry_after = int(response.headers.get("Retry-After", 60))

                if retry_count < self.max_retries:
                    time.sleep(retry_after)
                    return self._request(
                        method, path, retry_count=retry_count + 1, **kwargs
                    )
                else:
                    raise RateLimitError("Rate limit exceeded", retry_after=retry_after)

            # Handle authentication errors
            if response.status_code == 401:
                raise AuthenticationError()

            # Handle not found errors
            if response.status_code == 404:
                error_data = response.json() if response.text else {}
                raise APIError(
                    error_data.get("detail", "Resource not found"),
                    status_code=404,
                    response_data=error_data,
                )

            # Handle validation errors
            if response.status_code == 422:
                error_data = response.json() if response.text else {}
                raise ValidationError(error_data.get("detail", "Validation error"))

            # Handle other client/server errors
            if response.status_code >= 400:
                error_data = response.json() if response.text else {}
                raise APIError(
                    error_data.get("detail", f"HTTP {response.status_code}"),
                    status_code=response.status_code,
                    response_data=error_data,
                )

            return response

        except httpx.TimeoutException as e:
            raise TimeoutError(f"Request timed out: {e}")
        except httpx.HTTPError as e:
            raise APIError(f"HTTP error: {e}")

    def validate(
        self,
        molecule: str,
        format: str = "auto",
        include_alerts: bool = False,
        include_scores: bool = False,
        standardize: bool = False,
    ) -> ValidationResult:
        """
        Validate a single molecule.

        Args:
            molecule: Molecule string (SMILES, InChI, or MOL block)
            format: Input format ("auto", "smiles", "inchi", "mol")
            include_alerts: Include structural alert screening
            include_scores: Include ML-readiness scoring
            standardize: Standardize molecule before validation

        Returns:
            ValidationResult with scores, issues, and molecule info

        Raises:
            ValidationError: If molecule is invalid
            APIError: For other API errors

        Example:
            >>> result = client.validate("CCO")
            >>> print(f"Score: {result.overall_score}")
        """
        response = self._request(
            "POST",
            "/api/v1/validate",
            json={
                "molecule": molecule,
                "format": format,
            },
            params={
                "include_alerts": include_alerts,
                "include_scores": include_scores,
                "standardize": standardize,
            },
        )
        return ValidationResult(**response.json())

    def screen_alerts(
        self,
        smiles: str,
        catalogs: Optional[List[str]] = None,
    ) -> AlertScreeningResult:
        """
        Screen molecule for structural alerts.

        Args:
            smiles: SMILES string
            catalogs: Alert catalogs to screen (default: ["PAINS", "BRENK"])

        Returns:
            AlertScreeningResult with detected alerts

        Example:
            >>> result = client.screen_alerts("c1ccccc1")
            >>> print(f"Alerts: {result.total_alerts}")
        """
        response = self._request(
            "POST",
            "/api/v1/alerts",
            json={
                "smiles": smiles,
                "catalogs": catalogs or ["PAINS", "BRENK"],
            },
        )
        return AlertScreeningResult(**response.json())

    def score(self, smiles: str) -> ScoreResult:
        """
        Calculate ML-readiness and other scores.

        Args:
            smiles: SMILES string

        Returns:
            ScoreResult with various scoring metrics

        Example:
            >>> result = client.score("CCO")
            >>> print(f"ML-readiness: {result.scores['ml_readiness_score']}")
        """
        response = self._request(
            "POST",
            "/api/v1/score",
            json={"smiles": smiles},
        )
        return ScoreResult(**response.json())

    def standardize(
        self,
        smiles: str,
        tautomer: bool = False,
    ) -> StandardizationResult:
        """
        Standardize a molecule using ChEMBL pipeline.

        Args:
            smiles: SMILES string
            tautomer: Apply tautomer canonicalization (may affect E/Z stereo)

        Returns:
            StandardizationResult with standardized SMILES and changes

        Example:
            >>> result = client.standardize("[Na+].CC(=O)[O-]")
            >>> print(result.standardized_smiles)
        """
        response = self._request(
            "POST",
            "/api/v1/standardize",
            json={
                "smiles": smiles,
                "apply_tautomer": tautomer,
            },
        )
        return StandardizationResult(**response.json())

    def submit_batch(
        self,
        file_path: str,
        smiles_column: str = "smiles",
    ) -> BatchUploadResponse:
        """
        Submit batch file for processing.

        Args:
            file_path: Path to CSV file with molecules
            smiles_column: Name of column containing SMILES

        Returns:
            BatchUploadResponse with job_id for tracking

        Example:
            >>> response = client.submit_batch("molecules.csv")
            >>> job_id = response.job_id
        """
        file_path = Path(file_path)
        if not file_path.exists():
            raise ValidationError(f"File not found: {file_path}")

        with open(file_path, "rb") as f:
            files = {"file": (file_path.name, f, "text/csv")}
            response = self._request(
                "POST",
                "/api/v1/batch/upload",
                files=files,
                data={"smiles_column": smiles_column},
            )

        return BatchUploadResponse(**response.json())

    def get_batch_status(self, job_id: str) -> BatchJob:
        """
        Get batch job status.

        Args:
            job_id: Batch job identifier

        Returns:
            BatchJob with status and progress

        Raises:
            BatchJobNotFoundError: If job not found

        Example:
            >>> status = client.get_batch_status(job_id)
            >>> print(f"Progress: {status.progress}%")
        """
        response = self._request("GET", f"/api/v1/batch/{job_id}")
        data = response.json()
        return BatchJob(**data)

    def get_batch_results(
        self,
        job_id: str,
        page: int = 1,
        page_size: int = 100,
        score_min: Optional[int] = None,
        score_max: Optional[int] = None,
        status: Optional[str] = None,
    ) -> BatchResult:
        """
        Get batch job results with pagination.

        Args:
            job_id: Batch job identifier
            page: Page number (1-indexed)
            page_size: Results per page (max 100)
            score_min: Filter by minimum score
            score_max: Filter by maximum score
            status: Filter by result status ("success" or "error")

        Returns:
            BatchResult with paginated results and statistics

        Example:
            >>> results = client.get_batch_results(job_id, page=1)
            >>> for item in results.results:
            ...     print(f"{item.smiles}: {item.validation['overall_score']}")
        """
        params = {
            "page": page,
            "page_size": page_size,
        }
        if score_min is not None:
            params["score_min"] = score_min
        if score_max is not None:
            params["score_max"] = score_max
        if status:
            params["status"] = status

        response = self._request("GET", f"/api/v1/batch/{job_id}", params=params)
        return BatchResult(**response.json())

    def get_batch_stats(self, job_id: str) -> BatchStatistics:
        """
        Get batch job statistics.

        Args:
            job_id: Batch job identifier

        Returns:
            BatchStatistics with aggregate metrics

        Example:
            >>> stats = client.get_batch_stats(job_id)
            >>> print(f"Average score: {stats.avg_validation_score}")
        """
        result = self.get_batch_results(job_id, page=1, page_size=1)
        if result.statistics is None:
            raise APIError("Statistics not available yet")
        return result.statistics

    def cancel_batch(self, job_id: str) -> Dict[str, str]:
        """
        Cancel a running batch job.

        Args:
            job_id: Batch job identifier

        Returns:
            Dict with cancellation confirmation

        Example:
            >>> client.cancel_batch(job_id)
        """
        response = self._request("DELETE", f"/api/v1/batch/{job_id}")
        return response.json()

    def wait_for_batch(
        self,
        job_id: str,
        poll_interval: float = 2.0,
        timeout: float = 3600.0,
    ) -> BatchJob:
        """
        Wait for batch job to complete (polling).

        Args:
            job_id: Batch job identifier
            poll_interval: Seconds between status checks
            timeout: Maximum wait time in seconds

        Returns:
            Final BatchJob status

        Raises:
            TimeoutError: If job doesn't complete within timeout
            APIError: If job fails

        Example:
            >>> job = client.wait_for_batch(job_id)
            >>> print(f"Completed: {job.processed}/{job.total}")
        """
        start_time = time.time()

        while True:
            status = self.get_batch_status(job_id)

            if status.status == BatchJobStatus.COMPLETE:
                return status
            elif status.status == BatchJobStatus.FAILED:
                raise APIError(
                    f"Batch job failed: {status.error_message}",
                    response_data={"job_id": job_id},
                )
            elif status.status == BatchJobStatus.CANCELLED:
                raise APIError(
                    "Batch job was cancelled",
                    response_data={"job_id": job_id},
                )

            # Check timeout
            elapsed = time.time() - start_time
            if elapsed > timeout:
                raise TimeoutError(
                    f"Batch job did not complete within {timeout} seconds"
                )

            time.sleep(poll_interval)

    def iter_batch_results(
        self,
        job_id: str,
        page_size: int = 100,
        **filter_kwargs,
    ) -> Iterator[BatchResultItem]:
        """
        Iterate through all batch results (handles pagination).

        Args:
            job_id: Batch job identifier
            page_size: Results per page (max 100)
            **filter_kwargs: Additional filters (score_min, score_max, status)

        Yields:
            BatchResultItem for each result

        Example:
            >>> for item in client.iter_batch_results(job_id):
            ...     if item.status == "success":
            ...         print(item.smiles, item.validation["overall_score"])
        """
        page = 1
        while True:
            result = self.get_batch_results(
                job_id, page=page, page_size=page_size, **filter_kwargs
            )

            for item in result.results:
                yield item

            if page >= result.total_pages:
                break

            page += 1

    def export_batch(
        self,
        job_id: str,
        format: str = "csv",
        output_path: Optional[str] = None,
        **filter_kwargs,
    ) -> str:
        """
        Export batch results to file.

        Args:
            job_id: Batch job identifier
            format: Export format ("csv", "excel", "sdf", "json")
            output_path: Output file path (default: auto-generated)
            **filter_kwargs: Filters (score_min, score_max, status)

        Returns:
            Path to exported file

        Example:
            >>> path = client.export_batch(job_id, format="excel")
            >>> print(f"Exported to: {path}")
        """
        params = {"format": format}
        params.update(filter_kwargs)

        response = self._request(
            "GET",
            f"/api/v1/batch/{job_id}/export",
            params=params,
        )

        # Generate output path if not provided
        if output_path is None:
            ext = {"csv": "csv", "excel": "xlsx", "sdf": "sdf", "json": "json"}
            output_path = f"batch_{job_id}.{ext.get(format, 'csv')}"

        # Write response to file
        with open(output_path, "wb") as f:
            f.write(response.content)

        return output_path
