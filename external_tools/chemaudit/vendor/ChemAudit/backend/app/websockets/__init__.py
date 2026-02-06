"""
WebSocket Module

Provides real-time communication for batch processing progress.
"""

from .manager import ConnectionManager, manager

__all__ = ["ConnectionManager", "manager"]
