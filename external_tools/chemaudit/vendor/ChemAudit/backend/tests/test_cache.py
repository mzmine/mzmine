"""
Tests for validation caching functionality.

Tests the Redis-based caching layer for validation results.
"""

import json
from unittest.mock import AsyncMock

import pytest

from app.core.cache import (
    get_cache_stats,
    get_cached_validation,
    invalidate_cached_validation,
    set_cached_validation,
    validation_cache_key,
)


class TestValidationCacheKey:
    """Tests for cache key generation."""

    def test_cache_key_with_valid_inchikey(self):
        """Test cache key generation with valid InChIKey."""
        key = validation_cache_key("BSYNRYMUTXBXSQ-UHFFFAOYSA-N", None)
        assert key is not None
        assert key.startswith("validation:BSYNRYMUTXBXSQ-UHFFFAOYSA-N:")
        # Should have 16-character SHA256 hash suffix
        parts = key.split(":")
        assert len(parts) == 3
        assert len(parts[2]) == 16

    def test_cache_key_with_checks(self):
        """Test cache key includes checks hash."""
        key1 = validation_cache_key("BSYNRYMUTXBXSQ-UHFFFAOYSA-N", ["parsability"])
        key2 = validation_cache_key("BSYNRYMUTXBXSQ-UHFFFAOYSA-N", ["valence"])
        key3 = validation_cache_key("BSYNRYMUTXBXSQ-UHFFFAOYSA-N", ["parsability"])

        # Different checks should have different keys
        assert key1 != key2
        # Same checks should have same key
        assert key1 == key3

    def test_cache_key_checks_order_independent(self):
        """Test that check order doesn't affect cache key."""
        key1 = validation_cache_key("TEST-INCHIKEY", ["a", "b", "c"])
        key2 = validation_cache_key("TEST-INCHIKEY", ["c", "a", "b"])
        assert key1 == key2

    def test_cache_key_none_inchikey(self):
        """Test that None InChIKey returns None (graceful skip)."""
        key = validation_cache_key(None, None)
        assert key is None

    def test_cache_key_empty_inchikey(self):
        """Test that empty InChIKey returns None."""
        key = validation_cache_key("", None)
        assert key is None

    def test_cache_key_all_checks(self):
        """Test cache key with 'all' checks."""
        key1 = validation_cache_key("TEST-INCHIKEY", None)
        key2 = validation_cache_key("TEST-INCHIKEY", ["all"])
        assert key1 == key2


class TestGetCachedValidation:
    """Tests for cache retrieval."""

    @pytest.mark.asyncio
    async def test_cache_hit(self):
        """Test successful cache retrieval."""
        mock_redis = AsyncMock()
        cached_data = {"overall_score": 100, "issues": []}
        mock_redis.get.return_value = json.dumps(cached_data)

        result = await get_cached_validation(mock_redis, "validation:test:abc123")

        assert result == cached_data
        mock_redis.get.assert_called_once_with("validation:test:abc123")

    @pytest.mark.asyncio
    async def test_cache_miss(self):
        """Test cache miss returns None."""
        mock_redis = AsyncMock()
        mock_redis.get.return_value = None

        result = await get_cached_validation(mock_redis, "validation:test:abc123")

        assert result is None

    @pytest.mark.asyncio
    async def test_cache_with_none_key(self):
        """Test that None cache key returns None without Redis call."""
        mock_redis = AsyncMock()

        result = await get_cached_validation(mock_redis, None)

        assert result is None
        mock_redis.get.assert_not_called()

    @pytest.mark.asyncio
    async def test_cache_redis_error(self):
        """Test graceful handling of Redis errors."""
        mock_redis = AsyncMock()
        mock_redis.get.side_effect = Exception("Redis connection failed")

        result = await get_cached_validation(mock_redis, "validation:test:abc123")

        assert result is None  # Should not raise, just return None


class TestSetCachedValidation:
    """Tests for cache storage."""

    @pytest.mark.asyncio
    async def test_cache_set_success(self):
        """Test successful cache storage."""
        mock_redis = AsyncMock()
        mock_redis.setex.return_value = True

        result_data = {"overall_score": 100, "issues": []}
        result = await set_cached_validation(
            mock_redis, "validation:test:abc123", result_data, ttl=3600
        )

        assert result is True
        mock_redis.setex.assert_called_once()
        # Verify the key and TTL
        call_args = mock_redis.setex.call_args
        assert call_args[0][0] == "validation:test:abc123"
        assert call_args[0][1] == 3600

    @pytest.mark.asyncio
    async def test_cache_set_with_none_key(self):
        """Test that None cache key skips caching."""
        mock_redis = AsyncMock()

        result = await set_cached_validation(mock_redis, None, {"score": 100})

        assert result is False
        mock_redis.setex.assert_not_called()

    @pytest.mark.asyncio
    async def test_cache_set_redis_error(self):
        """Test graceful handling of Redis errors on write."""
        mock_redis = AsyncMock()
        mock_redis.setex.side_effect = Exception("Redis write failed")

        result = await set_cached_validation(
            mock_redis, "validation:test:abc123", {"score": 100}
        )

        assert result is False  # Should not raise, just return False


class TestInvalidateCachedValidation:
    """Tests for cache invalidation."""

    @pytest.mark.asyncio
    async def test_invalidate_existing_keys(self):
        """Test invalidation of existing cache entries."""
        mock_redis = AsyncMock()

        # Mock scan_iter to return some keys
        async def mock_scan_iter(pattern):
            for key in ["validation:TEST:abc", "validation:TEST:def"]:
                yield key

        mock_redis.scan_iter = mock_scan_iter
        mock_redis.delete.return_value = 2

        deleted = await invalidate_cached_validation(mock_redis, "TEST")

        assert deleted == 2

    @pytest.mark.asyncio
    async def test_invalidate_empty_inchikey(self):
        """Test that empty InChIKey skips invalidation."""
        mock_redis = AsyncMock()

        deleted = await invalidate_cached_validation(mock_redis, "")

        assert deleted == 0


class TestGetCacheStats:
    """Tests for cache statistics."""

    @pytest.mark.asyncio
    async def test_cache_stats_success(self):
        """Test successful stats retrieval."""
        mock_redis = AsyncMock()

        # Mock scan_iter
        async def mock_scan_iter(pattern):
            for _ in range(5):
                yield "key"

        mock_redis.scan_iter = mock_scan_iter
        mock_redis.info.return_value = {
            "used_memory": 1024000,
            "used_memory_human": "1000K",
        }

        stats = await get_cache_stats(mock_redis)

        assert stats["validation_keys"] == 5
        assert stats["memory_used_bytes"] == 1024000

    @pytest.mark.asyncio
    async def test_cache_stats_error(self):
        """Test graceful handling of stats errors."""
        mock_redis = AsyncMock()
        mock_redis.info.side_effect = Exception("Stats failed")

        stats = await get_cache_stats(mock_redis)

        assert "error" in stats
