"""
API Key schemas for request/response validation.
"""

from typing import Optional

from pydantic import BaseModel, Field


class APIKeyCreate(BaseModel):
    """Request to create a new API key."""

    name: str = Field(
        ..., description="Name/label for this API key", min_length=1, max_length=100
    )
    description: Optional[str] = Field(
        None, description="Optional description", max_length=500
    )
    expiry_days: Optional[int] = Field(
        None,
        description="Days until key expires (default: 90, max: 365, 0 for no expiry)",
        ge=0,
        le=365,
    )


class APIKeyResponse(BaseModel):
    """Response when creating an API key (includes full key - shown only once)."""

    key: str = Field(
        ..., description="The API key - save this, it won't be shown again"
    )
    name: str = Field(..., description="Name of the API key")
    created_at: str = Field(..., description="Creation timestamp")
    expires_at: Optional[str] = Field(None, description="Expiration timestamp")


class APIKeyInfo(BaseModel):
    """API key metadata (without the actual key)."""

    key_id: str = Field(..., description="Unique identifier (hash prefix)")
    name: str = Field(..., description="Name of the API key")
    description: Optional[str] = Field(None, description="Description")
    created_at: str = Field(..., description="Creation timestamp")
    last_used: Optional[str] = Field(None, description="Last used timestamp")
    request_count: int = Field(0, description="Total requests made with this key")
    expires_at: Optional[str] = Field(None, description="Expiration timestamp")
    is_expired: bool = Field(False, description="Whether the key has expired")


class APIKeyUsage(BaseModel):
    """API key usage statistics."""

    request_count: int = Field(..., description="Total requests")
    last_used: Optional[str] = Field(None, description="Last request timestamp")
    rate_limit_tier: str = Field(..., description="Rate limit tier")
