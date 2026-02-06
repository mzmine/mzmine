"""
API Key authentication, validation, and security utilities.

Provides:
- Argon2 password hashing for API keys
- Admin authentication for key management
- CSRF token generation and verification
- API key expiration checking
"""

import asyncio
import hashlib
import hmac
import logging
import secrets
import time
from datetime import datetime, timedelta, timezone
from typing import Optional

import redis.asyncio as redis
from argon2 import PasswordHasher
from argon2.exceptions import InvalidHashError, VerifyMismatchError
from fastapi import Header, HTTPException, Request, Security, status
from fastapi.security import APIKeyHeader

from app.core.config import settings

logger = logging.getLogger(__name__)

# Argon2 hasher configuration (secure defaults)
ph = PasswordHasher(
    time_cost=3,  # Number of iterations
    memory_cost=65536,  # 64MB memory
    parallelism=4,  # 4 parallel threads
    hash_len=32,  # Output hash length
    salt_len=16,  # Salt length
)

api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)
admin_secret_header = APIKeyHeader(name="X-Admin-Secret", auto_error=False)


async def get_redis_client():
    """Get async Redis client."""
    return redis.from_url(settings.REDIS_URL, decode_responses=True)


def hash_api_key(key: str) -> str:
    """
    Hash API key using Argon2 for secure storage.

    Args:
        key: The plain API key to hash

    Returns:
        Argon2 hash string
    """
    return ph.hash(key)


def hash_api_key_for_lookup(key: str) -> str:
    """
    Create a fast hash for API key lookup (not for storage).

    Uses SHA256 for fast lookups. The actual key verification
    uses Argon2 for security.

    Args:
        key: The plain API key

    Returns:
        SHA256 hex digest for use as Redis key
    """
    return hashlib.sha256(key.encode()).hexdigest()


def verify_api_key_hash(key: str, stored_hash: str) -> bool:
    """
    Verify an API key against its Argon2 hash.

    Args:
        key: The plain API key to verify
        stored_hash: The stored Argon2 hash

    Returns:
        True if the key matches, False otherwise
    """
    try:
        ph.verify(stored_hash, key)
        return True
    except (VerifyMismatchError, InvalidHashError):
        return False


async def validate_api_key(api_key: str) -> Optional[dict]:
    """
    Validate API key against Redis storage and check expiration.

    Args:
        api_key: The API key to validate

    Returns:
        Dictionary with key metadata if valid, None if invalid or expired
    """
    client = await get_redis_client()
    try:
        # Use fast hash for lookup
        key_hash = hash_api_key_for_lookup(api_key)
        key_data = await client.hgetall(f"apikey:{key_hash}")

        if not key_data:
            return None

        # Check if key has expired
        expires_at = key_data.get("expires_at")
        if expires_at:
            try:
                exp_time = datetime.fromisoformat(expires_at)
                if datetime.now(timezone.utc) > exp_time:
                    logger.info(f"API key expired: {key_hash[:12]}...")
                    return None
            except ValueError:
                pass  # Invalid date format, treat as non-expiring

        return dict(key_data)
    finally:
        await client.aclose()


def is_key_expired(key_data: dict) -> bool:
    """
    Check if an API key has expired.

    Args:
        key_data: The key metadata dictionary

    Returns:
        True if expired, False otherwise
    """
    expires_at = key_data.get("expires_at")
    if not expires_at:
        return False

    try:
        exp_time = datetime.fromisoformat(expires_at)
        return datetime.now(timezone.utc) > exp_time
    except ValueError:
        return False


def calculate_expiry_date(expiry_days: Optional[int] = None) -> Optional[str]:
    """
    Calculate API key expiry date.

    Args:
        expiry_days: Number of days until expiry, or None for default

    Returns:
        ISO format expiry date string, or None if no expiry
    """
    if expiry_days is None:
        expiry_days = settings.API_KEY_DEFAULT_EXPIRY_DAYS

    if expiry_days <= 0:
        return None  # No expiry

    # Cap at maximum expiry
    expiry_days = min(expiry_days, settings.API_KEY_MAX_EXPIRY_DAYS)

    expiry_date = datetime.now(timezone.utc) + timedelta(days=expiry_days)
    return expiry_date.isoformat()


async def get_api_key(
    request: Request, api_key: Optional[str] = Security(api_key_header)
) -> Optional[str]:
    """
    Validate API key if provided. Returns None for anonymous access.

    This dependency allows both anonymous and authenticated access.
    Invalid or expired API keys result in 401 Unauthorized.

    Args:
        request: FastAPI request object
        api_key: API key from X-API-Key header

    Returns:
        API key if valid, None if not provided

    Raises:
        HTTPException: 401 if API key is invalid or expired
    """
    if api_key is None:
        return None  # Anonymous access allowed

    key_data = await validate_api_key(api_key)
    if key_data is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired API key",
            headers={"WWW-Authenticate": "ApiKey"},
        )

    # Update usage stats (async, don't wait) with error handling
    task = asyncio.create_task(_update_usage_stats(api_key))
    task.add_done_callback(_handle_task_exception)

    return api_key


def _handle_task_exception(task: asyncio.Task) -> None:
    """Handle exceptions from background tasks."""
    if task.cancelled():
        return
    exc = task.exception()
    if exc is not None:
        logger.warning("Background task failed: %s", exc)


async def _update_usage_stats(api_key: str):
    """
    Update API key usage statistics.

    Increments request count and updates last_used timestamp.
    """
    client = await get_redis_client()
    try:
        key_hash = hash_api_key_for_lookup(api_key)
        await client.hset(
            f"apikey:{key_hash}", "last_used", datetime.now(timezone.utc).isoformat()
        )
        await client.hincrby(f"apikey:{key_hash}", "request_count", 1)
    finally:
        await client.aclose()


# =============================================================================
# Admin Authentication
# =============================================================================


async def require_admin_auth(
    admin_secret: Optional[str] = Security(admin_secret_header),
) -> bool:
    """
    Require admin authentication for sensitive operations.

    Used to protect API key management endpoints.

    Args:
        admin_secret: The admin secret from X-Admin-Secret header

    Returns:
        True if authenticated

    Raises:
        HTTPException: 401 if not authenticated or invalid secret
    """
    if not admin_secret:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Admin authentication required",
            headers={"WWW-Authenticate": "AdminSecret"},
        )

    # Constant-time comparison to prevent timing attacks
    if not secrets.compare_digest(admin_secret, settings.API_KEY_ADMIN_SECRET):
        logger.warning("Failed admin authentication attempt")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid admin credentials",
            headers={"WWW-Authenticate": "AdminSecret"},
        )

    return True


# =============================================================================
# CSRF Protection
# =============================================================================


def generate_csrf_token() -> str:
    """
    Generate a secure CSRF token.

    Returns:
        A secure random token string
    """
    # Generate random data
    random_bytes = secrets.token_bytes(32)
    timestamp = str(int(time.time())).encode()

    # Create HMAC signature
    signature = hmac.new(
        settings.CSRF_SECRET_KEY.encode(),
        random_bytes + timestamp,
        hashlib.sha256,
    ).hexdigest()

    # Combine into token
    token = f"{random_bytes.hex()}.{timestamp.decode()}.{signature}"
    return token


def verify_csrf_token(token: str, max_age_seconds: int = 3600) -> bool:
    """
    Verify a CSRF token.

    Args:
        token: The token to verify
        max_age_seconds: Maximum token age in seconds (default 1 hour)

    Returns:
        True if valid, False otherwise
    """
    if not token:
        return False

    try:
        parts = token.split(".")
        if len(parts) != 3:
            return False

        random_hex, timestamp_str, provided_signature = parts

        # Verify timestamp
        timestamp = int(timestamp_str)
        if time.time() - timestamp > max_age_seconds:
            return False

        # Verify signature
        random_bytes = bytes.fromhex(random_hex)
        expected_signature = hmac.new(
            settings.CSRF_SECRET_KEY.encode(),
            random_bytes + timestamp_str.encode(),
            hashlib.sha256,
        ).hexdigest()

        return secrets.compare_digest(provided_signature, expected_signature)

    except (ValueError, TypeError):
        return False


async def verify_csrf_header(
    request: Request,
    x_csrf_token: Optional[str] = Header(None, alias="X-CSRF-Token"),
) -> bool:
    """
    Verify CSRF token from request header.

    Args:
        request: The FastAPI request
        x_csrf_token: The CSRF token from header

    Returns:
        True if valid

    Raises:
        HTTPException: 403 if token is invalid
    """
    # Skip CSRF for safe methods
    if request.method in ("GET", "HEAD", "OPTIONS"):
        return True

    # Skip CSRF if API key is present (API clients don't need CSRF)
    if request.headers.get("X-API-Key"):
        return True

    if not x_csrf_token or not verify_csrf_token(x_csrf_token):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Invalid or missing CSRF token",
        )

    return True
