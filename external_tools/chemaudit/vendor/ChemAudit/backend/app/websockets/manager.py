"""
WebSocket Connection Manager

Manages WebSocket connections with Redis pub/sub for horizontal scaling.
Forwards batch progress updates to connected clients.
"""

import asyncio
import json
import logging
from typing import Dict, List, Optional

import redis.asyncio as redis
from fastapi import WebSocket

from app.core.config import settings

logger = logging.getLogger(__name__)


class ConnectionManager:
    """
    Manages WebSocket connections with Redis pub/sub integration.

    Features:
    - Multiple clients can connect to same job_id
    - Redis pub/sub enables horizontal scaling across multiple backend instances
    - Automatic cleanup on disconnect
    - Proper subscription synchronization to prevent race conditions
    """

    def __init__(self):
        self.active_connections: Dict[str, List[WebSocket]] = {}
        self._redis: Optional[redis.Redis] = None
        self._redis_url = settings.REDIS_URL
        self._subscriber_tasks: Dict[str, asyncio.Task] = {}
        self._subscription_ready: Dict[str, asyncio.Event] = {}

    async def init_redis(self, redis_url: Optional[str] = None) -> None:
        """Initialize Redis connection for pub/sub."""
        url = redis_url or self._redis_url
        try:
            self._redis = redis.from_url(url, decode_responses=True)
            # Test connection
            await self._redis.ping()
            logger.info("WebSocket manager connected to Redis")
        except Exception as e:
            logger.error(f"Failed to connect to Redis: {e}")
            self._redis = None

    async def close_redis(self) -> None:
        """Close Redis connection."""
        if self._redis:
            await self._redis.close()
            self._redis = None

    async def _ensure_redis(self) -> bool:
        """Ensure Redis connection is available, attempt reconnect if not."""
        if self._redis is None:
            await self.init_redis()

        if self._redis is None:
            return False

        try:
            await self._redis.ping()
            return True
        except Exception as e:
            logger.warning(f"Redis connection lost, attempting reconnect: {e}")
            await self.init_redis()
            return self._redis is not None

    async def connect(self, job_id: str, websocket: WebSocket) -> bool:
        """
        Accept WebSocket connection and register for job updates.

        Args:
            job_id: Job identifier to subscribe to
            websocket: WebSocket connection to register

        Returns:
            True if connection was successful, False otherwise
        """
        try:
            await websocket.accept()
        except Exception as e:
            logger.error(f"Failed to accept WebSocket connection: {e}")
            return False

        if job_id not in self.active_connections:
            self.active_connections[job_id] = []
        self.active_connections[job_id].append(websocket)

        # Start subscriber task for this job if not already running
        if (
            job_id not in self._subscriber_tasks
            or self._subscriber_tasks[job_id].done()
        ):
            # Create an event to signal when subscription is ready
            self._subscription_ready[job_id] = asyncio.Event()
            task = asyncio.create_task(self._subscribe_to_job(job_id))
            self._subscriber_tasks[job_id] = task

            # Wait for subscription to be ready (with timeout)
            try:
                await asyncio.wait_for(
                    self._subscription_ready[job_id].wait(), timeout=5.0
                )
            except asyncio.TimeoutError:
                logger.warning(f"Subscription ready timeout for job {job_id}")

        return True

    def disconnect(self, job_id: str, websocket: WebSocket) -> None:
        """
        Remove WebSocket connection from job subscription.

        Args:
            job_id: Job identifier
            websocket: WebSocket connection to remove
        """
        if job_id in self.active_connections:
            try:
                self.active_connections[job_id].remove(websocket)
            except ValueError:
                pass

            # Clean up if no more connections for this job
            if not self.active_connections[job_id]:
                del self.active_connections[job_id]

                # Cancel subscriber task
                if job_id in self._subscriber_tasks:
                    self._subscriber_tasks[job_id].cancel()
                    del self._subscriber_tasks[job_id]

                # Clean up subscription ready event
                if job_id in self._subscription_ready:
                    del self._subscription_ready[job_id]

    async def _subscribe_to_job(self, job_id: str) -> None:
        """
        Subscribe to Redis channel for job updates and forward to WebSockets.

        Args:
            job_id: Job identifier to subscribe to
        """
        if not await self._ensure_redis():
            logger.error(f"Cannot subscribe to job {job_id}: Redis not available")
            # Signal that subscription is "ready" (even if failed) to unblock connect
            if job_id in self._subscription_ready:
                self._subscription_ready[job_id].set()
            return

        pubsub = self._redis.pubsub()
        channel = f"batch:progress:{job_id}"

        try:
            await pubsub.subscribe(channel)
            logger.debug(f"Subscribed to channel {channel}")

            # Signal that subscription is ready
            if job_id in self._subscription_ready:
                self._subscription_ready[job_id].set()

            while job_id in self.active_connections:
                try:
                    # Use listen() for proper async iteration instead of polling
                    message = await asyncio.wait_for(
                        pubsub.get_message(ignore_subscribe_messages=True, timeout=1.0),
                        timeout=2.0,
                    )

                    if message and message["type"] == "message":
                        try:
                            data = json.loads(message["data"])
                            await self._broadcast_to_job(job_id, data)

                            # If job is complete/failed/cancelled, stop subscribing
                            status = data.get("status", "")
                            if status in ("complete", "failed", "cancelled"):
                                logger.debug(
                                    f"Job {job_id} reached terminal status: {status}"
                                )
                                break
                        except json.JSONDecodeError as e:
                            logger.error(
                                f"Failed to parse message for job {job_id}: {e}"
                            )

                except asyncio.TimeoutError:
                    # No message received, check if still have connections
                    if job_id not in self.active_connections:
                        break
                except asyncio.CancelledError:
                    logger.debug(f"Subscription task cancelled for job {job_id}")
                    break
                except Exception as e:
                    logger.error(f"Error in subscription loop for job {job_id}: {e}")
                    await asyncio.sleep(0.5)  # Brief pause before retry

        except asyncio.CancelledError:
            logger.debug(f"Subscription cancelled for job {job_id}")
        except Exception as e:
            logger.error(f"Subscription error for job {job_id}: {e}")
        finally:
            try:
                await pubsub.unsubscribe(channel)
                await pubsub.close()
                logger.debug(f"Unsubscribed from channel {channel}")
            except Exception as e:
                logger.warning(f"Error during pubsub cleanup for job {job_id}: {e}")

    async def _broadcast_to_job(self, job_id: str, data: dict) -> None:
        """
        Send message to all WebSocket connections for a job.

        Args:
            job_id: Job identifier
            data: Data to broadcast
        """
        if job_id not in self.active_connections:
            return

        dead_connections = []

        for websocket in self.active_connections[job_id]:
            try:
                await websocket.send_json(data)
            except Exception as e:
                logger.debug(f"Failed to send to WebSocket: {e}")
                dead_connections.append(websocket)

        # Clean up dead connections
        for ws in dead_connections:
            self.disconnect(job_id, ws)

    async def send_initial_status(self, job_id: str, websocket: WebSocket) -> bool:
        """
        Send current job status when client first connects.

        Args:
            job_id: Job identifier
            websocket: WebSocket to send status to

        Returns:
            True if initial status was sent successfully
        """
        if not await self._ensure_redis():
            logger.error(
                f"Cannot get initial status for job {job_id}: Redis not available"
            )
            return False

        try:
            # Get current progress from Redis
            data = await self._redis.get(f"batch:job:{job_id}")
            if data:
                progress = json.loads(data)
                await websocket.send_json(progress)
                logger.debug(
                    f"Sent initial status for job {job_id}: {progress.get('status')}"
                )
                return True
            else:
                logger.debug(f"No initial status found for job {job_id}")
                return False
        except Exception as e:
            logger.error(f"Error sending initial status for job {job_id}: {e}")
            return False


# Singleton instance
manager = ConnectionManager()
