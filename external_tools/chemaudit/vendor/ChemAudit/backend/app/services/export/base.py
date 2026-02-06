"""
Base Exporter and Factory Pattern

Provides abstract base class for exporters and factory for creating exporters.
"""

from abc import ABC, abstractmethod
from enum import Enum
from io import BytesIO
from typing import Any, Dict, List, Tuple

# Alert catalog names used across exporters
ALERT_CATALOGS = ["pains", "brenk", "nih", "glaxo"]


def analyze_alerts(alerts: Dict[str, Any]) -> Tuple[int, Dict[str, int], List[str]]:
    """
    Analyze alerts dictionary and return all needed data in a single pass.

    Args:
        alerts: Alerts dictionary from batch result

    Returns:
        Tuple of (total_count, distribution_by_catalog, alert_names)
    """
    if not alerts:
        return 0, {}, []

    total_count = 0
    distribution: Dict[str, int] = {}
    names: List[str] = []

    for catalog in ALERT_CATALOGS:
        catalog_data = alerts.get(catalog, {})
        if not isinstance(catalog_data, dict):
            continue

        matches = catalog_data.get("matches", [])
        count = len(matches)
        total_count += count

        if count > 0:
            distribution[catalog] = count
            for match in matches:
                pattern_name = match.get("pattern_name", "unknown")
                names.append(f"{catalog.upper()}:{pattern_name}")

    return total_count, distribution, names


def count_alerts(alerts: Dict[str, Any]) -> int:
    """Count total alerts from an alerts dictionary."""
    total, _, _ = analyze_alerts(alerts)
    return total


def count_alerts_by_catalog(alerts: Dict[str, Any]) -> Dict[str, int]:
    """Count alerts grouped by catalog."""
    _, distribution, _ = analyze_alerts(alerts)
    return distribution


def extract_alert_names(alerts: Dict[str, Any]) -> List[str]:
    """Extract alert names in 'CATALOG:pattern_name' format."""
    _, _, names = analyze_alerts(alerts)
    return names


class ExportFormat(str, Enum):
    """Export format options (extends str for JSON serialization)."""

    CSV = "csv"
    EXCEL = "excel"
    SDF = "sdf"
    JSON = "json"
    PDF = "pdf"


class BaseExporter(ABC):
    """Abstract base class for all exporters."""

    @abstractmethod
    def export(self, results: List[Dict[str, Any]]) -> BytesIO:
        """
        Export batch results to specific format.

        Args:
            results: List of batch result dictionaries

        Returns:
            BytesIO buffer containing exported data
        """
        pass

    @property
    @abstractmethod
    def media_type(self) -> str:
        """MIME type for this export format."""
        pass

    @property
    @abstractmethod
    def file_extension(self) -> str:
        """File extension for this export format (without dot)."""
        pass


class ExporterFactory:
    """Factory for creating exporters based on format."""

    _exporters: Dict[ExportFormat, type] = {}

    @classmethod
    def register(cls, format: ExportFormat, exporter_class: type) -> None:
        """Register an exporter class for a format."""
        cls._exporters[format] = exporter_class

    @classmethod
    def create(cls, format: ExportFormat) -> BaseExporter:
        """
        Create exporter instance for specified format.

        Args:
            format: ExportFormat enum value

        Returns:
            BaseExporter instance

        Raises:
            ValueError: If format not supported
        """
        exporter_class = cls._exporters.get(format)
        if not exporter_class:
            raise ValueError(f"Unsupported export format: {format}")
        return exporter_class()
