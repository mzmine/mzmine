---
sidebar_position: 2
title: Configuration
description: Configure ChemAudit with environment variables and deployment profiles
---

# Configuration

ChemAudit is configured through environment variables and deployment profiles that control batch limits, worker counts, and resource allocation.

## Environment Variables

Configuration is managed through a `.env` file in the project root. Copy `.env.example` to get started:

```bash
cp .env.example .env
```

### Required Variables

These variables must be set before starting ChemAudit:

| Variable | Description | Example |
|----------|-------------|---------|
| `POSTGRES_PASSWORD` | Database password | Generated with `openssl rand -base64 32` |
| `SECRET_KEY` | Application secret key | Generated with `openssl rand -base64 32` |
| `API_KEY_ADMIN_SECRET` | Admin secret for API key management | Generated with `openssl rand -base64 32` |
| `CSRF_SECRET_KEY` | CSRF protection secret | Generated with `openssl rand -base64 32` |

:::danger Never Commit Secrets
Never commit your `.env` file to version control. Keep secrets secure and rotate them periodically.
:::

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_USER` | Database username | `chemaudit` |
| `POSTGRES_DB` | Database name | `chemaudit` |
| `DATABASE_URL` | Full database connection string | Auto-generated from above |
| `REDIS_URL` | Redis connection string | `redis://redis:6379` |
| `DEBUG` | Enable debug mode | `false` |
| `CORS_ORIGINS_STR` | Allowed CORS origins (comma-separated) | `http://localhost:3002` |
| `GRAFANA_PASSWORD` | Grafana admin password | `admin` |

### Development vs Production

:::info Development Mode
In development (using `docker-compose.yml`), services are exposed on individual ports. The frontend runs on 3002, backend on 8001.
:::

:::info Production Mode
In production (using `docker-compose.prod.yml`), all services run behind Nginx on port 80/443.
:::

## Deployment Profiles

ChemAudit includes pre-configured deployment profiles that set batch limits, worker counts, and memory allocation based on your workload.

### Available Profiles

| Profile | Max Batch | Max File Size | Celery Workers | Use Case |
|---------|-----------|---------------|----------------|----------|
| **small** | 1,000 | 100 MB | 2 | Development, testing |
| **medium** | 10,000 | 500 MB | 4 | Standard production |
| **large** | 50,000 | 500 MB | 8 | High-throughput labs |
| **xl** | 100,000 | 1 GB | 12 | Enterprise scale |
| **coconut** | 1,000,000 | 1 GB | 16 | Full COCONUT database |

### Profile Configuration Files

Profiles are defined in YAML files under `config/`:

```yaml
# config/medium.yml
MAX_BATCH_SIZE: 10000
MAX_FILE_SIZE_MB: 500
CELERY_WORKERS: 4
GUNICORN_WORKERS: 4
REDIS_MAXMEMORY: 512mb
BACKEND_MEMORY_LIMIT: 2g
CELERY_MEMORY_LIMIT: 2g
```

### Using Deployment Profiles

The `deploy.sh` script makes it easy to deploy with a specific profile:

```bash
# Interactive mode - shows menu of profiles
./deploy.sh

# Direct profile selection
./deploy.sh large
```

The script automatically:

1. Parses the selected profile from `config/{profile}.yml`
2. Exports environment variables for Docker Compose
3. Updates `.env` with profile settings
4. Launches `docker-compose.prod.yml`

### Custom Profiles

Create a custom profile by copying an existing one:

```bash
# Copy and edit
cp config/medium.yml config/custom.yml

# Edit config/custom.yml with your settings
# Then deploy
./deploy.sh custom
```

## Dynamic Limit Discovery

The frontend automatically discovers deployment limits from the backend API:

```bash
curl http://localhost:8001/api/v1/config
```

Response:

```json
{
  "app_name": "ChemAudit",
  "app_version": "1.0.0",
  "deployment_profile": "medium",
  "limits": {
    "max_batch_size": 10000,
    "max_file_size_mb": 500,
    "max_file_size_bytes": 524288000
  }
}
```

This ensures the UI always displays accurate limits for your deployment.

## CORS Configuration

For development, CORS origins are configured in `.env`:

```env
# Comma-separated string (not JSON array)
CORS_ORIGINS_STR=http://localhost:3002,http://127.0.0.1:3002
```

For production behind Nginx, CORS is handled by the proxy configuration.

:::warning CORS Security
Only add trusted origins to CORS_ORIGINS_STR. In production, use your actual domain names.
:::

## Next Steps

With configuration complete:

1. **[Run your first validation](/docs/getting-started/first-validation)** - Test your setup
2. **[Explore deployment options](/docs/deployment/docker)** - Production deployment guide
3. **[Monitor your deployment](/docs/deployment/monitoring)** - Set up Prometheus and Grafana
