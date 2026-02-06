import { useState, useEffect, useCallback, useRef } from 'react';
import type { BatchProgress } from '../types/batch';

/**
 * WebSocket hook for real-time batch progress updates.
 *
 * Connects to the batch progress WebSocket endpoint and provides
 * reactive progress state updates.
 *
 * @param jobId - Job ID to subscribe to (null to disconnect)
 * @returns Progress state, connection status, and manual close function
 */
export function useBatchProgress(jobId: string | null) {
  const [progress, setProgress] = useState<BatchProgress | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectAttempts = useRef(0);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(
    null
  );
  const jobCompletedRef = useRef(false);
  const maxReconnectAttempts = 3;

  // Connect/disconnect when jobId changes
  useEffect(() => {
    if (!jobId) {
      // Clear state when jobId is null
      setProgress(null);
      setIsConnected(false);
      setError(null);
      jobCompletedRef.current = false;
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }
      return;
    }

    // Reset completion flag for new job
    jobCompletedRef.current = false;
    reconnectAttempts.current = 0;

    const connect = () => {
      // Close existing connection
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }

      // Determine WebSocket URL based on current location
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.hostname;
      // Use VITE_BACKEND_PORT for development, or same port for production
      const port = import.meta.env.VITE_BACKEND_PORT || window.location.port;
      const wsUrl = `${protocol}//${host}:${port}/ws/batch/${jobId}`;

      try {
        const ws = new WebSocket(wsUrl);
        wsRef.current = ws;

        ws.onopen = () => {
          setIsConnected(true);
          setError(null);
          reconnectAttempts.current = 0;
        };

        ws.onclose = (event) => {
          setIsConnected(false);

          // If job is complete/failed/cancelled, don't reconnect
          if (jobCompletedRef.current) {
            return;
          }

          // Attempt reconnection for unexpected closes
          if (
            !event.wasClean &&
            reconnectAttempts.current < maxReconnectAttempts
          ) {
            reconnectAttempts.current += 1;
            reconnectTimeoutRef.current = setTimeout(
              connect,
              1000 * reconnectAttempts.current
            );
          }
        };

        ws.onerror = () => {
          setError('WebSocket connection error');
        };

        ws.onmessage = (event) => {
          try {
            const data: BatchProgress = JSON.parse(event.data);
            setProgress(data);

            // If job completed, mark it and close the connection
            if (
              data.status === 'complete' ||
              data.status === 'failed' ||
              data.status === 'cancelled'
            ) {
              jobCompletedRef.current = true;
              // Keep connection open briefly to ensure we got the final message
              setTimeout(() => {
                if (wsRef.current) {
                  wsRef.current.close();
                  wsRef.current = null;
                }
              }, 500);
            }
          } catch (e) {
            console.error('Failed to parse WebSocket message:', e);
          }
        };
      } catch (e) {
        setError('Failed to create WebSocket connection');
      }
    };

    connect();

    // Cleanup on unmount or jobId change
    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [jobId]);

  // Manual close function
  const close = useCallback(() => {
    jobCompletedRef.current = true; // Prevent reconnection
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    setIsConnected(false);
  }, []);

  return {
    progress,
    isConnected,
    error,
    close,
  };
}
