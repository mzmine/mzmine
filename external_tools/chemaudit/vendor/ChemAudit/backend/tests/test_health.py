"""
Test health check endpoint
"""

import pytest


@pytest.mark.asyncio
async def test_health_check_endpoint(client):
    """Test GET /api/v1/health returns 200 with correct status and rdkit_version"""
    response = await client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["app_name"] == "ChemAudit"
    assert data["app_version"] == "1.0.0"
    assert "rdkit_version" in data
    # RDKit version should be present if installed
    if data["rdkit_version"] is not None:
        assert isinstance(data["rdkit_version"], str)
        assert len(data["rdkit_version"]) > 0


@pytest.mark.asyncio
async def test_root_endpoint(client):
    """Test GET / returns API info"""
    response = await client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "name" in data
    assert "version" in data
    assert data["name"] == "ChemAudit"
    assert data["version"] == "1.0.0"


@pytest.mark.asyncio
async def test_health_response_schema(client):
    """Test health response matches HealthResponse schema"""
    response = await client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    # Verify all required fields are present
    required_fields = ["status", "app_name", "app_version", "rdkit_version"]
    for field in required_fields:
        assert field in data, f"Missing required field: {field}"
