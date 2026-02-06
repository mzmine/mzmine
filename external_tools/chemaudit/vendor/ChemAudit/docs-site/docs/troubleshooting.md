---
sidebar_position: 100
title: Troubleshooting
description: Common issues and solutions for ChemAudit
---

# Troubleshooting

Common issues and solutions for ChemAudit installation, deployment, and operation.

## Installation Issues

### Docker containers won't start

**Port already in use:**

```bash
# Find what's using the port
lsof -i :8001

# Change port in docker-compose.yml or kill the process
```

**Cannot connect to Docker daemon:**

```bash
# Start Docker service
sudo systemctl start docker

# Or on macOS, launch Docker Desktop
```

**Out of memory:**

Increase Docker memory allocation:
- **Docker Desktop**: Settings → Resources → Memory → 4GB+
- **Linux**: Check system memory with `free -h`

### Frontend won't load

**Blank page or React errors:**

```bash
# Clear and rebuild
cd frontend
rm -rf node_modules dist
npm install
npm run dev
```

**Cannot connect to API:**

Verify backend is running:

```bash
curl http://localhost:8001/api/v1/health
```

Check CORS settings in `.env`:

```env
CORS_ORIGINS_STR=http://localhost:3002,http://127.0.0.1:3002
```

## Validation Errors

### Invalid SMILES

**Unclosed rings:**

| Invalid | Valid | Fix |
|---------|-------|-----|
| `C1CCC1` | `C1CCCC1` | Close all rings |

**Too many bonds:**

| Invalid | Valid | Fix |
|---------|-------|-----|
| `CC(C)(C)(C)C` | `CC(C)(C)C` | Check valence |

**Invalid aromatic:**

| Invalid | Valid | Fix |
|---------|-------|-----|
| `c1cccc1` | `c1ccccc1` | Benzene needs 6 carbons |

### Kekulization failed

Check aromatic ring follows Hückel's rule (4n+2 electrons).

## Batch Processing Issues

### File too large

Check deployment limits:

```bash
curl http://localhost:8001/api/v1/config
```

Use larger profile or split file:

```bash
# Split SDF
split -l 10000 large.sdf chunk_

# Deploy larger profile
./deploy.sh large
```

### Progress stuck at 0%

```bash
# Check Celery worker
docker-compose logs celery-worker

# Check Redis
docker-compose exec redis redis-cli ping

# Restart workers
docker-compose restart celery-worker
```

### CSV column not detected

Ensure CSV has headers and use supported column names:
- `SMILES`, `smiles`, `Smiles`
- `CANONICAL_SMILES`, `canonical_smiles`

## API Errors

### HTTP 429 - Rate Limited

You exceeded the rate limit. Solutions:
- Wait and retry with exponential backoff
- Use API key for higher limits (300/min)
- Use batch processing for many molecules

### HTTP 500 - Internal Server Error

```bash
# Check backend logs
docker-compose logs backend

# Restart backend
docker-compose restart backend
```

### HTTP 504 - Gateway Timeout

Request took too long. For complex molecules, this is normal.

## Performance Issues

### Slow validation

**First request cold start:** Wait ~10s after startup for RDKit initialization.

**Complex molecules:** Expected for molecules with many rings and stereocenters.

**Resource constraints:**

```bash
# Check container resources
docker stats
```

### Batch processing slow

**Scale workers:**

```bash
# Use larger profile
./deploy.sh large  # 8 workers

# Or scale manually
docker-compose -f docker-compose.prod.yml up -d --scale celery-worker=8
```

**Check worker utilization:**

```bash
docker-compose exec celery-worker celery -A app.celery_app inspect active
```

## Docker Issues

### Images won't build

```bash
# Clear Docker cache
docker system prune -a
docker-compose build --no-cache
```

### Container keeps restarting

```bash
# Check logs
docker-compose logs --tail=50 backend

# Recreate containers
docker-compose down
docker-compose up -d
```

### Database connection refused

```bash
# Check PostgreSQL health
docker-compose exec postgres pg_isready -U chemaudit

# Verify connection string in .env
DATABASE_URL=postgresql+asyncpg://chemaudit:password@postgres:5432/chemaudit
```

Use `postgres` (service name) not `localhost` inside Docker.

## Getting Help

### Collect Debug Information

```bash
# System info
uname -a
docker --version
docker-compose --version

# Container status
docker-compose ps

# Recent logs
docker-compose logs --tail=100 > debug_logs.txt

# Resource usage
docker stats --no-stream
```

### Report Issues

[Open a GitHub Issue](https://github.com/Kohulan/ChemAudit/issues) with:
- Description of the problem
- Steps to reproduce
- Debug information from above
- Screenshots (if applicable)

## Next Steps

- **[Getting Started](/docs/getting-started/installation)** - Installation guide
- **[Deployment](/docs/deployment/docker)** - Docker deployment
- **[API Reference](/docs/api/overview)** - API documentation
