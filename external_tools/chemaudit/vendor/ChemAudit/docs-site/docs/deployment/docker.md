---
sidebar_position: 1
title: Docker
description: Deploy ChemAudit using Docker Compose for development and testing
---

# Docker Deployment

Deploy ChemAudit using Docker Compose for a complete development environment with all services configured and ready to use.

## Architecture

ChemAudit's Docker deployment includes:

- **Frontend**: React SPA (port 3002 in development)
- **Backend**: FastAPI application (port 8001 in development)
- **PostgreSQL**: Database (port 5432, internal only)
- **Redis**: Cache and message broker (port 6379, internal only)
- **Celery Workers**: Background job processing
- **Nginx**: Reverse proxy (port 80 in production)
- **Prometheus** (optional): Metrics collection
- **Grafana** (optional): Monitoring dashboards

## Development Setup

### Prerequisites

- Docker Engine 24.0+
- Docker Compose 2.20+
- 4 GB RAM minimum (8 GB recommended)
- 20 GB disk space

### Quick Start

```bash
# Clone repository
git clone https://github.com/Kohulan/ChemAudit.git
cd chemaudit

# Create environment file
cp .env.example .env
# Edit .env to set required secrets

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f
```

**Access points:**

- Web UI: http://localhost:3002
- API: http://localhost:8001/api/v1/docs
- Backend health: http://localhost:8001/api/v1/health

### Environment Configuration

Edit `.env` to configure required secrets:

```env
# Database
POSTGRES_PASSWORD=your_secure_password

# Application
SECRET_KEY=your_secret_key
API_KEY_ADMIN_SECRET=your_admin_secret
CSRF_SECRET_KEY=your_csrf_secret

# Monitoring (optional)
GRAFANA_PASSWORD=your_grafana_password
```

Generate secure secrets:

```bash
openssl rand -base64 32
```

## Service Overview

| Service | Description | Ports |
|---------|-------------|-------|
| **frontend** | React development server | 3002 |
| **backend** | FastAPI application | 8001 |
| **postgres** | PostgreSQL database | 5432 (internal) |
| **redis** | Cache and broker | 6379 (internal) |
| **celery-worker** | Background processing | None |

## Common Commands

### Start Services

```bash
# Start all services
docker-compose up -d

# Start with monitoring
docker-compose --profile monitoring up -d

# Start specific service
docker-compose up -d backend
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend

# Last 100 lines
docker-compose logs --tail=100 backend
```

### Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (deletes data)
docker-compose down -v
```

### Restart Services

```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart backend
```

### Execute Commands

```bash
# Access backend shell
docker-compose exec backend bash

# Run database migrations
docker-compose exec backend alembic upgrade head

# Access PostgreSQL
docker-compose exec postgres psql -U chemaudit
```

## Troubleshooting

**Port already in use:**

Change ports in `docker-compose.yml`:

```yaml
ports:
  - "3003:3002"  # Map to different host port
```

**Services won't start:**

```bash
# Check service status
docker-compose ps

# View detailed logs
docker-compose logs backend

# Rebuild images
docker-compose build --no-cache
docker-compose up -d
```

**Database connection errors:**

```bash
# Verify PostgreSQL is running
docker-compose exec postgres pg_isready

# Check connection from backend
docker-compose exec backend python -c "from app.database import engine; print('Connected!')"
```

## Next Steps

- **[Production Deployment](/docs/deployment/production)** - Production setup with Nginx and SSL
- **[Monitoring](/docs/deployment/monitoring)** - Prometheus and Grafana setup
- **[Getting Started](/docs/getting-started/installation)** - Complete installation guide
