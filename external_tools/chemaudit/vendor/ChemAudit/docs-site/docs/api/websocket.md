---
sidebar_position: 4
title: WebSocket
description: Real-time batch processing progress updates via WebSocket
---

# WebSocket API

ChemAudit provides real-time batch processing progress updates via WebSocket, allowing you to monitor job status without polling.

## Connection

### Endpoint

```
ws://localhost:8001/ws/batch/{job_id}
```

Replace `{job_id}` with the job ID returned from POST /batch/upload.

### When to Connect

Connect to the WebSocket immediately after uploading a batch file:

1. POST /batch/upload → receive `job_id`
2. Open WebSocket connection to `ws://.../batch/{job_id}`
3. Listen for progress messages
4. Close connection when status is `complete`, `failed`, or `cancelled`

## Message Format

### Progress Messages

The server sends JSON messages with job status:

```json
{
  "job_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "processing",
  "progress": 45.5,
  "processed": 455,
  "total": 1000,
  "eta_seconds": 68
}
```

| Field | Type | Description |
|-------|------|-------------|
| `job_id` | string | Job identifier |
| `status` | string | Current status |
| `progress` | float | Percentage complete (0-100) |
| `processed` | int | Number of molecules processed |
| `total` | int | Total molecules in job |
| `eta_seconds` | int | Estimated time remaining (seconds) |

### Status Values

| Status | Meaning |
|--------|---------|
| `processing` | Job in progress |
| `complete` | Job finished successfully |
| `failed` | Job encountered fatal error |
| `cancelled` | Job was cancelled |

## Client Examples

### JavaScript

```javascript
const jobId = 'your-job-id';
const ws = new WebSocket(`ws://localhost:8001/ws/batch/${jobId}`);

ws.onopen = () => {
  console.log('WebSocket connected');
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log(`Progress: ${data.progress}% (${data.processed}/${data.total})`);
  console.log(`ETA: ${data.eta_seconds} seconds`);

  if (data.status === 'complete') {
    console.log('Job complete!');
    ws.close();
  } else if (data.status === 'failed') {
    console.error('Job failed');
    ws.close();
  }
};

ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};

ws.onclose = () => {
  console.log('WebSocket closed');
};

// Send keep-alive pings every 30 seconds
const keepAlive = setInterval(() => {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send('ping');
  }
}, 30000);

// Clean up on close
ws.addEventListener('close', () => {
  clearInterval(keepAlive);
});
```

### Python

```python
import asyncio
import websockets
import json

async def monitor_batch(job_id):
    uri = f"ws://localhost:8001/ws/batch/`{job_id}`"

    async with websockets.connect(uri) as websocket:
        print("Connected to WebSocket")

        # Send keep-alive pings
        async def keep_alive():
            while True:
                await asyncio.sleep(30)
                await websocket.send("ping")

        # Start keep-alive task
        ping_task = asyncio.create_task(keep_alive())

        try:
            async for message in websocket:
                data = json.loads(message)

                print(f"Progress: {data['progress']}% ({data['processed']}/{data['total']})")
                print(f"ETA: {data['eta_seconds']} seconds")

                if data['status'] in ['complete', 'failed', 'cancelled']:
                    print(f"Job {data['status']}")
                    break

        finally:
            ping_task.cancel()

# Run
asyncio.run(monitor_batch('your-job-id'))
```

### React Hook

```typescript
import { useEffect, useState } from 'react';

interface BatchProgress {
  job_id: string;
  status: string;
  progress: number;
  processed: number;
  total: number;
  eta_seconds: number;
}

export function useBatchProgress(jobId: string) {
  const [progress, setProgress] = useState<BatchProgress | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const ws = new WebSocket(`ws://localhost:8001/ws/batch/${jobId}`);

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      setProgress(data);

      if (data.status !== 'processing') {
        ws.close();
      }
    };

    ws.onerror = () => {
      setError('WebSocket connection error');
    };

    // Keep-alive
    const interval = setInterval(() => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send('ping');
      }
    }, 30000);

    return () => {
      clearInterval(interval);
      ws.close();
    };
  }, [jobId]);

  return { progress, error };
}
```

## Keep-Alive

WebSocket connections may timeout if idle. Send periodic ping messages to keep the connection alive:

```javascript
// Client-side ping every 30 seconds
setInterval(() => {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send('ping');
  }
}, 30000);
```

The server will ignore ping messages and continue sending progress updates.

## Error Handling

### Connection Errors

If the WebSocket connection fails:

- Check that the job ID is valid
- Verify the backend is running
- Check for firewall/proxy issues
- Fall back to polling GET /batch/\{job_id\}/status

### Reconnection Strategy

If the connection drops unexpectedly:

1. Try to reconnect immediately (up to 3 times)
2. If reconnection fails, fall back to polling
3. Check job status via GET /batch/\{job_id\}/status

```javascript
let reconnectAttempts = 0;
const maxReconnectAttempts = 3;

function connectWebSocket(jobId) {
  const ws = new WebSocket(`ws://localhost:8001/ws/batch/${jobId}`);

  ws.onclose = () => {
    if (reconnectAttempts < maxReconnectAttempts) {
      reconnectAttempts++;
      console.log(`Reconnecting (attempt ${reconnectAttempts})...`);
      setTimeout(() => connectWebSocket(jobId), 1000);
    } else {
      console.log('Max reconnect attempts reached, falling back to polling');
      startPolling(jobId);
    }
  };

  ws.onopen = () => {
    reconnectAttempts = 0; // Reset on successful connection
  };

  return ws;
}
```

## Best Practices

1. **Connect immediately after upload**: Don't delay WebSocket connection
2. **Send keep-alive pings**: Prevent connection timeouts
3. **Handle disconnections gracefully**: Implement reconnection or fall back to polling
4. **Close on completion**: Close the WebSocket when job finishes
5. **Validate messages**: Always parse and validate JSON messages
6. **Show user feedback**: Display progress to users for long-running jobs

## Comparison with Polling

| Approach | Pros | Cons |
|----------|------|------|
| **WebSocket** | Real-time updates, efficient, low latency | Requires WebSocket support, more complex |
| **Polling** | Simple, works everywhere | Higher latency, more server load |

Use WebSocket for the best user experience, with polling as a fallback.

## Next Steps

- **[Batch Processing](/docs/user-guide/batch-processing)** - Using batch features
- **[Endpoints](/docs/api/endpoints)** - Full API reference
- **[Error Handling](/docs/api/error-handling)** - Handling errors
