<div align="center">

<img src="assets/logo.png" alt="ChemAudit" width="80" />

# Deployment Guide

### Production Deployment for ChemAudit

[![Docker](https://img.shields.io/badge/Docker-24.0+-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Nginx](https://img.shields.io/badge/Nginx-1.25+-009639?logo=nginx&logoColor=white)](https://nginx.org/)
[![Let's Encrypt](https://img.shields.io/badge/Let's_Encrypt-SSL-003A70?logo=letsencrypt&logoColor=white)](https://letsencrypt.org/)

</div>

---

## 📋 Table of Contents

- [Architecture Overview](#-architecture-overview)
- [Prerequisites](#-prerequisites)
- [Deployment Profiles](#-deployment-profiles)
- [Quick Start](#-quick-start)
- [Production Deployment](#-production-deployment)
- [SSL Configuration](#-ssl-configuration)
- [Monitoring](#-monitoring)
- [Maintenance](#-maintenance)
- [Scaling](#-scaling)
- [Troubleshooting](#-troubleshooting)
- [Security Checklist](#-security-checklist)

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         INTERNET                                │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    NGINX (Port 80/443)                          │
│              SSL Termination • Load Balancing                   │
└──────┬──────────────────┬──────────────────┬────────────────────┘
       │                  │                  │
       ▼                  ▼                  ▼
┌─────────────┐   ┌─────────────┐   ┌─────────────────────┐
│   Frontend  │   │   Backend   │   │   Celery Workers    │
│  React SPA  │   │   FastAPI   │   │  Batch Processing   │
└─────────────┘   └──────┬──────┘   └──────────┬──────────┘
                         │                     │
              ┌──────────┴──────────┐          │
              ▼                     ▼          ▼
       ┌─────────────┐       ┌─────────────────────┐
       │ PostgreSQL  │       │       Redis         │
       │  Database   │       │  Cache + Broker     │
       └─────────────┘       └─────────────────────┘
```

---

## 📦 Prerequisites

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **Docker Engine** | 24.0+ | Latest |
| **Docker Compose** | 2.20+ | Latest |
| **RAM** | 4 GB | 8 GB+ |
| **Disk Space** | 20 GB | 50 GB+ |
| **CPU Cores** | 2 | 4+ |

**Additional Requirements:**
- Domain name (for production SSL)
- Ports 80 and 443 available
- Linux server (Ubuntu 22.04 LTS recommended)

---

## 🎛️ Deployment Profiles

ChemAudit includes pre-configured deployment profiles to match your workload requirements. Each profile configures batch limits, worker counts, and memory allocation.

### Available Profiles

| Profile | Max Batch Size | Max File Size | Celery Workers | Gunicorn Workers | Redis Memory | Use Case |
|---------|----------------|---------------|----------------|------------------|--------------|----------|
| **small** | 1,000 | 100 MB | 2 | 2 | 256 MB | Development, testing |
| **medium** | 10,000 | 500 MB | 4 | 4 | 512 MB | Standard production |
| **large** | 50,000 | 500 MB | 8 | 8 | 1 GB | High-throughput labs |
| **xl** | 100,000 | 1 GB | 12 | 8 | 2 GB | Enterprise scale |
| **coconut** | 1,000,000 | 1 GB | 16 | 8 | 4 GB | Full COCONUT database |

### Using the Deploy Script

The interactive deploy script (`deploy.sh`) simplifies profile selection:

```bash
# Interactive mode - shows menu
./deploy.sh

# Direct profile selection
./deploy.sh large
```

**What the script does:**
1. Parses the selected profile from `config/{profile}.yml`
2. Exports environment variables for Docker Compose
3. Updates `.env` with profile settings
4. Launches `docker-compose.prod.yml`

### Profile Configuration Files

Profiles are defined in `config/` as YAML files:

```yaml
# config/large.yml
MAX_BATCH_SIZE: 50000
MAX_FILE_SIZE_MB: 500
CELERY_WORKERS: 8
GUNICORN_WORKERS: 8
REDIS_MAXMEMORY: 1gb
BACKEND_MEMORY_LIMIT: 4g
CELERY_MEMORY_LIMIT: 4g
```

### Dynamic Limits API

The frontend fetches deployment limits from the `/api/v1/config` endpoint:

```bash
curl http://localhost:8000/api/v1/config
```

```json
{
  "app_name": "ChemAudit",
  "app_version": "1.0.0",
  "deployment_profile": "large",
  "limits": {
    "max_batch_size": 50000,
    "max_file_size_mb": 500,
    "max_file_size_bytes": 524288000
  }
}
```

This ensures the UI always displays accurate limits for your deployment.

### Custom Profiles

Create a custom profile by copying an existing one:

```bash
cp config/medium.yml config/custom.yml
# Edit config/custom.yml with your settings
./deploy.sh custom
```

---

## ⚡ Quick Start

For local testing of production build:

```bash
# 1️⃣ Clone and enter directory
git clone https://github.com/Kohulan/ChemAudit.git
cd chemaudit

# 2️⃣ Configure environment
cp .env.prod.example .env
# Edit .env with your database password

# 3️⃣ Deploy with profile selection
./deploy.sh medium
```

The deploy script handles:
- Frontend building
- Environment configuration
- Profile-based resource allocation
- Docker Compose orchestration

**Manual deployment** (without deploy script):

```bash
# Build frontend
cd frontend && npm install && npm run build && cd ..
cp -r frontend/dist frontend-dist

# Launch with default settings
docker-compose -f docker-compose.prod.yml up -d

# Verify deployment
docker-compose -f docker-compose.prod.yml ps
```

🌐 **Access:** http://localhost

---

## 🌍 Production Deployment

### Step 1: Server Setup

```bash
# Update system
sudo apt-get update && sudo apt-get upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Configure firewall
sudo ufw allow 22/tcp   # SSH
sudo ufw allow 80/tcp   # HTTP
sudo ufw allow 443/tcp  # HTTPS
sudo ufw enable
```

### Step 2: Environment Configuration

```bash
# Copy template
cp .env.prod.example .env

# Generate secure passwords
openssl rand -base64 32  # Use for POSTGRES_PASSWORD
openssl rand -base64 32  # Use for GRAFANA_PASSWORD
```

<details>
<summary><b>📄 Example .env Configuration</b></summary>

```env
# Database
POSTGRES_USER=chemaudit
POSTGRES_PASSWORD=your_secure_password_here
POSTGRES_DB=chemaudit
DATABASE_URL=postgresql+asyncpg://chemaudit:your_secure_password_here@postgres:5432/chemaudit

# Redis
REDIS_URL=redis://redis:6379

# Application
DEBUG=false
CORS_ORIGINS=["https://your-domain.com"]

# Monitoring
GRAFANA_PASSWORD=your_grafana_password_here
```

</details>

### Step 3: Build and Deploy

```bash
# Build frontend for production
cd frontend
npm ci --production
npm run build
cd ..
cp -r frontend/dist frontend-dist

# Start all services
docker-compose -f docker-compose.prod.yml up -d

# Start with monitoring (optional)
docker-compose -f docker-compose.prod.yml --profile monitoring up -d

# Run database migrations
docker-compose -f docker-compose.prod.yml exec backend alembic upgrade head
```

### Step 4: Verify Deployment

```bash
# Check all services are running
docker-compose -f docker-compose.prod.yml ps

# Test health endpoint
curl http://localhost/api/v1/health

# Test validation API
curl -X POST http://localhost/api/v1/validate \
  -H "Content-Type: application/json" \
  -d '{"molecule": "CCO", "format": "smiles"}'
```

---

## 🔒 SSL Configuration

### Using Let's Encrypt (Recommended)

```bash
# Install Certbot
sudo apt-get install certbot -y

# Stop nginx temporarily
docker-compose -f docker-compose.prod.yml stop nginx

# Obtain certificate
sudo certbot certonly --standalone \
  -d your-domain.com \
  -d www.your-domain.com

# Create SSL directory
mkdir -p nginx/ssl

# Copy certificates
sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem nginx/ssl/cert.pem
sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem nginx/ssl/key.pem
sudo chown $USER:$USER nginx/ssl/*.pem

# Generate DH parameters (takes several minutes)
openssl dhparam -out nginx/ssl/dhparam.pem 4096
```

### Configure Nginx for HTTPS

Edit `nginx/nginx.conf`:

```nginx
# HTTPS Server
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    include /etc/nginx/conf.d/ssl-params.conf;
    include /etc/nginx/conf.d/locations.conf;
}

# HTTP to HTTPS Redirect
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

### Auto-Renewal Setup

```bash
# Add to crontab
sudo crontab -e

# Add this line (runs daily at 3 AM)
0 3 * * * certbot renew --quiet --post-hook "docker-compose -f /path/to/docker-compose.prod.yml restart nginx"
```

---

## 📊 Monitoring

### Access Points

| Service | URL | Default Credentials |
|---------|-----|---------------------|
| **Grafana** | http://localhost:3001 | admin / (from .env) |
| **Prometheus** | http://localhost:9090 | - |
| **Backend Metrics** | http://localhost:8000/metrics | - |

### Key Metrics

| Metric | Description |
|--------|-------------|
| `validation_requests_total` | Total validation requests |
| `validation_duration_seconds` | Request duration histogram |
| `batch_jobs_active` | Currently processing batch jobs |
| `molecules_processed_total` | Total molecules processed |

### Pre-built Dashboards

Grafana comes with pre-configured dashboards:
- **Application Overview** - Request rates, latencies, errors
- **Batch Processing** - Job queue depth, processing rates
- **Infrastructure** - Container resources, database connections

---

## 🔧 Maintenance

### Viewing Logs

```bash
# All services
docker-compose -f docker-compose.prod.yml logs -f

# Specific service
docker-compose -f docker-compose.prod.yml logs -f backend

# Last 100 lines
docker-compose -f docker-compose.prod.yml logs --tail=100 backend
```

### Database Backup

```bash
# Create backup
docker-compose -f docker-compose.prod.yml exec postgres \
  pg_dump -U chemaudit chemaudit > backup_$(date +%Y%m%d).sql

# Restore backup
cat backup_20260129.sql | docker-compose -f docker-compose.prod.yml exec -T postgres \
  psql -U chemaudit chemaudit
```

### Updating Application

```bash
# Pull latest changes
git pull origin main

# Rebuild frontend
cd frontend && npm ci && npm run build && cd ..
cp -r frontend/dist frontend-dist

# Rebuild and restart
docker-compose -f docker-compose.prod.yml build
docker-compose -f docker-compose.prod.yml up -d

# Run new migrations
docker-compose -f docker-compose.prod.yml exec backend alembic upgrade head
```

---

## 📈 Scaling

### Using Deployment Profiles (Recommended)

The easiest way to scale is to use a larger deployment profile:

```bash
# Switch to larger profile
./deploy.sh large   # 8 Celery workers, 8 Gunicorn workers
./deploy.sh xl      # 12 Celery workers, 8 Gunicorn workers
./deploy.sh coconut # 16 Celery workers, 8 Gunicorn workers
```

### Horizontal Scaling - Celery Workers

```bash
# Scale to 8 workers (overrides profile setting)
docker-compose -f docker-compose.prod.yml up -d --scale celery-worker=8

# Verify workers
docker-compose -f docker-compose.prod.yml exec backend \
  celery -A app.celery_app inspect active
```

### Vertical Scaling - Gunicorn Workers

Worker count is now configurable via environment variable:

```bash
# Set in .env or export
export GUNICORN_WORKERS=8

# Or use a profile with more workers
./deploy.sh xl
```

The `GUNICORN_WORKERS` environment variable is passed to the container at runtime.

### Performance Tuning

<details>
<summary><b>PostgreSQL Configuration</b></summary>

```yaml
# docker-compose.prod.yml
postgres:
  command:
    - postgres
    - -c
    - max_connections=200
    - -c
    - shared_buffers=256MB
    - -c
    - effective_cache_size=1GB
    - -c
    - work_mem=16MB
```

</details>

<details>
<summary><b>Redis Configuration</b></summary>

```yaml
# docker-compose.prod.yml
redis:
  command: redis-server --maxmemory 1gb --maxmemory-policy allkeys-lru
```

</details>

---

## 🔍 Troubleshooting

### Common Issues

<details>
<summary><b>🔴 Service Won't Start</b></summary>

```bash
# Check service status
docker-compose -f docker-compose.prod.yml ps

# View detailed logs
docker-compose -f docker-compose.prod.yml logs backend

# Check health status
docker inspect chemaudit-backend-prod | grep -A 10 Health
```

</details>

<details>
<summary><b>🔴 Database Connection Issues</b></summary>

```bash
# Verify PostgreSQL is running
docker-compose -f docker-compose.prod.yml exec postgres pg_isready

# Test connection from backend
docker-compose -f docker-compose.prod.yml exec backend \
  python -c "from app.database import engine; print('Connected!')"

# View PostgreSQL logs
docker-compose -f docker-compose.prod.yml logs postgres
```

</details>

<details>
<summary><b>🔴 SSL Certificate Issues</b></summary>

```bash
# Verify certificate files
ls -la nginx/ssl/

# Check certificate expiration
openssl x509 -in nginx/ssl/cert.pem -noout -dates

# Test SSL configuration
openssl s_client -connect your-domain.com:443 -servername your-domain.com
```

</details>

<details>
<summary><b>🔴 Nginx Configuration Errors</b></summary>

```bash
# Test nginx configuration
docker-compose -f docker-compose.prod.yml exec nginx nginx -t

# Reload nginx
docker-compose -f docker-compose.prod.yml exec nginx nginx -s reload
```

</details>

---

## ✅ Security Checklist

Before going to production, ensure:

| Category | Item | Status |
|----------|------|--------|
| **Authentication** | Changed all default passwords | ⬜ |
| **SSL/TLS** | Valid SSL certificate configured | ⬜ |
| **SSL/TLS** | HSTS enabled in nginx | ⬜ |
| **Network** | Firewall configured (80, 443 only) | ⬜ |
| **Network** | Database not publicly accessible | ⬜ |
| **Network** | Redis not publicly accessible | ⬜ |
| **Monitoring** | Prometheus metrics access restricted | ⬜ |
| **Monitoring** | Grafana access secured | ⬜ |
| **Backup** | Regular database backups configured | ⬜ |
| **Backup** | Backup restoration tested | ⬜ |
| **Headers** | Security headers configured in nginx | ⬜ |
| **Rate Limiting** | API rate limiting enabled | ⬜ |
| **Containers** | Non-root users in Docker containers | ⬜ |

---

## 🏢 High Availability

For production at scale, consider:

| Component | Recommendation |
|-----------|----------------|
| **Load Balancer** | AWS ALB, Google Cloud LB, HAProxy |
| **Database** | Managed PostgreSQL (RDS, Cloud SQL) |
| **Cache** | Managed Redis (ElastiCache, Redis Cloud) |
| **Orchestration** | Kubernetes for multi-node |
| **CDN** | CloudFlare, AWS CloudFront |
| **Monitoring** | DataDog, New Relic, Grafana Cloud |
| **Logging** | ELK Stack, Grafana Loki |

---

<div align="center">

**Need help?** [Open an Issue](https://github.com/Kohulan/ChemAudit/issues)

</div>
