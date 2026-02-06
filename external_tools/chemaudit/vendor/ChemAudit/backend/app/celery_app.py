"""
Celery Application Configuration

Configures Celery for batch processing of molecules with Redis as broker/backend.
Implements priority queues for handling concurrent jobs of different sizes.
"""

from celery import Celery
from kombu import Exchange, Queue

from app.core.config import settings

celery_app = Celery(
    "chemaudit",
    broker=settings.REDIS_URL,
    backend=settings.REDIS_URL,
    include=["app.services.batch.tasks"],
)

# Define exchanges and queues for priority-based routing
default_exchange = Exchange("default", type="direct")
priority_exchange = Exchange("priority", type="direct")

celery_app.conf.update(
    task_serializer="json",
    result_serializer="json",
    accept_content=["json"],
    result_expires=3600,  # Results expire after 1 hour
    task_track_started=True,
    worker_prefetch_multiplier=1,  # Process one task at a time for accurate progress
    task_acks_late=True,  # Acknowledge tasks after completion for reliability
    task_reject_on_worker_lost=True,  # Requeue tasks if worker dies
    # Queue definitions
    task_queues=(
        Queue("default", default_exchange, routing_key="default"),
        Queue("high_priority", priority_exchange, routing_key="high_priority"),
    ),
    task_default_queue="default",
    task_default_exchange="default",
    task_default_routing_key="default",
    # Route tasks based on their queue argument
    task_routes={
        # Single molecule validation - always high priority
        "app.services.batch.tasks.validate_single_molecule": {
            "queue": "high_priority",
        },
        # Batch processing - large jobs
        "app.services.batch.tasks.process_molecule_chunk": {
            "queue": "default",
        },
        "app.services.batch.tasks.aggregate_batch_results": {
            "queue": "default",
        },
        # Batch processing - small jobs (priority)
        "app.services.batch.tasks.process_molecule_chunk_priority": {
            "queue": "high_priority",
        },
        "app.services.batch.tasks.aggregate_batch_results_priority": {
            "queue": "high_priority",
        },
    },
)

# Threshold for small vs large jobs (molecules)
SMALL_JOB_THRESHOLD = 500
