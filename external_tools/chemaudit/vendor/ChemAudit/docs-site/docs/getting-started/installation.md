---
sidebar_position: 1
title: Installation
description: Install ChemAudit using Docker or manual setup for development and testing
---

# Installation

Get ChemAudit running on your system using Docker (recommended) or manual installation for development.

## Prerequisites

Before installing ChemAudit, ensure you have the following requirements:

| Requirement | Minimum Version | Recommended |
|-------------|----------------|-------------|
| **Docker Engine** | 24.0+ | Latest |
| **Docker Compose** | 2.20+ | Latest |
| **RAM** | 4 GB | 8 GB+ |
| **Disk Space** | 20 GB | 50 GB+ |
| **CPU Cores** | 2 | 4+ |

For manual installation (development only):

| Requirement | Version |
|-------------|---------|
| **Python** | 3.11+ |
| **Node.js** | 18+ |
| **PostgreSQL** | 16+ |
| **Redis** | 7+ |

## Docker Installation (Recommended)

The fastest way to get started is with Docker Compose:

```bash
# Clone the repository
git clone https://github.com/Kohulan/ChemAudit.git
cd chemaudit

# Create environment file with required secrets
cp .env.example .env
# Edit .env to set POSTGRES_PASSWORD, SECRET_KEY,
# API_KEY_ADMIN_SECRET, CSRF_SECRET_KEY, GRAFANA_PASSWORD

# Start all services in development mode
docker-compose up -d

# Wait for services to be ready (watch logs)
docker-compose logs -f
```

:::tip Success
When you see "Application startup complete" in the logs, ChemAudit is ready to use.
:::

### Access Points

After successful startup, ChemAudit will be available at:

| Service | URL |
|---------|-----|
| **Web Interface** | http://localhost:3002 |
| **API Documentation** | http://localhost:8001/api/v1/docs |
| **API ReDoc** | http://localhost:8001/api/v1/redoc |

### Production Deployment

For production use with configurable batch limits:

```bash
# Interactive deployment - select a profile
./deploy.sh

# Or specify profile directly
./deploy.sh medium   # 10K molecules, 4 workers
./deploy.sh large    # 50K molecules, 8 workers
./deploy.sh coconut  # 1M molecules, 16 workers
```

Access the production deployment at http://localhost (all services behind Nginx).

:::info
See the [Deployment Guide](/docs/deployment/docker) for full production setup including SSL, monitoring, and scaling.
:::

## Manual Installation (Development)

For development work on the backend or frontend, you can install components manually.

### Backend Setup

**Requirements:** Python 3.11+, PostgreSQL, Redis

```bash
cd backend

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -e .

# Install dev dependencies (optional, for testing and linting)
pip install -e ".[dev]"

# Start development server
uvicorn app.main:app --reload --port 8000
```

The backend API will be available at http://localhost:8000.

### Frontend Setup

**Requirements:** Node.js 18+, npm

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will run on port 3002 by default.

:::warning Database Required
The backend requires PostgreSQL and Redis to be running. Use Docker for these services or install them locally.
:::

## Verifying Installation

Test your installation with these verification steps:

### 1. Check Service Health

```bash
# Docker installation
docker-compose ps

# All services should show "Up" status
```

### 2. Test API Endpoint

```bash
curl http://localhost:8001/api/v1/health
```

Expected response:

```json
{
  "status": "healthy",
  "app_name": "ChemAudit",
  "app_version": "1.0.0",
  "rdkit_version": "2025.09.3"
}
```

### 3. Test Web Interface

Open http://localhost:3002 in your browser. You should see the ChemAudit interface.

## Next Steps

Now that ChemAudit is installed:

1. **[Configure your deployment](/docs/getting-started/configuration)** - Set up environment variables and deployment profiles
2. **[Run your first validation](/docs/getting-started/first-validation)** - Learn the basics by validating a molecule
3. **[Explore the User Guide](/docs/user-guide/single-validation)** - Discover all features

## Troubleshooting

Having installation issues? Check the [Troubleshooting Guide](/docs/troubleshooting) for common solutions.
