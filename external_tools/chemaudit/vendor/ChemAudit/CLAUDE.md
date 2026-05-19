# ChemAudit Project

## Overview
ChemAudit is a comprehensive web-based chemical structure validation suite.
React frontend + FastAPI backend for validating, standardizing, and assessing ML-readiness of chemical structures.

## Documentation
- `docs/` - Public-facing documentation (README, Getting Started, API Reference, User Guide, Deployment, Troubleshooting)
- `local_docs/` - Internal planning docs (PRD, Architecture, Phases, Skills)
- `.planning/` - GSD planning artifacts (research, phases, codebase maps)

## Tech Stack
- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS, Framer Motion, RDKit.js, Recharts, Axios
- **Backend**: Python 3.11+, FastAPI, RDKit, MolVS, chembl-structure-pipeline, Pydantic v2
- **Database**: PostgreSQL 16 with asyncpg
- **Queue**: Redis 7 + Celery for batch processing (default + priority queues)
- **Monitoring**: Prometheus + Grafana (optional profile)
- **Proxy**: Nginx (production)
- **Testing**: pytest (backend), Vitest (frontend)

## Commands
```bash
# Backend
cd backend
pip install -e .            # Install dependencies
pip install -e ".[dev]"     # Install with dev dependencies
uvicorn app.main:app --reload --port 8000  # Start dev server
pytest                      # Run tests

# Frontend
cd frontend
npm install                 # Install dependencies
npm run dev                 # Start dev server (port 3002)
npm test                    # Run tests

# Docker (development)
docker-compose up -d        # Start all services
docker-compose logs -f      # View logs

# Docker (production)
./deploy.sh medium          # Deploy with profile (small/medium/large/xl/coconut)
```

## Code Style
- Python: Ruff (lint + format, line-length=100), type hints required
- TypeScript: ESLint + strict mode
- Always include docstrings for public functions
- Write tests for new features

## Architecture
- Backend routes: `backend/app/api/routes/` (validation, alerts, scoring, standardization, batch, export, integrations, api_keys, config, health)
- Validation checks: `backend/app/services/validation/checks/` (inherit from BaseCheck)
- Schemas: `backend/app/schemas/` (Pydantic v2 models)
- Frontend pages: `frontend/src/pages/` (SingleValidation, BatchValidation, About, Privacy)
- Frontend API: `frontend/src/services/api.ts`
- Security: API key auth (Redis-backed), CSRF protection, rate limiting (slowapi), IP banning

## Important Patterns
- All validation checks inherit from BaseCheck abstract class
- Use Pydantic models for request/response schemas
- Celery uses two queues: `default` (batch) and `high_priority` (single validation)
- WebSocket at `/ws/batch/{job_id}` for real-time batch progress
- Redis caching for validation results (keyed by InChIKey)
- Deploy profiles in `config/*.yml` control batch limits and worker counts