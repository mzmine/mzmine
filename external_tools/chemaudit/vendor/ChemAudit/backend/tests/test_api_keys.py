"""
Tests for API key management functionality.
"""

from unittest.mock import AsyncMock, patch

import pytest
from fastapi.testclient import TestClient

from app.core.config import settings

# Admin secret for testing
ADMIN_SECRET = settings.API_KEY_ADMIN_SECRET


@pytest.fixture
def client():
    """Create test client."""
    from app.main import app

    return TestClient(app)


@pytest.fixture
def admin_headers():
    """Headers with admin authentication."""
    return {"X-Admin-Secret": ADMIN_SECRET}


@pytest.fixture
def mock_redis():
    """Mock Redis client."""
    mock = AsyncMock()
    return mock


def test_create_api_key_success(client, admin_headers, mock_redis):
    """Test creating a new API key."""
    with patch("app.api.routes.api_keys.get_redis_client") as mock_get_redis:
        mock_get_redis.return_value = mock_redis

        response = client.post(
            "/api/v1/api-keys",
            json={"name": "Test Key", "description": "Test API key"},
            headers=admin_headers,
        )

        assert response.status_code == 201
        data = response.json()

        # Check response structure
        assert "key" in data
        assert "name" in data
        assert "created_at" in data
        assert data["name"] == "Test Key"

        # Key should be a secure token
        assert len(data["key"]) > 20
        assert isinstance(data["key"], str)


def test_create_api_key_minimal(client, admin_headers, mock_redis):
    """Test creating API key with minimal data (no description)."""
    with patch("app.api.routes.api_keys.get_redis_client") as mock_get_redis:
        mock_get_redis.return_value = mock_redis

        response = client.post(
            "/api/v1/api-keys",
            json={"name": "Minimal Key"},
            headers=admin_headers,
        )

        assert response.status_code == 201
        data = response.json()
        assert data["name"] == "Minimal Key"


def test_create_api_key_validation_error(client, admin_headers):
    """Test validation errors when creating API key."""
    # Missing required name field
    response = client.post("/api/v1/api-keys", json={}, headers=admin_headers)

    assert response.status_code == 422  # Validation error


def test_list_api_keys(client, admin_headers, mock_redis):
    """Test listing API keys."""
    with patch("app.api.routes.api_keys.get_redis_client") as mock_get_redis:
        mock_redis.smembers.return_value = {"hash1", "hash2"}
        mock_redis.hgetall.side_effect = [
            {
                "name": "Key 1",
                "description": "First key",
                "created_at": "2026-01-23T00:00:00Z",
                "last_used": "2026-01-23T01:00:00Z",
                "request_count": "42",
            },
            {
                "name": "Key 2",
                "description": "",
                "created_at": "2026-01-23T00:00:00Z",
                "last_used": "",
                "request_count": "0",
            },
        ]
        mock_get_redis.return_value = mock_redis

        response = client.get("/api/v1/api-keys", headers=admin_headers)

        assert response.status_code == 200
        data = response.json()

        # Should return list of keys
        assert isinstance(data, list)
        assert len(data) == 2

        # Check first key
        assert data[0]["name"] == "Key 1"
        assert data[0]["request_count"] == 42
        assert "key" not in data[0]  # Full key should NOT be in list response

        # Check second key with no last_used
        assert data[1]["name"] == "Key 2"
        assert data[1]["last_used"] is None


def test_revoke_api_key(client, admin_headers, mock_redis):
    """Test revoking an API key."""
    with patch("app.api.routes.api_keys.get_redis_client") as mock_get_redis:
        mock_redis.smembers.return_value = {"abcd123456789012"}
        mock_get_redis.return_value = mock_redis

        response = client.delete("/api/v1/api-keys/abcd12345678", headers=admin_headers)

        assert response.status_code == 204


def test_revoke_nonexistent_key(client, admin_headers, mock_redis):
    """Test revoking a key that doesn't exist."""
    with patch("app.api.routes.api_keys.get_redis_client") as mock_get_redis:
        mock_redis.smembers.return_value = set()  # No keys
        mock_get_redis.return_value = mock_redis

        response = client.delete("/api/v1/api-keys/nonexistent", headers=admin_headers)

        assert response.status_code == 404
        data = response.json()
        assert "not found" in data["detail"].lower()


def test_validate_api_key_success(client):
    """Test successful API key validation."""
    with patch("app.core.security.validate_api_key") as mock_validate:
        mock_validate.return_value = {
            "name": "Valid Key",
            "created_at": "2026-01-23T00:00:00Z",
        }

        # Use API key in a request
        response = client.get("/api/v1/checks", headers={"X-API-Key": "valid_key"})

        # Should succeed (or fail for other reasons, but not auth)
        assert response.status_code != 401


def test_validate_api_key_failure(client):
    """Test API key validation failure."""
    with patch("app.core.security.validate_api_key") as mock_validate:
        mock_validate.return_value = None  # Invalid key

        response = client.get("/api/v1/checks", headers={"X-API-Key": "invalid_key"})

        assert response.status_code == 401
        data = response.json()
        assert "Invalid" in data["detail"] and "API key" in data["detail"]


def test_hash_api_key():
    """Test API key hashing function uses Argon2."""
    from app.core.security import hash_api_key, verify_api_key_hash

    key1 = "test_key_123"
    key2 = "different_key"

    # Hash should be Argon2 format (starts with $argon2)
    hash1 = hash_api_key(key1)
    assert hash1.startswith("$argon2")

    # Verify function should work
    assert verify_api_key_hash(key1, hash1) is True
    assert verify_api_key_hash(key2, hash1) is False


def test_hash_api_key_for_lookup():
    """Test API key lookup hash function uses SHA256."""
    from app.core.security import hash_api_key_for_lookup

    key1 = "test_key_123"
    key2 = "test_key_123"
    key3 = "different_key"

    # Same input should produce same hash
    hash1 = hash_api_key_for_lookup(key1)
    hash2 = hash_api_key_for_lookup(key2)
    assert hash1 == hash2

    # Different input should produce different hash
    hash3 = hash_api_key_for_lookup(key3)
    assert hash1 != hash3

    # Hash should be SHA256 (64 hex characters)
    assert len(hash1) == 64
    assert all(c in "0123456789abcdef" for c in hash1)


def test_anonymous_access_allowed(client):
    """Test that endpoints work without API key (anonymous access)."""
    # Should work without X-API-Key header
    response = client.get("/api/v1/checks")

    # Should succeed or fail for non-auth reasons
    assert response.status_code != 401
