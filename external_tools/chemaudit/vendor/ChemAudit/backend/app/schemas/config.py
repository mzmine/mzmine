"""
Configuration response schemas for ChemAudit API.
"""

from pydantic import BaseModel, Field, computed_field


class DeploymentLimits(BaseModel):
    """Deployment limits based on the selected profile."""

    max_batch_size: int = Field(description="Maximum number of molecules per batch job")
    max_file_size_mb: int = Field(description="Maximum file upload size in megabytes")

    @computed_field
    @property
    def max_file_size_bytes(self) -> int:
        """Maximum file upload size in bytes (computed from MB)."""
        return self.max_file_size_mb * 1024 * 1024


class ConfigResponse(BaseModel):
    """Public configuration response for frontend consumption."""

    app_name: str = Field(description="Application name")
    app_version: str = Field(description="Application version")
    deployment_profile: str = Field(
        description="Current deployment profile (small, medium, large, xl, coconut)"
    )
    limits: DeploymentLimits = Field(description="Deployment limits")

    model_config = {
        "json_schema_extra": {
            "example": {
                "app_name": "ChemAudit",
                "app_version": "1.0.0",
                "deployment_profile": "medium",
                "limits": {
                    "max_batch_size": 10000,
                    "max_file_size_mb": 500,
                    "max_file_size_bytes": 524288000,
                },
            }
        }
    }
