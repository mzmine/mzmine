"""
JSON Exporter

Exports batch results to JSON format with metadata.
"""

import json
from datetime import datetime, timezone
from io import BytesIO
from typing import Any, Dict, List

try:
    import orjson

    HAS_ORJSON = True
except ImportError:
    HAS_ORJSON = False

from .base import BaseExporter, ExporterFactory, ExportFormat


class JSONExporter(BaseExporter):
    """Export batch results to JSON format."""

    def export(self, results: List[Dict[str, Any]]) -> BytesIO:
        """
        Export results to JSON format with metadata.

        Args:
            results: List of batch result dictionaries

        Returns:
            BytesIO buffer containing JSON data
        """
        # Prepare export data with metadata
        export_data = {
            "metadata": {
                "export_date": datetime.now(timezone.utc).isoformat(),
                "total_count": len(results),
                "format_version": "1.0",
                "tool": "ChemAudit",
            },
            "results": results,
        }

        # Serialize to JSON
        if HAS_ORJSON:
            # Use orjson for fast serialization
            json_bytes = orjson.dumps(
                export_data, option=orjson.OPT_INDENT_2 | orjson.OPT_APPEND_NEWLINE
            )
        else:
            # Fallback to standard json
            json_str = json.dumps(export_data, indent=2, ensure_ascii=False)
            json_bytes = json_str.encode("utf-8")

        # Create BytesIO buffer
        bytes_buffer = BytesIO(json_bytes)
        bytes_buffer.seek(0)

        return bytes_buffer

    @property
    def media_type(self) -> str:
        """MIME type for JSON."""
        return "application/json"

    @property
    def file_extension(self) -> str:
        """File extension for JSON."""
        return "json"


# Register with factory
ExporterFactory.register(ExportFormat.JSON, JSONExporter)
