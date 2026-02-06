"""
Configuration endpoint for ChemAudit API.

Exposes deployment limits to the frontend for dynamic UI configuration.
"""

from fastapi import APIRouter

from app.core.config import settings
from app.schemas.config import ConfigResponse, DeploymentLimits

router = APIRouter()


@router.get(
    "/config",
    response_model=ConfigResponse,
    summary="Get application configuration",
    description="Returns public configuration including deployment limits for frontend use.",
)
async def get_config() -> ConfigResponse:
    """
    Get public application configuration.

    Returns deployment limits based on the current profile, allowing
    the frontend to dynamically adjust validation limits and display
    appropriate messages.
    """
    return ConfigResponse(
        app_name=settings.APP_NAME,
        app_version=settings.APP_VERSION,
        deployment_profile=settings.DEPLOYMENT_PROFILE,
        limits=DeploymentLimits(
            max_batch_size=settings.MAX_BATCH_SIZE,
            max_file_size_mb=settings.MAX_FILE_SIZE_MB,
        ),
    )
