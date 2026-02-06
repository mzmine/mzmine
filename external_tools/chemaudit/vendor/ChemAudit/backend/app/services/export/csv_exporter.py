"""
CSV Exporter

Exports batch results to CSV format using pandas.
"""

from io import BytesIO, StringIO
from typing import Any, Dict, List

import pandas as pd

from .base import BaseExporter, ExporterFactory, ExportFormat, count_alerts


class CSVExporter(BaseExporter):
    """Export batch results to CSV format."""

    def export(self, results: List[Dict[str, Any]]) -> BytesIO:
        """
        Export results to CSV format.

        Args:
            results: List of batch result dictionaries

        Returns:
            BytesIO buffer containing CSV data
        """
        # Extract relevant fields from results
        rows = []
        for idx, result in enumerate(results):
            # Get validation data - use 'or {}' to handle None values
            validation = result.get("validation") or {}
            scoring = result.get("scoring") or {}
            alerts = result.get("alerts") or {}

            # Count alerts
            alerts_count = count_alerts(alerts)

            # Get QED and SA scores
            qed_score = None
            sa_score = None
            if scoring:
                druglikeness = scoring.get("druglikeness") or {}
                if druglikeness and "error" not in druglikeness:
                    qed_score = druglikeness.get("qed_score")

                admet = scoring.get("admet") or {}
                if admet and "error" not in admet:
                    sa_score = admet.get("sa_score")

            # Collect issues for summary
            issues = validation.get("issues", [])
            issues_summary = "; ".join(
                [
                    f"{issue.get('check_name', 'unknown')}: {issue.get('message', '')}"
                    for issue in issues[:3]
                ]  # Limit to first 3 issues
            )

            row = {
                "index": idx + 1,
                "name": result.get("name", ""),
                "input_smiles": result.get("smiles", ""),
                "canonical_smiles": validation.get("canonical_smiles", ""),
                "inchikey": validation.get("inchikey", ""),
                "overall_score": validation.get("overall_score", 0),
                "ml_readiness_score": (
                    scoring.get("ml_readiness_score", 0) if scoring else 0
                ),
                "qed_score": qed_score if qed_score is not None else "",
                "sa_score": sa_score if sa_score is not None else "",
                "np_likeness_score": (
                    scoring.get("np_likeness_score", 0) if scoring else 0
                ),
                "alerts_count": alerts_count,
                "issues_summary": issues_summary,
                "standardized_smiles": result.get("standardized_smiles", ""),
            }
            rows.append(row)

        # Define columns
        columns = [
            "index",
            "name",
            "input_smiles",
            "canonical_smiles",
            "inchikey",
            "overall_score",
            "ml_readiness_score",
            "qed_score",
            "sa_score",
            "np_likeness_score",
            "alerts_count",
            "issues_summary",
            "standardized_smiles",
        ]

        # Create DataFrame with explicit columns
        df = pd.DataFrame(rows, columns=columns)

        # Export to CSV in StringIO
        string_buffer = StringIO()
        df.to_csv(string_buffer, index=False)

        # Convert to BytesIO
        bytes_buffer = BytesIO()
        bytes_buffer.write(string_buffer.getvalue().encode("utf-8"))
        bytes_buffer.seek(0)

        return bytes_buffer

    @property
    def media_type(self) -> str:
        """MIME type for CSV."""
        return "text/csv"

    @property
    def file_extension(self) -> str:
        """File extension for CSV."""
        return "csv"


# Register with factory
ExporterFactory.register(ExportFormat.CSV, CSVExporter)
