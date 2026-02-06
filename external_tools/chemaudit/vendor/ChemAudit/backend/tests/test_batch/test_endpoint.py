"""
Tests for batch API endpoints.

These tests use mocked Celery tasks to avoid requiring Redis.
"""

from unittest.mock import MagicMock, patch

import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app


@pytest.fixture
def mock_celery():
    """Mock Celery task execution."""
    with patch("app.services.batch.tasks.process_batch_job") as mock:
        mock.return_value = "test-job-id"
        yield mock


@pytest.fixture
def mock_progress_tracker():
    """Mock progress tracker."""
    with patch("app.api.routes.batch.progress_tracker") as mock:
        yield mock


@pytest.fixture
def mock_result_storage():
    """Mock result storage."""
    with patch("app.api.routes.batch.result_storage") as mock:
        yield mock


class TestBatchUpload:
    """Tests for POST /api/v1/batch/upload endpoint."""

    @pytest.fixture
    def sample_csv_content(self):
        """Simple CSV with valid molecules."""
        return b"SMILES,Name\nCCO,Ethanol\nC,Methane\nCC,Ethane\n"

    @pytest.fixture
    def sample_sdf_content(self):
        """Simple SDF with one molecule."""
        return b"""
     RDKit          3D

  2  1  0  0  0  0  0  0  0  0999 V2000
    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
    1.5000    0.0000    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0
  1  2  1  0
M  END
> <_Name>
Methanol

$$$$
"""

    async def test_upload_csv_returns_job_id(self, sample_csv_content, mock_celery):
        """Test that CSV upload returns a job_id."""
        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/api/v1/batch/upload",
                files={"file": ("test.csv", sample_csv_content, "text/csv")},
            )

        assert response.status_code == 200
        data = response.json()
        assert "job_id" in data
        assert data["status"] == "pending"
        assert data["total_molecules"] == 3

    async def test_upload_sdf_returns_job_id(self, sample_sdf_content, mock_celery):
        """Test that SDF upload returns a job_id."""
        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/api/v1/batch/upload",
                files={
                    "file": ("test.sdf", sample_sdf_content, "chemical/x-mdl-sdfile")
                },
            )

        assert response.status_code == 200
        data = response.json()
        assert "job_id" in data
        assert data["total_molecules"] == 1

    async def test_upload_invalid_extension_rejected(self):
        """Test that unsupported file types are rejected."""
        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/api/v1/batch/upload",
                files={
                    "file": ("test.xlsx", b"some content", "application/vnd.ms-excel")
                },
            )

        assert response.status_code == 400
        assert "Invalid file type" in response.json()["detail"]

    async def test_upload_tsv_returns_job_id(self, sample_csv_content, mock_celery):
        """Test that TSV upload returns a job_id."""
        # TSV content uses tab separators
        tsv_content = b"SMILES\tName\nCCO\tEthanol\nC\tMethane\nCC\tEthane\n"
        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/api/v1/batch/upload",
                files={"file": ("test.tsv", tsv_content, "text/tab-separated-values")},
            )

        assert response.status_code == 200
        data = response.json()
        assert "job_id" in data
        assert data["status"] == "pending"
        assert data["total_molecules"] == 3

    async def test_upload_txt_returns_job_id(self, sample_csv_content, mock_celery):
        """Test that TXT upload with delimited content returns a job_id."""
        # TXT file with comma-separated content
        txt_content = b"SMILES,Name\nCCO,Ethanol\nC,Methane\n"
        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/api/v1/batch/upload",
                files={"file": ("test.txt", txt_content, "text/plain")},
            )

        assert response.status_code == 200
        data = response.json()
        assert "job_id" in data
        assert data["status"] == "pending"
        assert data["total_molecules"] == 2

    async def test_upload_csv_with_missing_smiles_column(self):
        """Test error when SMILES column not found."""
        csv_content = b"Name,MW\nEthanol,46\n"

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/api/v1/batch/upload",
                files={"file": ("test.csv", csv_content, "text/csv")},
                data={"smiles_column": "SMILES"},
            )

        assert response.status_code == 400
        assert "not found" in response.json()["detail"]

    async def test_upload_empty_file_rejected(self, mock_celery):
        """Test that empty files are rejected."""
        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/api/v1/batch/upload",
                files={"file": ("test.csv", b"SMILES,Name\n", "text/csv")},
            )

        assert response.status_code == 400
        assert "No valid molecules" in response.json()["detail"]


class TestBatchStatus:
    """Tests for GET /api/v1/batch/{job_id}/status endpoint."""

    async def test_get_status_returns_progress(self, mock_progress_tracker):
        """Test getting job status."""
        # Setup mock
        mock_progress = MagicMock()
        mock_progress.job_id = "test-job"
        mock_progress.status = "processing"
        mock_progress.progress = 50
        mock_progress.processed = 50
        mock_progress.total = 100
        mock_progress.eta_seconds = 30
        mock_progress.error_message = None
        mock_progress_tracker.get_progress.return_value = mock_progress

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.get("/api/v1/batch/test-job/status")

        assert response.status_code == 200
        data = response.json()
        assert data["job_id"] == "test-job"
        assert data["status"] == "processing"
        assert data["progress"] == 50
        assert data["processed"] == 50
        assert data["total"] == 100

    async def test_get_status_not_found(self, mock_progress_tracker):
        """Test 404 for unknown job."""
        mock_progress_tracker.get_progress.return_value = None

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.get("/api/v1/batch/unknown-job/status")

        assert response.status_code == 404


class TestBatchResults:
    """Tests for GET /api/v1/batch/{job_id} endpoint."""

    async def test_get_results_with_pagination(
        self, mock_progress_tracker, mock_result_storage
    ):
        """Test getting paginated results."""
        # Setup mocks
        mock_progress = MagicMock()
        mock_progress.status = "complete"
        mock_progress_tracker.get_progress.return_value = mock_progress

        mock_stats = MagicMock()
        mock_stats.total = 3
        mock_stats.successful = 2
        mock_stats.errors = 1
        mock_stats.avg_validation_score = 85.0
        mock_stats.avg_ml_readiness_score = 80.0
        mock_stats.avg_qed_score = 0.75
        mock_stats.avg_sa_score = 3.2
        mock_stats.lipinski_pass_rate = 100.0
        mock_stats.safety_pass_rate = 90.0
        mock_stats.score_distribution = {"excellent": 1, "good": 1}
        mock_stats.alert_summary = {}
        mock_stats.issue_summary = {}
        mock_stats.processing_time_seconds = 2.5
        mock_result_storage.get_statistics.return_value = mock_stats

        mock_result_storage.get_results.return_value = {
            "results": [
                {
                    "smiles": "CCO",
                    "name": "Ethanol",
                    "index": 0,
                    "status": "success",
                    "validation": {"overall_score": 90},
                    "alerts": {},
                    "scoring": {},
                }
            ],
            "page": 1,
            "page_size": 50,
            "total_results": 3,
            "total_pages": 1,
        }

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.get("/api/v1/batch/test-job?page=1&page_size=50")

        assert response.status_code == 200
        data = response.json()
        assert data["job_id"] == "test-job"
        assert data["status"] == "complete"
        assert data["statistics"]["total"] == 3
        assert len(data["results"]) == 1


class TestDetectColumns:
    """Tests for POST /api/v1/batch/detect-columns endpoint."""

    async def test_detect_columns_returns_list(self):
        """Test column detection."""
        csv_content = b"SMILES,Name,MW,LogP\nCCO,Ethanol,46,0.5\n"

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/api/v1/batch/detect-columns",
                files={"file": ("test.csv", csv_content, "text/csv")},
            )

        assert response.status_code == 200
        data = response.json()
        assert "SMILES" in data["columns"]
        assert data["suggested_smiles"] == "SMILES"

    async def test_detect_columns_rejects_non_text_files(self):
        """Test that non-delimited text files are rejected."""
        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/api/v1/batch/detect-columns",
                files={"file": ("test.sdf", b"content", "application/octet-stream")},
            )

        assert response.status_code == 400

    async def test_detect_columns_accepts_tsv(self):
        """Test column detection for TSV files."""
        tsv_content = b"SMILES\tName\tMW\nCCO\tEthanol\t46\n"

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/api/v1/batch/detect-columns",
                files={"file": ("test.tsv", tsv_content, "text/tab-separated-values")},
            )

        assert response.status_code == 200
        data = response.json()
        assert "SMILES" in data["columns"]
        assert data["suggested_smiles"] == "SMILES"

    async def test_detect_columns_accepts_txt(self):
        """Test column detection for TXT files."""
        txt_content = b"SMILES,Name,MW\nCCO,Ethanol,46\n"

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            response = await client.post(
                "/api/v1/batch/detect-columns",
                files={"file": ("test.txt", txt_content, "text/plain")},
            )

        assert response.status_code == 200
        data = response.json()
        assert "SMILES" in data["columns"]
        assert data["suggested_smiles"] == "SMILES"
