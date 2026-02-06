"""
Pytest configuration and fixtures for backend tests
"""

import os

import pytest
from httpx import ASGITransport, AsyncClient

# Disable rate limiting for tests
os.environ.setdefault("RATE_LIMIT_ENABLED", "false")

from app.main import app


@pytest.fixture
async def client():
    """Create async test client"""
    async with AsyncClient(
        transport=ASGITransport(app=app), base_url="http://test"
    ) as ac:
        yield ac
