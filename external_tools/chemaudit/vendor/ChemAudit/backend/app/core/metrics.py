"""
Prometheus metrics for ChemAudit monitoring.

Provides custom metrics for validation performance, cache efficiency,
batch processing, and alert matching. These metrics enable:

- Performance monitoring (latency, throughput)
- Resource utilization (batch sizes, active jobs)
- Cache efficiency (hit/miss rates)
- Alert pattern analysis (match counts by type)

Usage:
    from app.core.metrics import (
        VALIDATION_DURATION,
        MOLECULES_PROCESSED,
        record_validation,
    )

    # Record validation timing
    with VALIDATION_DURATION.time():
        result = validate(mol)

    # Or use convenience function
    record_validation(duration=0.5, status="success", batch_size=100)
"""

from prometheus_client import Counter, Gauge, Histogram, Info

from app.core.config import settings

# Application info metric
APP_INFO = Info(
    "chemaudit",
    "ChemAudit application information",
)
APP_INFO.info(
    {
        "version": settings.APP_VERSION,
        "app_name": settings.APP_NAME,
    }
)

# Validation timing histogram
# Buckets chosen for typical validation times: 10ms to 30s
VALIDATION_DURATION = Histogram(
    "chemaudit_validation_duration_seconds",
    "Time spent validating molecules",
    ["validation_type"],  # single, batch
    buckets=(0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0),
)

# Molecules processed counter with status label
MOLECULES_PROCESSED = Counter(
    "chemaudit_molecules_processed_total",
    "Total number of molecules processed",
    ["status"],  # success, error, invalid
)

# Batch size histogram
# Buckets for typical batch sizes: 1 to 10000
BATCH_SIZE = Histogram(
    "chemaudit_batch_size",
    "Distribution of batch job sizes",
    buckets=(1, 10, 50, 100, 250, 500, 1000, 2500, 5000, 10000),
)

# Cache metrics
CACHE_HITS = Counter(
    "chemaudit_cache_hits_total",
    "Total number of cache hits",
)

CACHE_MISSES = Counter(
    "chemaudit_cache_misses_total",
    "Total number of cache misses",
)

# Active batch jobs gauge
ACTIVE_BATCH_JOBS = Gauge(
    "chemaudit_active_batch_jobs",
    "Number of currently active batch processing jobs",
)

# Alert matches counter by alert type
ALERT_MATCHES = Counter(
    "chemaudit_alert_matches_total",
    "Total number of structural alert matches",
    ["alert_type"],  # PAINS, BRENK, NIH, ZINC
)

# Standardization counter
STANDARDIZATIONS_PERFORMED = Counter(
    "chemaudit_standardizations_total",
    "Total number of molecule standardizations performed",
    ["status"],  # success, error
)

# External API calls
EXTERNAL_API_CALLS = Counter(
    "chemaudit_external_api_calls_total",
    "Total number of external API calls",
    ["api", "status"],  # api: pubchem, chembl, coconut; status: success, error
)

EXTERNAL_API_DURATION = Histogram(
    "chemaudit_external_api_duration_seconds",
    "Time spent calling external APIs",
    ["api"],
    buckets=(0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0),
)


# Convenience functions for recording metrics
def record_validation(
    duration: float,
    status: str = "success",
    validation_type: str = "single",
    batch_size: int = 1,
) -> None:
    """
    Record a validation operation in metrics.

    Args:
        duration: Time taken in seconds
        status: Result status (success, error, invalid)
        validation_type: Type of validation (single, batch)
        batch_size: Number of molecules in batch (default 1)
    """
    VALIDATION_DURATION.labels(validation_type=validation_type).observe(duration)
    MOLECULES_PROCESSED.labels(status=status).inc(batch_size)

    if batch_size > 1:
        BATCH_SIZE.observe(batch_size)


def record_cache_access(hit: bool) -> None:
    """
    Record a cache access (hit or miss).

    Args:
        hit: True if cache hit, False if miss
    """
    if hit:
        CACHE_HITS.inc()
    else:
        CACHE_MISSES.inc()


def record_batch_job_start() -> None:
    """Record start of a batch job."""
    ACTIVE_BATCH_JOBS.inc()


def record_batch_job_end() -> None:
    """Record end of a batch job."""
    ACTIVE_BATCH_JOBS.dec()


def record_alert_match(alert_type: str, count: int = 1) -> None:
    """
    Record structural alert matches.

    Args:
        alert_type: Type of alert (PAINS, BRENK, NIH, ZINC)
        count: Number of matches to record
    """
    ALERT_MATCHES.labels(alert_type=alert_type).inc(count)


def record_standardization(status: str = "success") -> None:
    """
    Record a standardization operation.

    Args:
        status: Result status (success, error)
    """
    STANDARDIZATIONS_PERFORMED.labels(status=status).inc()


def record_external_api_call(
    api: str,
    status: str,
    duration: float | None = None,
) -> None:
    """
    Record an external API call.

    Args:
        api: API name (pubchem, chembl, coconut)
        status: Result status (success, error)
        duration: Request duration in seconds (optional)
    """
    EXTERNAL_API_CALLS.labels(api=api, status=status).inc()
    if duration is not None:
        EXTERNAL_API_DURATION.labels(api=api).observe(duration)
