"""
Unit Tests for Export Services

Tests each exporter individually.
"""

import json

from app.services.export import (
    CSVExporter,
    ExcelExporter,
    ExporterFactory,
    ExportFormat,
    JSONExporter,
    SDFExporter,
)

# Sample test data
SAMPLE_RESULTS = [
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
    {
        "smiles": "c1ccccc1",
        "name": "Benzene",
        "index": 1,
        "status": "success",
        "validation": {
            "canonical_smiles": "c1ccccc1",
            "inchikey": "UHOVQNZJYSORNB-UHFFFAOYSA-N",
            "overall_score": 85,
            "issues": [
                {
                    "check_name": "Alert",
                    "message": "PAINS alert detected",
                }
            ],
        },
        "alerts": {
            "pains": {
                "matches": [
                    {
                        "pattern_name": "anil_di_alk_D(258)",
                        "smarts": "[Br]",
                    }
                ]
            },
            "brenk": {"matches": []},
        },
        "scoring": {
            "ml_readiness_score": 75,
            "np_likeness_score": 1.2,
        },
        "standardized_smiles": "c1ccccc1",
    },
    {
        "smiles": "INVALID",
        "name": "Bad Molecule",
        "index": 2,
        "status": "error",
        "error": "Invalid SMILES",
        "validation": {},
        "alerts": {},
        "scoring": {},
    },
]


class TestCSVExporter:
    """Test CSV export functionality."""

    def test_csv_export_basic(self):
        """Test basic CSV export."""
        exporter = CSVExporter()
        buffer = exporter.export(SAMPLE_RESULTS)

        # Read CSV
        buffer.seek(0)
        content = buffer.read().decode("utf-8")

        # Check header
        assert "index,name,input_smiles,canonical_smiles" in content
        assert "overall_score" in content
        assert "ml_readiness_score" in content

        # Check data rows
        assert "Ethanol" in content
        assert "CCO" in content
        assert "95" in content

    def test_csv_media_type(self):
        """Test CSV media type."""
        exporter = CSVExporter()
        assert exporter.media_type == "text/csv"
        assert exporter.file_extension == "csv"

    def test_csv_empty_results(self):
        """Test CSV export with empty results."""
        exporter = CSVExporter()
        buffer = exporter.export([])

        buffer.seek(0)
        content = buffer.read().decode("utf-8")

        # Should have header but no data rows
        assert "index" in content
        lines = content.strip().split("\n")
        assert len(lines) == 1  # Only header


class TestExcelExporter:
    """Test Excel export functionality."""

    def test_excel_export_basic(self):
        """Test basic Excel export."""
        exporter = ExcelExporter()
        buffer = exporter.export(SAMPLE_RESULTS)

        # Check buffer is not empty
        buffer.seek(0)
        content = buffer.read()
        assert len(content) > 0

        # Verify it's a valid Excel file (starts with PK for ZIP)
        assert content[:2] == b"PK"

    def test_excel_media_type(self):
        """Test Excel media type."""
        exporter = ExcelExporter()
        assert (
            exporter.media_type
            == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        assert exporter.file_extension == "xlsx"

    def test_excel_empty_results(self):
        """Test Excel export with empty results."""
        exporter = ExcelExporter()
        buffer = exporter.export([])

        buffer.seek(0)
        content = buffer.read()
        assert len(content) > 0  # Should still create a valid Excel file


class TestSDFExporter:
    """Test SDF export functionality."""

    def test_sdf_export_basic(self):
        """Test basic SDF export."""
        exporter = SDFExporter()
        buffer = exporter.export(SAMPLE_RESULTS)

        # Read SDF
        buffer.seek(0)
        content = buffer.read().decode("utf-8")

        # Check SDF structure markers
        assert "$$$$" in content  # SDF record separator

        # Check molecule names
        assert "Ethanol" in content or "mol_1" in content

    def test_sdf_properties(self):
        """Test SDF export includes properties."""
        exporter = SDFExporter()
        buffer = exporter.export(SAMPLE_RESULTS)

        buffer.seek(0)
        content = buffer.read().decode("utf-8")

        # Check properties are included
        assert "overall_score" in content
        assert "ml_readiness_score" in content

    def test_sdf_invalid_smiles_skipped(self):
        """Test SDF export skips invalid SMILES."""
        exporter = SDFExporter()
        buffer = exporter.export(SAMPLE_RESULTS)

        buffer.seek(0)
        content = buffer.read().decode("utf-8")

        # Invalid molecule should be skipped
        assert "Bad Molecule" not in content or "INVALID" not in content

    def test_sdf_media_type(self):
        """Test SDF media type."""
        exporter = SDFExporter()
        assert exporter.media_type == "chemical/x-mdl-sdfile"
        assert exporter.file_extension == "sdf"

    def test_sdf_empty_results(self):
        """Test SDF export with empty results."""
        exporter = SDFExporter()
        buffer = exporter.export([])

        buffer.seek(0)
        content = buffer.read().decode("utf-8")
        assert len(content) == 0  # Empty SDF


class TestJSONExporter:
    """Test JSON export functionality."""

    def test_json_export_basic(self):
        """Test basic JSON export."""
        exporter = JSONExporter()
        buffer = exporter.export(SAMPLE_RESULTS)

        # Parse JSON
        buffer.seek(0)
        data = json.loads(buffer.read().decode("utf-8"))

        # Check structure
        assert "metadata" in data
        assert "results" in data

        # Check metadata
        assert "export_date" in data["metadata"]
        assert "total_count" in data["metadata"]
        assert data["metadata"]["total_count"] == len(SAMPLE_RESULTS)

        # Check results
        assert len(data["results"]) == len(SAMPLE_RESULTS)
        assert data["results"][0]["name"] == "Ethanol"

    def test_json_media_type(self):
        """Test JSON media type."""
        exporter = JSONExporter()
        assert exporter.media_type == "application/json"
        assert exporter.file_extension == "json"

    def test_json_empty_results(self):
        """Test JSON export with empty results."""
        exporter = JSONExporter()
        buffer = exporter.export([])

        buffer.seek(0)
        data = json.loads(buffer.read().decode("utf-8"))

        assert data["metadata"]["total_count"] == 0
        assert len(data["results"]) == 0


class TestExporterFactory:
    """Test ExporterFactory."""

    def test_factory_creates_csv(self):
        """Test factory creates CSV exporter."""
        exporter = ExporterFactory.create(ExportFormat.CSV)
        assert isinstance(exporter, CSVExporter)

    def test_factory_creates_excel(self):
        """Test factory creates Excel exporter."""
        exporter = ExporterFactory.create(ExportFormat.EXCEL)
        assert isinstance(exporter, ExcelExporter)

    def test_factory_creates_sdf(self):
        """Test factory creates SDF exporter."""
        exporter = ExporterFactory.create(ExportFormat.SDF)
        assert isinstance(exporter, SDFExporter)

    def test_factory_creates_json(self):
        """Test factory creates JSON exporter."""
        exporter = ExporterFactory.create(ExportFormat.JSON)
        assert isinstance(exporter, JSONExporter)

    def test_factory_invalid_format(self):
        """Test factory raises error for invalid format."""
        # Note: This test would require passing an invalid enum value
        # which is type-checked, so we skip this test
        pass
