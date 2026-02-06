"""
ChemAudit API - Chemical Structure Validation Suite
"""

from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware

from app.api.routes import (
    alerts,
    api_keys,
    batch,
    config,
    export,
    health,
    integrations,
    scoring,
    standardization,
    validation,
)
from app.core.config import settings
from app.core.exceptions import (
    ChemAuditException,
    chemaudit_exception_handler,
    generic_exception_handler,
)
from app.core.rate_limit import (
    check_ip_ban_middleware,
    limiter,
    rate_limit_exceeded_handler,
)
from app.core.security import generate_csrf_token, verify_csrf_token
from app.websockets import manager

# Conditional Prometheus imports
if settings.ENABLE_METRICS:
    from prometheus_fastapi_instrumentator import Instrumentator


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan manager.

    Handles startup and shutdown events.
    """
    # Startup
    print(f"Starting {settings.APP_NAME} v{settings.APP_VERSION}")

    # Initialize WebSocket manager Redis connection
    await manager.init_redis()
    print("WebSocket manager initialized with Redis")

    yield

    # Shutdown
    await manager.close_redis()
    print("Shutting down...")


app = FastAPI(
    title="ChemAudit API",
    description="Chemical Structure Validation and Standardization API",
    version=settings.APP_VERSION,
    docs_url="/api/v1/docs",
    redoc_url="/api/v1/redoc",
    openapi_url="/api/v1/openapi.json",
    lifespan=lifespan,
)

# Add rate limiter to app state
app.state.limiter = limiter

# Configure CORS with explicit methods and headers (not wildcards)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=settings.CORS_ALLOW_METHODS,
    allow_headers=settings.CORS_ALLOW_HEADERS,
)

# Add SlowAPI middleware for rate limiting
app.add_middleware(SlowAPIMiddleware)


# =============================================================================
# Security Middleware
# =============================================================================


@app.middleware("http")
async def security_middleware(request: Request, call_next):
    """
    Security middleware that runs on every request.

    - Checks IP ban status
    - Validates CSRF tokens for browser requests
    """
    # Check IP ban status early
    try:
        check_ip_ban_middleware(request)
    except Exception as e:
        # If IP is banned, return error
        if hasattr(e, "status_code") and e.status_code == 403:
            return JSONResponse(
                status_code=403,
                content=e.detail if hasattr(e, "detail") else {"error": "ip_banned"},
            )
        raise

    # CSRF validation for state-changing requests from browsers
    # Skip for:
    # - Safe methods (GET, HEAD, OPTIONS)
    # - API key authenticated requests
    # - WebSocket connections
    # - Health check endpoints
    if (
        request.method not in ("GET", "HEAD", "OPTIONS")
        and not request.headers.get("X-API-Key")
        and not request.url.path.startswith("/ws/")
        and not request.url.path.endswith("/health")
        and not request.url.path == "/api/v1/csrf-token"
    ):
        # Check for CSRF token in header
        csrf_token = request.headers.get("X-CSRF-Token")

        # For browser requests (with cookies/credentials), require CSRF token
        # Check for origin header to identify browser requests
        origin = request.headers.get("origin")
        if origin and origin in settings.CORS_ORIGINS:
            if not csrf_token or not verify_csrf_token(csrf_token):
                return JSONResponse(
                    status_code=403,
                    content={
                        "error": "csrf_validation_failed",
                        "message": "Invalid or missing CSRF token",
                    },
                )

    response = await call_next(request)
    return response


# Register exception handlers
app.add_exception_handler(RateLimitExceeded, rate_limit_exceeded_handler)
app.add_exception_handler(ChemAuditException, chemaudit_exception_handler)
app.add_exception_handler(Exception, generic_exception_handler)

# Include routers
app.include_router(health.router, prefix="/api/v1", tags=["health"])
app.include_router(validation.router, prefix="/api/v1", tags=["validation"])
app.include_router(alerts.router, prefix="/api/v1", tags=["alerts"])
app.include_router(standardization.router, prefix="/api/v1", tags=["standardization"])
app.include_router(scoring.router, prefix="/api/v1", tags=["scoring"])
app.include_router(batch.router, prefix="/api/v1", tags=["batch"])
app.include_router(export.router, prefix="/api/v1", tags=["export"])
app.include_router(api_keys.router, prefix="/api/v1", tags=["api-keys"])
app.include_router(integrations.router, prefix="/api/v1", tags=["integrations"])
app.include_router(config.router, prefix="/api/v1", tags=["config"])

# Set up Prometheus metrics if enabled
if settings.ENABLE_METRICS:
    instrumentator = Instrumentator(
        should_group_status_codes=False,
        should_ignore_untemplated=True,
        should_respect_env_var=True,
        should_instrument_requests_inprogress=True,
        excluded_handlers=["/metrics", "/health", "/api/v1/health"],
        inprogress_name="chemaudit_inprogress_requests",
        inprogress_labels=True,
    )
    instrumentator.instrument(app).expose(app, endpoint="/metrics")


# =============================================================================
# CSRF Token Endpoint
# =============================================================================


@app.get("/api/v1/csrf-token")
async def get_csrf_token():
    """
    Get a CSRF token for browser-based requests.

    Frontend should call this endpoint on page load and include
    the token in X-CSRF-Token header for all state-changing requests.

    Returns:
        JSON with csrf_token field
    """
    token = generate_csrf_token()
    return {"csrf_token": token}


# =============================================================================
# WebSocket Endpoint
# =============================================================================


@app.websocket("/ws/batch/{job_id}")
async def batch_progress_websocket(websocket: WebSocket, job_id: str):
    """
    WebSocket endpoint for real-time batch progress updates.

    Connect to /ws/batch/{job_id} after uploading a file to receive
    progress updates in real-time.

    Message format:
    {
        "job_id": "...",
        "status": "processing|complete|failed|cancelled",
        "progress": 0-100,
        "processed": int,
        "total": int,
        "eta_seconds": int|null
    }
    """
    # Connect and wait for subscription to be ready before sending initial status
    connected = await manager.connect(job_id, websocket)
    if not connected:
        return

    # Send initial status after subscription is ready
    await manager.send_initial_status(job_id, websocket)

    try:
        # Keep connection alive, waiting for close
        while True:
            try:
                # Wait for client messages (pings, close)
                data = await websocket.receive_text()
                # Client can send "ping" to keep alive
                if data == "ping":
                    await websocket.send_text("pong")
            except WebSocketDisconnect:
                break
    finally:
        manager.disconnect(job_id, websocket)


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "name": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "status": "running",
    }
