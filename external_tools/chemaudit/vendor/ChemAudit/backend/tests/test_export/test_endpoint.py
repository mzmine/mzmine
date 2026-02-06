"""
API Tests for Export Endpoints

Tests export API endpoint functionality.
"""

import json
import os
from unittest.mock import MagicMock, patch

import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app

# Disable rate limiting for tests
os.environ["RATE_LIMIT_ENABLED"] = "false"


@pytest.fixture
def sample_results():
    """Sample batch results for testing."""
    return [
        {
            "smiles": "CCO",
            "name": "Ethanol",
            "index": 0,
            "status": "success",
            "validation": {
                "canonical_smiles": "CCO",
                "inchikey": "LFQSCWFLJHTTHZ-UHFFFAOYSA-N",
                "overall_score": 95,
                "issues": [],
            },
            "alerts": {
                "pains": {"matches": []},
                "brenk": {"matches": []},
            },
            "scoring": {
                "ml_readiness_score": 88,
                "np_likeness_score": -0.5,
            },
            "standardized_smiles": "CCO",
        },
    ]


@pytest.mark.asyncio
class TestExportEndpoint:
    """Test export API endpoint."""

    async def test_export_csv_success(self, sample_results):
        """Test successful CSV export."""
        # Mock progress_tracker and result_storage
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage:
            # Setup mocks
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )
            mock_storage.get_results.return_value = {
                "results": sample_results,
                "page": 1,
                "page_size": 50,
                "total_results": 1,
                "total_pages": 1,
            }

            # Make request
            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job/export",
                    params={"format": "csv"},
                )

            # Verify response
            assert response.status_code == 200
            assert response.headers["content-type"] == "text/csv; charset=utf-8"
            assert "attachment" in response.headers["content-disposition"]
            assert ".csv" in response.headers["content-disposition"]

            # Verify content
            content = response.text
            assert "Ethanol" in content
            assert "CCO" in content

    async def test_export_excel_success(self, sample_results):
        """Test successful Excel export."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage:
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )
            mock_storage.get_results.return_value = {
                "results": sample_results,
                "page": 1,
                "page_size": 50,
                "total_results": 1,
                "total_pages": 1,
            }

            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job/export",
                    params={"format": "excel"},
                )

            assert response.status_code == 200
            assert "spreadsheetml.sheet" in response.headers["content-type"]
            assert ".xlsx" in response.headers["content-disposition"]

    async def test_export_sdf_success(self, sample_results):
        """Test successful SDF export."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage:
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )
            mock_storage.get_results.return_value = {
                "results": sample_results,
                "page": 1,
                "page_size": 50,
                "total_results": 1,
                "total_pages": 1,
            }

            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job/export",
                    params={"format": "sdf"},
                )

            assert response.status_code == 200
            assert "chemical/x-mdl-sdfile" in response.headers["content-type"]
            assert ".sdf" in response.headers["content-disposition"]

    async def test_export_json_success(self, sample_results):
        """Test successful JSON export."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage:
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )
            mock_storage.get_results.return_value = {
                "results": sample_results,
                "page": 1,
                "page_size": 50,
                "total_results": 1,
                "total_pages": 1,
            }

            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job/export",
                    params={"format": "json"},
                )

            assert response.status_code == 200
            assert "application/json" in response.headers["content-type"]
            assert ".json" in response.headers["content-disposition"]

            # Verify JSON structure
            data = json.loads(response.text)
            assert "metadata" in data
            assert "results" in data

    async def test_export_job_not_found(self):
        """Test export with non-existent job ID."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress:
            mock_progress.get_progress.return_value = None

            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/nonexistent-job/export",
                    params={"format": "csv"},
                )

            assert response.status_code == 404
            assert "not found" in response.json()["detail"].lower()

    async def test_export_no_results(self):
        """Test export with no matching results."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage:
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )
            mock_storage.get_results.return_value = {
                "results": [],
                "page": 1,
                "page_size": 50,
                "total_results": 0,
                "total_pages": 0,
            }

            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job/export",
                    params={"format": "csv"},
                )

            assert response.status_code == 404
            assert "no results" in response.json()["detail"].lower()

    async def test_export_with_filters(self, sample_results):
        """Test export with score and status filters."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage:
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )
            mock_storage.get_results.return_value = {
                "results": sample_results,
                "page": 1,
                "page_size": 50,
                "total_results": 1,
                "total_pages": 1,
            }

            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job/export",
                    params={
                        "format": "csv",
                        "score_min": 80,
                        "score_max": 100,
                        "status": "success",
                    },
                )

            assert response.status_code == 200

            # Verify filters were passed to result_storage
            mock_storage.get_results.assert_called_once()
            call_kwargs = mock_storage.get_results.call_args.kwargs
            assert call_kwargs["min_score"] == 80
            assert call_kwargs["max_score"] == 100
            assert call_kwargs["status_filter"] == "success"

    async def test_export_invalid_format(self):
        """Test export with invalid format parameter."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress:
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )

            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job/export",
                    params={"format": "invalid"},
                )

            # FastAPI should return 422 for invalid enum value
            assert response.status_code == 422

    async def test_export_content_disposition_header(self, sample_results):
        """Test that Content-Disposition header is properly formatted."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage:
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job-12345678",
                status="complete",
            )
            mock_storage.get_results.return_value = {
                "results": sample_results,
                "page": 1,
                "page_size": 50,
                "total_results": 1,
                "total_pages": 1,
            }

            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job-12345678/export",
                    params={"format": "csv"},
                )

            assert response.status_code == 200

            # Verify filename format: batch_{job_id[:8]}_{timestamp}.csv
            disposition = response.headers["content-disposition"]
            assert 'attachment; filename="batch_test-job' in disposition
            assert ".csv" in disposition
