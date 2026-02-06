"""
Structural Alert Screening Services

Provides PAINS, BRENK, and other structural alert pattern screening
using RDKit FilterCatalog.
"""

from .alert_manager import AlertManager, alert_manager
from .filter_catalog import AVAILABLE_CATALOGS, get_filter_catalog

__all__ = [
    "get_filter_catalog",
    "AVAILABLE_CATALOGS",
    "AlertManager",
    "alert_manager",
]
