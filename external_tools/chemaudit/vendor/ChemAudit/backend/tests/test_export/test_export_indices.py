"""
Tests for Export Endpoint with Indices Filtering

Tests the indices filtering functionality for both GET and POST export endpoints.
"""

import os
from io import BytesIO
from unittest.mock import MagicMock, patch

import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app

# Disable rate limiting for tests
os.environ["RATE_LIMIT_ENABLED"] = "false"


@pytest.fixture
def sample_results():
    """Sample batch results with 5 molecules for testing."""
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
            "alerts": {"alert_count": 0},
            "scoring": {"ml_readiness": {"score": 88}},
        },
        {
            "smiles": "CC(C)O",
            "name": "Isopropanol",
            "index": 1,
            "status": "success",
            "validation": {
                "canonical_smiles": "CC(C)O",
                "inchikey": "KFZMGEQAYNKOFK-UHFFFAOYSA-N",
                "overall_score": 92,
                "issues": [],
            },
            "alerts": {"alert_count": 0},
            "scoring": {"ml_readiness": {"score": 85}},
        },
        {
            "smiles": "CC(=O)O",
            "name": "Acetic Acid",
            "index": 2,
            "status": "success",
            "validation": {
                "canonical_smiles": "CC(=O)O",
                "inchikey": "QTBSBXVTEAMEQO-UHFFFAOYSA-N",
                "overall_score": 88,
                "issues": [],
            },
            "alerts": {"alert_count": 0},
            "scoring": {"ml_readiness": {"score": 82}},
        },
        {
            "smiles": "C1CCCCC1",
            "name": "Cyclohexane",
            "index": 3,
            "status": "success",
            "validation": {
                "canonical_smiles": "C1CCCCC1",
                "inchikey": "XDTMQSROBMDMFD-UHFFFAOYSA-N",
                "overall_score": 75,
                "issues": [],
            },
            "alerts": {"alert_count": 0},
            "scoring": {"ml_readiness": {"score": 70}},
        },
        {
            "smiles": "c1ccccc1",
            "name": "Benzene",
            "index": 4,
            "status": "success",
            "validation": {
                "canonical_smiles": "c1ccccc1",
                "inchikey": "UHOVQNZJYSORNB-UHFFFAOYSA-N",
                "overall_score": 90,
                "issues": [],
            },
            "alerts": {"alert_count": 1},
            "scoring": {"ml_readiness": {"score": 88}},
        },
    ]


@pytest.mark.asyncio
class TestExportIndices:
    """Test export endpoint with indices filtering."""

    async def test_export_get_with_indices(self, sample_results):
        """Test GET endpoint with indices parameter - should filter to specific molecules."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage, patch(
            "app.api.routes.export.ExporterFactory"
        ) as mock_factory:
            # Setup mocks
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )
            mock_storage.get_results.return_value = {
                "results": sample_results,
                "page": 1,
                "page_size": 50,
                "total_results": 5,
                "total_pages": 1,
            }

            # Mock exporter to capture what gets passed to export()
            mock_exporter = MagicMock()
            mock_exporter.export.return_value = BytesIO(b"test,data\n")
            mock_exporter.media_type = "text/csv"
            mock_exporter.file_extension = "csv"
            mock_factory.create.return_value = mock_exporter

            # Make request with indices=0,2,4
            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job/export",
                    params={"format": "csv", "indices": "0,2,4"},
                )

            # Verify response
            assert response.status_code == 200

            # Verify that only 3 results were passed to exporter
            mock_exporter.export.assert_called_once()
            filtered_results = mock_exporter.export.call_args[0][0]
            assert len(filtered_results) == 3
            assert filtered_results[0]["index"] == 0
            assert filtered_results[1]["index"] == 2
            assert filtered_results[2]["index"] == 4

    async def test_export_get_without_indices(self, sample_results):
        """Test GET endpoint without indices - should export all results (backward compat)."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage, patch(
            "app.api.routes.export.ExporterFactory"
        ) as mock_factory:
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )
            mock_storage.get_results.return_value = {
                "results": sample_results,
                "page": 1,
                "page_size": 50,
                "total_results": 5,
                "total_pages": 1,
            }

            mock_exporter = MagicMock()
            mock_exporter.export.return_value = BytesIO(b"test,data\n")
            mock_exporter.media_type = "text/csv"
            mock_exporter.file_extension = "csv"
            mock_factory.create.return_value = mock_exporter

            # Make request without indices parameter
            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job/export",
                    params={"format": "csv"},
                )

            # Verify response
            assert response.status_code == 200

            # Verify all 5 results were passed to exporter
            mock_exporter.export.assert_called_once()
            filtered_results = mock_exporter.export.call_args[0][0]
            assert len(filtered_results) == 5

    async def test_export_post_with_indices(self, sample_results):
        """Test POST endpoint with indices in request body."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage, patch(
            "app.api.routes.export.ExporterFactory"
        ) as mock_factory:
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )
            mock_storage.get_results.return_value = {
                "results": sample_results,
                "page": 1,
                "page_size": 50,
                "total_results": 5,
                "total_pages": 1,
            }

            mock_exporter = MagicMock()
            mock_exporter.export.return_value = BytesIO(b"test,data\n")
            mock_exporter.media_type = "text/csv"
            mock_exporter.file_extension = "csv"
            mock_factory.create.return_value = mock_exporter

            # Make POST request with indices in body
            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.post(
                    "/api/v1/batch/test-job/export",
                    params={"format": "csv"},
                    json={"indices": [1, 3]},
                )

            # Verify response
            assert response.status_code == 200

            # Verify only 2 results were passed to exporter
            mock_exporter.export.assert_called_once()
            filtered_results = mock_exporter.export.call_args[0][0]
            assert len(filtered_results) == 2
            assert filtered_results[0]["index"] == 1
            assert filtered_results[1]["index"] == 3

    async def test_export_get_with_invalid_indices(self, sample_results):
        """Test GET with invalid indices - should skip invalid values gracefully."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage, patch(
            "app.api.routes.export.ExporterFactory"
        ) as mock_factory:
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )
            mock_storage.get_results.return_value = {
                "results": sample_results,
                "page": 1,
                "page_size": 50,
                "total_results": 5,
                "total_pages": 1,
            }

            mock_exporter = MagicMock()
            mock_exporter.export.return_value = BytesIO(b"test,data\n")
            mock_exporter.media_type = "text/csv"
            mock_exporter.file_extension = "csv"
            mock_factory.create.return_value = mock_exporter

            # Make request with mixed valid/invalid indices
            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job/export",
                    params={"format": "csv", "indices": "0,abc,2,xyz"},
                )

            # Verify response - should succeed with only valid indices
            assert response.status_code == 200

            # Verify only valid indices (0 and 2) were used
            mock_exporter.export.assert_called_once()
            filtered_results = mock_exporter.export.call_args[0][0]
            assert len(filtered_results) == 2
            assert filtered_results[0]["index"] == 0
            assert filtered_results[1]["index"] == 2

    async def test_export_get_with_out_of_range_indices(self, sample_results):
        """Test GET with out-of-range indices - should skip non-existent indices."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage, patch(
            "app.api.routes.export.ExporterFactory"
        ) as mock_factory:
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )
            mock_storage.get_results.return_value = {
                "results": sample_results,
                "page": 1,
                "page_size": 50,
                "total_results": 5,
                "total_pages": 1,
            }

            mock_exporter = MagicMock()
            mock_exporter.export.return_value = BytesIO(b"test,data\n")
            mock_exporter.media_type = "text/csv"
            mock_exporter.file_extension = "csv"
            mock_factory.create.return_value = mock_exporter

            # Make request with index 0 (exists) and 999 (doesn't exist)
            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job/export",
                    params={"format": "csv", "indices": "0,999"},
                )

            # Verify response - should succeed with only existing index
            assert response.status_code == 200

            # Verify only index 0 was exported
            mock_exporter.export.assert_called_once()
            filtered_results = mock_exporter.export.call_args[0][0]
            assert len(filtered_results) == 1
            assert filtered_results[0]["index"] == 0

    async def test_export_get_empty_indices_returns_404(self, sample_results):
        """Test GET with indices that match no results - should return 404."""
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
                "total_results": 5,
                "total_pages": 1,
            }

            # Make request with index that doesn't exist in results
            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.get(
                    "/api/v1/batch/test-job/export",
                    params={"format": "csv", "indices": "999"},
                )

            # Verify 404 response
            assert response.status_code == 404
            assert "no results" in response.json()["detail"].lower()

    async def test_export_post_with_indices_and_filters(self, sample_results):
        """Test POST with both indices and score filter - both filters should apply."""
        with patch("app.api.routes.export.progress_tracker") as mock_progress, patch(
            "app.api.routes.export.result_storage"
        ) as mock_storage, patch(
            "app.api.routes.export.ExporterFactory"
        ) as mock_factory:
            mock_progress.get_progress.return_value = MagicMock(
                job_id="test-job",
                status="complete",
            )

            # Return only results that pass the score filter (score >= 90)
            # Indices 0 (95), 1 (92), 4 (90) would pass the score filter
            filtered_by_score = [
                sample_results[0],
                sample_results[1],
                sample_results[4],
            ]
            mock_storage.get_results.return_value = {
                "results": filtered_by_score,
                "page": 1,
                "page_size": 50,
                "total_results": 3,
                "total_pages": 1,
            }

            mock_exporter = MagicMock()
            mock_exporter.export.return_value = BytesIO(b"test,data\n")
            mock_exporter.media_type = "text/csv"
            mock_exporter.file_extension = "csv"
            mock_factory.create.return_value = mock_exporter

            # Make POST request with indices [0, 4] and score_min=90
            # Should only get indices 0 and 4 (both pass score filter)
            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as client:
                response = await client.post(
                    "/api/v1/batch/test-job/export",
                    params={"format": "csv", "score_min": 90},
                    json={"indices": [0, 4]},
                )

            # Verify response
            assert response.status_code == 200

            # Verify result_storage was called with score_min filter
            mock_storage.get_results.assert_called_once()
            call_kwargs = mock_storage.get_results.call_args.kwargs
            assert call_kwargs["min_score"] == 90

            # Verify only 2 results were passed to exporter (intersection of filters)
            mock_exporter.export.assert_called_once()
            filtered_results = mock_exporter.export.call_args[0][0]
            assert len(filtered_results) == 2
            assert filtered_results[0]["index"] == 0
            assert filtered_results[1]["index"] == 4
