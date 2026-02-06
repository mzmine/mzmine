"""
Redis-based caching for validation results.

Provides InChIKey-based caching to avoid re-computing validation for repeated molecules.
Cache keys combine InChIKey with check configuration for unique identification.

Usage:
    cache_key = validation_cache_key(inchikey, checks)
    cached = await get_cached_validation(redis, cache_key)
    if cached:
        return cached
    result = compute_validation(mol)
    await set_cached_validation(redis, cache_key, result)
    return result
"""

import hashlib
import json
import logging
from typing import Any, Dict, List, Optional

from redis.asyncio import Redis

from app.core.config import settings

logger = logging.getLogger(__name__)


def validation_cache_key(
    inchikey: Optional[str],
    checks: Optional[List[str]] = None,
) -> Optional[str]:
    """
    Generate cache key for validation results.

    Key format: validation:{inchikey}:{checks_hash}

    Args:
        inchikey: InChIKey of the molecule (27 characters)
        checks: List of check names to run (None = all checks)

    Returns:
        Cache key string, or None if inchikey is None/empty

    Example:
        >>> validation_cache_key("BSYNRYMUTXBXSQ-UHFFFAOYSA-N", ["parsability", "valence"])
        "validation:BSYNRYMUTXBXSQ-UHFFFAOYSA-N:a1b2c3d4"
    """
    if not inchikey:
        return None

    # Normalize checks to a consistent sorted string for hashing
    if checks is None or "all" in checks:
        checks_str = "all"
    else:
        checks_str = ",".join(sorted(checks))

    # Short hash of checks for key uniqueness (SHA256 for security)
    checks_hash = hashlib.sha256(checks_str.encode()).hexdigest()[:16]

    return f"validation:{inchikey}:{checks_hash}"


async def get_cached_validation(
    redis: Redis,
    cache_key: Optional[str],
) -> Optional[Dict[str, Any]]:
    """
    Retrieve cached validation result.

    Args:
        redis: Redis async client
        cache_key: Cache key from validation_cache_key()

    Returns:
        Cached validation result dict, or None if not found/expired

    Note:
        Returns None if cache_key is None (graceful handling of invalid molecules)
    """
    if cache_key is None:
        return None

    try:
        cached = await redis.get(cache_key)
        if cached:
            return json.loads(cached)
    except Exception as e:
        logger.warning("Cache get failed for key %s: %s", cache_key, e)

    return None


async def set_cached_validation(
    redis: Redis,
    cache_key: Optional[str],
    result: Dict[str, Any],
    ttl: Optional[int] = None,
) -> bool:
    """
    Store validation result in cache.

    Args:
        redis: Redis async client
        cache_key: Cache key from validation_cache_key()
        result: Validation result to cache (must be JSON-serializable)
        ttl: Time-to-live in seconds (default: settings.VALIDATION_CACHE_TTL)

    Returns:
        True if cached successfully, False otherwise

    Note:
        Returns False if cache_key is None (graceful handling of invalid molecules)
    """
    if cache_key is None:
        return False

    if ttl is None:
        ttl = settings.VALIDATION_CACHE_TTL

    try:
        serialized = json.dumps(result)
        await redis.setex(cache_key, ttl, serialized)
        return True
    except Exception as e:
        logger.warning("Cache set failed for key %s: %s", cache_key, e)

    return False


async def invalidate_cached_validation(
    redis: Redis,
    inchikey: str,
) -> int:
    """
    Invalidate all cached validations for a molecule.

    Useful when validation logic changes and old cache entries are stale.

    Args:
        redis: Redis async client
        inchikey: InChIKey of the molecule

    Returns:
        Number of keys deleted
    """
    if not inchikey:
        return 0

    try:
        # Find all cache keys for this inchikey
        pattern = f"validation:{inchikey}:*"
        keys = []
        async for key in redis.scan_iter(pattern):
            keys.append(key)

        if keys:
            return await redis.delete(*keys)
    except Exception as e:
        logger.warning("Cache invalidation failed for inchikey %s: %s", inchikey, e)

    return 0


# Convenience function for cache statistics
async def get_cache_stats(redis: Redis) -> Dict[str, Any]:
    """
    Get basic cache statistics.

    Returns:
        Dict with key_count, memory_used (approximate)
    """
    try:
        # Count validation keys
        key_count = 0
        async for _ in redis.scan_iter("validation:*"):
            key_count += 1

        # Get memory info
        info = await redis.info("memory")
        memory_used = info.get("used_memory", 0)

        return {
            "validation_keys": key_count,
            "memory_used_bytes": memory_used,
            "memory_used_human": info.get("used_memory_human", "unknown"),
        }
    except Exception:
        return {"error": "Could not retrieve cache stats"}
