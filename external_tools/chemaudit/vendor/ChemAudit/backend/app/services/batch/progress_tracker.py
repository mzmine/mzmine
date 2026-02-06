"""
Progress Tracker Module

Tracks batch job progress in Redis with throttling to prevent message flooding.
Uses moving average for ETA calculation.
"""

import json
import logging
import time
from collections import deque
from dataclasses import asdict, dataclass
from typing import Optional

import redis

from app.core.config import settings

logger = logging.getLogger(__name__)


@dataclass
class ProgressInfo:
    """Progress information for a batch job."""

    job_id: str
    status: str  # pending, processing, complete, failed, cancelled
    progress: int  # 0-100 percentage
    processed: int  # Number of molecules processed
    total: int  # Total number of molecules
    eta_seconds: Optional[int] = None  # Estimated time remaining
    started_at: Optional[float] = None
    completed_at: Optional[float] = None
    error_message: Optional[str] = None


class ProgressTracker:
    """
    Tracks and broadcasts batch processing progress.

    Features:
    - Redis-backed progress storage
    - Throttled updates (max 2/second per job)
    - ETA calculation using moving average
    - Pub/sub for real-time WebSocket forwarding
    """

    UPDATE_INTERVAL = 0.5  # Minimum seconds between updates (max 2/second)
    ETA_WINDOW_SIZE = 10  # Number of chunks to average for ETA

    def __init__(self, redis_url: str = None):
        self._redis_url = redis_url or settings.REDIS_URL
        self._redis: Optional[redis.Redis] = None
        self._last_update_times: dict[str, float] = {}
        self._chunk_times: dict[str, deque] = {}

    def _get_redis(self) -> redis.Redis:
        """Get or create Redis connection."""
        if self._redis is None:
            self._redis = redis.from_url(self._redis_url)
        return self._redis

    def init_job(self, job_id: str, total: int) -> None:
        """
        Initialize a new batch job.

        Args:
            job_id: Unique job identifier
            total: Total number of molecules to process
        """
        r = self._get_redis()
        progress = ProgressInfo(
            job_id=job_id,
            status="pending",
            progress=0,
            processed=0,
            total=total,
            started_at=time.time(),
        )
        r.set(f"batch:job:{job_id}", json.dumps(asdict(progress)), ex=3600)
        # Initialize atomic counter for parallel chunk processing
        r.set(f"batch:counter:{job_id}", 0, ex=3600)
        self._chunk_times[job_id] = deque(maxlen=self.ETA_WINDOW_SIZE)
        self._last_update_times[job_id] = 0

    def increment_processed(self, job_id: str, count: int = 1) -> int:
        """
        Atomically increment processed count for parallel chunk processing.

        Args:
            job_id: Job identifier
            count: Number to increment by

        Returns:
            New processed count after increment
        """
        r = self._get_redis()
        return r.incrby(f"batch:counter:{job_id}", count)

    def update_progress(
        self,
        job_id: str,
        processed: int,
        total: int,
        status: str = "processing",
        force: bool = False,
    ) -> bool:
        """
        Update job progress with throttling.

        Args:
            job_id: Job identifier
            processed: Number of molecules processed so far
            total: Total number of molecules
            status: Current status
            force: If True, bypass throttling

        Returns:
            True if update was sent, False if throttled
        """
        current_time = time.time()

        # Throttle updates unless forced
        if not force:
            last_update = self._last_update_times.get(job_id, 0)
            if current_time - last_update < self.UPDATE_INTERVAL:
                return False

        self._last_update_times[job_id] = current_time

        # Calculate ETA
        eta_seconds = self._calculate_eta(job_id, processed, total, current_time)

        # Calculate progress percentage
        progress_pct = int((processed / total) * 100) if total > 0 else 0

        r = self._get_redis()

        progress = ProgressInfo(
            job_id=job_id,
            status=status,
            progress=progress_pct,
            processed=processed,
            total=total,
            eta_seconds=eta_seconds,
        )

        # Store in Redis
        r.set(f"batch:job:{job_id}", json.dumps(asdict(progress)), ex=3600)

        # Publish for WebSocket forwarding
        r.publish(f"batch:progress:{job_id}", json.dumps(asdict(progress)))

        return True

    def _calculate_eta(
        self, job_id: str, processed: int, total: int, current_time: float
    ) -> Optional[int]:
        """Calculate ETA using moving average of recent processing times."""
        if job_id not in self._chunk_times:
            self._chunk_times[job_id] = deque(maxlen=self.ETA_WINDOW_SIZE)

        chunk_times = self._chunk_times[job_id]

        if len(chunk_times) == 0:
            # First update - record timestamp
            chunk_times.append((processed, current_time))
            return None

        # Record current progress
        chunk_times.append((processed, current_time))

        if len(chunk_times) < 2:
            return None

        # Calculate average processing rate over recent chunks
        first_processed, first_time = chunk_times[0]
        molecules_in_window = processed - first_processed
        time_in_window = current_time - first_time

        if molecules_in_window <= 0 or time_in_window <= 0:
            return None

        rate = molecules_in_window / time_in_window  # molecules/second
        remaining = total - processed

        if rate > 0:
            return int(remaining / rate)

        return None

    def mark_complete(self, job_id: str) -> None:
        """Mark a job as complete."""
        r = self._get_redis()

        # Get current progress
        data = r.get(f"batch:job:{job_id}")
        if data:
            progress = json.loads(data)
            progress["status"] = "complete"
            progress["progress"] = 100
            progress["completed_at"] = time.time()
            progress["eta_seconds"] = 0

            r.set(f"batch:job:{job_id}", json.dumps(progress), ex=3600)

            # Publish completion message
            publish_count = r.publish(f"batch:progress:{job_id}", json.dumps(progress))
            logger.info(
                f"Job {job_id} marked complete. "
                f"Published to {publish_count} subscriber(s). "
                f"Processed: {progress.get('processed')}/{progress.get('total')}"
            )
        else:
            logger.warning(
                f"Cannot mark job {job_id} as complete: no progress data found"
            )

        # Cleanup
        self._cleanup_job(job_id)

    def mark_failed(self, job_id: str, error_message: str) -> None:
        """Mark a job as failed."""
        r = self._get_redis()

        data = r.get(f"batch:job:{job_id}")
        if data:
            progress = json.loads(data)
            progress["status"] = "failed"
            progress["error_message"] = error_message
            progress["completed_at"] = time.time()

            r.set(f"batch:job:{job_id}", json.dumps(progress), ex=3600)
            r.publish(f"batch:progress:{job_id}", json.dumps(progress))

        self._cleanup_job(job_id)

    def mark_cancelled(self, job_id: str) -> None:
        """Mark a job as cancelled."""
        r = self._get_redis()

        data = r.get(f"batch:job:{job_id}")
        if data:
            progress = json.loads(data)
            progress["status"] = "cancelled"
            progress["completed_at"] = time.time()

            r.set(f"batch:job:{job_id}", json.dumps(progress), ex=3600)
            r.publish(f"batch:progress:{job_id}", json.dumps(progress))

        self._cleanup_job(job_id)

    def get_progress(self, job_id: str) -> Optional[ProgressInfo]:
        """Get current progress for a job."""
        r = self._get_redis()
        data = r.get(f"batch:job:{job_id}")
        if data:
            d = json.loads(data)
            return ProgressInfo(**d)
        return None

    def _cleanup_job(self, job_id: str) -> None:
        """Clean up in-memory tracking for a job."""
        self._last_update_times.pop(job_id, None)
        self._chunk_times.pop(job_id, None)
        # Clean up atomic counter
        try:
            r = self._get_redis()
            r.delete(f"batch:counter:{job_id}")
        except Exception:
            pass  # Ignore cleanup errors


# Singleton instance
progress_tracker = ProgressTracker()
