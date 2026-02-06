---
sidebar_position: 3
title: Monitoring
description: Monitor ChemAudit with Prometheus metrics and Grafana dashboards
---

# Monitoring

ChemAudit includes built-in monitoring with Prometheus metrics and optional Grafana dashboards.

## Prometheus Metrics

### Enabling Monitoring

```bash
# Start with monitoring profile
docker-compose --profile monitoring up -d
```

### Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | http://localhost:3001 | admin / (from .env) |
| **Prometheus** | http://localhost:9090 | None |
| **Backend Metrics** | http://localhost:8000/metrics | None |

## Key Metrics

ChemAudit exposes these metrics:

### Request Metrics

- `validation_requests_total`: Total validation requests
- `validation_duration_seconds`: Request duration histogram
- `http_requests_total`: Total HTTP requests by endpoint
- `http_request_duration_seconds`: HTTP request duration

### Batch Processing

- `batch_jobs_active`: Currently processing batch jobs
- `batch_jobs_total`: Total batch jobs by status
- `molecules_processed_total`: Total molecules processed
- `batch_job_duration_seconds`: Batch job processing time

### System Metrics

- `celery_tasks_active`: Active Celery tasks
- `redis_connected_clients`: Redis client count
- `postgres_connections`: Database connections

## Grafana Dashboards

Pre-built dashboards are available after enabling monitoring:

### Application Overview

- Request rate by endpoint
- Average response time
- Error rate
- Active batch jobs

### Batch Processing

- Job queue depth
- Processing rate (molecules/second)
- Job completion time distribution
- Success/failure ratio

### Infrastructure

- Container resource usage
- Database connection pool
- Redis memory usage
- Celery worker status

## Queries

### Prometheus Query Examples

**Request rate:**

```promql
rate(validation_requests_total[5m])
```

**95th percentile latency:**

```promql
histogram_quantile(0.95, rate(validation_duration_seconds_bucket[5m]))
```

**Active batch jobs:**

```promql
batch_jobs_active
```

**Error rate:**

```promql
rate(http_requests_total{status="500"}[5m])
```

## Alerts

Set up alerts in Prometheus for critical conditions:

```yaml
# prometheus/alerts.yml
groups:
  - name: chemaudit
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status="500"}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate detected"

      - alert: BatchQueueBacklog
        expr: batch_jobs_active > 10
        for: 10m
        annotations:
          summary: "Batch queue backlog"
```

## Best Practices

1. **Monitor regularly**: Check dashboards at least daily
2. **Set up alerts**: Configure alerts for critical metrics
3. **Track trends**: Monitor trends over time, not just current values
4. **Capacity planning**: Use metrics to plan scaling
5. **Performance optimization**: Identify slow endpoints for optimization

## Next Steps

- **[Production](/docs/deployment/production)** - Production deployment
- **[Docker](/docs/deployment/docker)** - Docker setup
- **[Troubleshooting](/docs/troubleshooting)** - Debug issues
