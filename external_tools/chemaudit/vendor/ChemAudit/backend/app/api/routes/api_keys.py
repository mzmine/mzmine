"""
API Key Management Routes

Endpoints for creating, listing, and revoking API keys.
All endpoints require admin authentication via X-Admin-Secret header.
"""

import secrets
from datetime import datetime, timezone
from typing import List

from fastapi import APIRouter, Depends, HTTPException, status

from app.core.security import (
    calculate_expiry_date,
    get_redis_client,
    hash_api_key_for_lookup,
    is_key_expired,
    require_admin_auth,
)
from app.schemas.api_key import APIKeyCreate, APIKeyInfo, APIKeyResponse

router = APIRouter()


@router.post(
    "/api-keys",
    response_model=APIKeyResponse,
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(require_admin_auth)],
)
async def create_api_key(request: APIKeyCreate):
    """
    Create a new API key.

    Requires admin authentication via X-Admin-Secret header.

    The full API key is returned only in this response. Store it securely.
    Only the hash is stored in Redis, not the plain key.

    Args:
        request: API key creation request

    Returns:
        APIKeyResponse with the full API key (shown only once)
    """
    # Generate secure API key
    api_key = secrets.token_urlsafe(32)
    key_hash = hash_api_key_for_lookup(api_key)
    created_at = datetime.now(timezone.utc).isoformat()

    # Calculate expiry date
    expires_at = calculate_expiry_date(request.expiry_days)

    # Store in Redis
    client = await get_redis_client()
    try:
        # Store key metadata
        mapping = {
            "name": request.name,
            "description": request.description or "",
            "created_at": created_at,
            "last_used": "",
            "request_count": "0",
        }
        if expires_at:
            mapping["expires_at"] = expires_at

        await client.hset(f"apikey:{key_hash}", mapping=mapping)
        # Add to index for listing
        await client.sadd("apikey:index", key_hash)
    finally:
        await client.aclose()

    return APIKeyResponse(
        key=api_key,
        name=request.name,
        created_at=created_at,
        expires_at=expires_at,
    )


@router.get(
    "/api-keys",
    response_model=List[APIKeyInfo],
    dependencies=[Depends(require_admin_auth)],
)
async def list_api_keys():
    """
    List all API keys (metadata only, not the actual keys).

    Requires admin authentication via X-Admin-Secret header.

    Returns:
        List of API key metadata
    """
    client = await get_redis_client()
    try:
        # Get all key hashes from index
        key_hashes = await client.smembers("apikey:index")

        keys_info = []
        for key_hash in key_hashes:
            key_data = await client.hgetall(f"apikey:{key_hash}")
            if key_data:
                keys_info.append(
                    APIKeyInfo(
                        key_id=key_hash[:12],  # Show first 12 chars as identifier
                        name=key_data.get("name", ""),
                        description=key_data.get("description") or None,
                        created_at=key_data.get("created_at", ""),
                        last_used=key_data.get("last_used") or None,
                        request_count=int(key_data.get("request_count", 0)),
                        expires_at=key_data.get("expires_at") or None,
                        is_expired=is_key_expired(key_data),
                    )
                )

        return keys_info
    finally:
        await client.aclose()


@router.delete(
    "/api-keys/{key_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    dependencies=[Depends(require_admin_auth)],
)
async def revoke_api_key(key_id: str):
    """
    Revoke an API key.

    Requires admin authentication via X-Admin-Secret header.

    Args:
        key_id: The key hash prefix (first 12 characters)

    Raises:
        HTTPException: 404 if key not found
    """
    client = await get_redis_client()
    try:
        # Find matching key hash
        key_hashes = await client.smembers("apikey:index")
        matching_hash = None
        for key_hash in key_hashes:
            if key_hash.startswith(key_id):
                matching_hash = key_hash
                break

        if not matching_hash:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="API key not found"
            )

        # Delete key data and remove from index
        await client.delete(f"apikey:{matching_hash}")
        await client.srem("apikey:index", matching_hash)

        return None
    finally:
        await client.aclose()
