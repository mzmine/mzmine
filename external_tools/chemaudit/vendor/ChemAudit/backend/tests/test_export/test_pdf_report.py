"""
Tests for PDF Report Generator

Tests PDF generation, chart rendering, molecule images, and API endpoint.
"""

import base64
from io import BytesIO

import pytest
from PyPDF2 import PdfReader

from app.services.export.base import ExporterFactory, ExportFormat
from app.services.export.pdf_report import PDFReportGenerator

# Check if weasyprint is available (requires system libraries)
try:
    import weasyprint  # noqa: F401

    WEASYPRINT_AVAILABLE = True
except (ImportError, OSError):
    WEASYPRINT_AVAILABLE = False

# Decorator to skip tests that require weasyprint
requires_weasyprint = pytest.mark.skipif(
    not WEASYPRINT_AVAILABLE,
    reason="WeasyPrint requires system libraries (pango, gobject). Install with: brew install pango",
)

# Sample test data
SAMPLE_RESULTS = [
    {
        "index": 0,
        "smiles": "CCO",
        "name": "Ethanol",
        "status": "success",
        "validation": {
            "overall_score": 95,
            "issues": [
                {
                    "check_name": "parsability",
                    "passed": True,
                    "severity": "INFO",
                    "message": "Valid",
                }
            ],
        },
        "alerts": {"has_alerts": False, "alert_count": 0, "alerts": []},
        "scoring": {"ml_readiness": {"score": 88, "interpretation": "Good"}},
    },
    {
        "index": 1,
        "smiles": "c1ccccc1",
        "name": "Benzene",
        "status": "success",
        "validation": {
            "overall_score": 75,
            "issues": [
                {
                    "check_name": "valence",
                    "passed": False,
                    "severity": "WARNING",
                    "message": "Aromatic ring",
                }
            ],
        },
        "alerts": {
            "has_alerts": True,
            "alert_count": 1,
            "alerts": [
                {"catalog": "PAINS", "rule_name": "aromatic", "severity": "WARNING"}
            ],
        },
        "scoring": {"ml_readiness": {"score": 72, "interpretation": "Good"}},
    },
    {
        "index": 2,
        "smiles": "CC(C)C",
        "name": "Isobutane",
        "status": "success",
        "validation": {
            "overall_score": 45,
            "issues": [
                {
                    "check_name": "complexity",
                    "passed": False,
                    "severity": "CRITICAL",
                    "message": "Too simple",
                }
            ],
        },
        "alerts": {"has_alerts": False, "alert_count": 0, "alerts": []},
        "scoring": {"ml_readiness": {"score": 40, "interpretation": "Poor"}},
    },
    {
        "index": 3,
        "smiles": "INVALID",
        "name": None,
        "status": "error",
        "validation": None,
        "alerts": None,
        "scoring": None,
    },
]


class TestPDFReportGenerator:
    """Test PDFReportGenerator class."""

    def test_init(self):
        """Test generator initialization."""
        generator = PDFReportGenerator()
        assert generator is not None
        assert generator.media_type == "application/pdf"
        assert generator.file_extension == "pdf"

    def test_factory_registration(self):
        """Test PDF format is registered in factory."""
        exporter = ExporterFactory.create(ExportFormat.PDF)
        assert isinstance(exporter, PDFReportGenerator)

    @requires_weasyprint
    def test_generate_pdf_basic(self):
        """Test PDF generation with sample data."""
        generator = PDFReportGenerator()
        pdf_buffer = generator.export(SAMPLE_RESULTS)

        # Verify it's a BytesIO buffer
        assert isinstance(pdf_buffer, BytesIO)

        # Verify it contains PDF data
        pdf_buffer.seek(0)
        pdf_data = pdf_buffer.read()
        assert pdf_data.startswith(b"%PDF-")  # PDF signature

    @requires_weasyprint
    def test_generate_pdf_readable(self):
        """Test generated PDF is valid and readable."""
        generator = PDFReportGenerator()
        pdf_buffer = generator.export(SAMPLE_RESULTS)

        # Try to read with PyPDF2
        pdf_buffer.seek(0)
        reader = PdfReader(pdf_buffer)

        # Verify has pages
        assert len(reader.pages) > 0

        # Verify first page has content
        first_page = reader.pages[0]
        text = first_page.extract_text()
        assert "ChemAudit" in text
        # PDF text extraction may break "Batch Validation Report" across lines
        assert "Validation" in text and "Report" in text

    def test_calculate_statistics(self):
        """Test statistics calculation."""
        generator = PDFReportGenerator()
        stats = generator._calculate_statistics(SAMPLE_RESULTS)

        assert stats["total"] == 4
        assert stats["successful"] == 3
        assert stats["errors"] == 1
        assert stats["avg_validation_score"] is not None
        assert 60 < stats["avg_validation_score"] < 75  # (95+75+45)/3 ≈ 71.67
        assert stats["score_distribution"]["excellent"] == 1  # 95
        assert stats["score_distribution"]["good"] == 1  # 75
        assert stats["score_distribution"]["moderate"] == 0
        assert stats["score_distribution"]["poor"] == 1  # 45

    def test_calculate_statistics_empty(self):
        """Test statistics with no successful results."""
        generator = PDFReportGenerator()
        stats = generator._calculate_statistics(
            [{"index": 0, "status": "error", "validation": None}]
        )

        assert stats["total"] == 1
        assert stats["successful"] == 0
        assert stats["errors"] == 1
        assert stats["avg_validation_score"] is None
        assert stats["avg_ml_readiness_score"] is None

    def test_generate_score_distribution_chart(self):
        """Test SVG chart generation."""
        generator = PDFReportGenerator()
        chart_data = generator._generate_score_distribution_chart(SAMPLE_RESULTS)

        # Verify it's base64-encoded
        assert isinstance(chart_data, str)
        assert len(chart_data) > 0

        # Decode and verify it's valid SVG
        svg_content = base64.b64decode(chart_data).decode("utf-8")
        assert "<svg" in svg_content
        assert "</svg>" in svg_content
        assert "Excellent" in svg_content
        assert "Good" in svg_content

    def test_generate_chart_empty_results(self):
        """Test chart generation with no valid scores."""
        generator = PDFReportGenerator()
        chart_data = generator._generate_score_distribution_chart([])
        assert chart_data == ""

    def test_extract_critical_issues(self):
        """Test critical issues extraction."""
        generator = PDFReportGenerator()
        issues = generator._extract_critical_issues(SAMPLE_RESULTS, limit=10)

        # Should find the CRITICAL issue
        assert len(issues) == 1
        assert issues[0]["severity"] == "CRITICAL"
        assert issues[0]["issue"] == "Too simple"
        assert issues[0]["index"] == 2

    def test_extract_critical_issues_limit(self):
        """Test critical issues respects limit."""
        # Create results with many critical issues
        many_results = [
            {
                "index": i,
                "smiles": "C",
                "validation": {
                    "overall_score": 50,
                    "issues": [
                        {
                            "severity": "CRITICAL",
                            "message": f"Issue {i}",
                            "check_name": "test",
                        }
                    ],
                },
            }
            for i in range(50)
        ]

        generator = PDFReportGenerator()
        issues = generator._extract_critical_issues(many_results, limit=20)

        assert len(issues) == 20

    def test_get_all_molecules(self):
        """Test all molecules extraction."""
        generator = PDFReportGenerator()
        all_mols = generator._get_all_molecules(SAMPLE_RESULTS)

        # Should get 4 molecules (all with non-empty SMILES strings, even "INVALID")
        assert len(all_mols) == 4

        # Should be sorted by index
        assert all_mols[0]["index"] == 0
        assert all_mols[1]["index"] == 1
        assert all_mols[2]["index"] == 2
        assert all_mols[3]["index"] == 3

    def test_get_all_molecules_with_images(self):
        """Test all molecules include images."""
        generator = PDFReportGenerator()
        all_mols = generator._get_all_molecules(SAMPLE_RESULTS)

        # Check images were generated for valid SMILES
        for mol in all_mols:
            assert "image_data" in mol
            # Should be base64-encoded PNG or None
            if mol["image_data"]:
                assert isinstance(mol["image_data"], str)

    def test_mol_to_base64_png_valid(self):
        """Test molecule to PNG conversion."""
        generator = PDFReportGenerator()
        img_data = generator._mol_to_base64_png("CCO", width=200, height=200)

        assert img_data is not None
        assert isinstance(img_data, str)

        # Decode and verify it's PNG
        img_bytes = base64.b64decode(img_data)
        assert img_bytes.startswith(b"\x89PNG")  # PNG signature

    def test_mol_to_base64_png_invalid(self):
        """Test molecule conversion with invalid SMILES."""
        generator = PDFReportGenerator()
        img_data = generator._mol_to_base64_png("INVALID_SMILES")

        assert img_data is None

    @requires_weasyprint
    def test_pdf_includes_alert_summary(self):
        """Test PDF includes alert summary when alerts present."""
        generator = PDFReportGenerator()
        pdf_buffer = generator.export(SAMPLE_RESULTS)

        pdf_buffer.seek(0)
        reader = PdfReader(pdf_buffer)
        text = "".join(page.extract_text() for page in reader.pages)

        # Should mention PAINS
        assert "PAINS" in text or "Alert" in text

    @requires_weasyprint
    def test_pdf_includes_statistics(self):
        """Test PDF includes all key statistics."""
        generator = PDFReportGenerator()
        pdf_buffer = generator.export(SAMPLE_RESULTS)

        pdf_buffer.seek(0)
        reader = PdfReader(pdf_buffer)
        text = "".join(page.extract_text() for page in reader.pages)

        # Should include key statistics
        assert "Total Molecules" in text or "4" in text
        assert "Successful" in text or "3" in text


class TestPDFExportEndpoint:
    """Test PDF export via API endpoint (integration test)."""

    @pytest.fixture
    async def client(self):
        """Create test client."""
        from httpx import ASGITransport, AsyncClient

        from app.main import app

        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as client:
            yield client

    @pytest.mark.asyncio
    async def test_pdf_export_format_available(self, client):
        """Test PDF format is accepted by endpoint."""
        # This will fail if job doesn't exist, but should accept PDF format
        response = await client.get("/api/v1/batch/test_job/export?format=pdf")

        # Should return 404 (job not found) not 422 (invalid format)
        assert response.status_code == 404
        assert "not found" in response.json()["detail"].lower()
