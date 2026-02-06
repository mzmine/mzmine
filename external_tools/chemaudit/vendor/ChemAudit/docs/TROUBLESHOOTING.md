<div align="center">

<img src="assets/logo.png" alt="ChemAudit" width="80" />

# Troubleshooting Guide

### Common Issues and Solutions

</div>

---

## 📋 Table of Contents

- [Installation Issues](#-installation-issues)
- [Validation Errors](#-validation-errors)
- [Batch Processing Issues](#-batch-processing-issues)
- [API Errors](#-api-errors)
- [Performance Issues](#-performance-issues)
- [Docker Issues](#-docker-issues)

---

## 💻 Installation Issues

### Docker containers won't start

<details>
<summary><b>🔴 Error: Port already in use</b></summary>

**Symptoms:**
```
Error starting userland proxy: listen tcp 0.0.0.0:8001: bind: address already in use
```

**Solution:**
```bash
# Find what's using the port
lsof -i :8001

# Kill the process or change the port in docker-compose.yml
```

</details>

<details>
<summary><b>🔴 Error: Cannot connect to Docker daemon</b></summary>

**Symptoms:**
```
Cannot connect to the Docker daemon at unix:///var/run/docker.sock
```

**Solution:**
```bash
# Start Docker service
sudo systemctl start docker

# Or on macOS, launch Docker Desktop

# Verify Docker is running
docker ps
```

</details>

<details>
<summary><b>🔴 Error: Out of memory</b></summary>

**Symptoms:**
Container exits with code 137

**Solution:**
Increase Docker memory allocation:
- **Docker Desktop**: Settings → Resources → Memory → 4GB+
- **Linux**: Check system memory with `free -h`

</details>

---

### Frontend won't load

<details>
<summary><b>🔴 Blank page / React errors</b></summary>

**Check browser console for errors, then:**

```bash
# Clear and rebuild
cd frontend
rm -rf node_modules dist
npm install
npm run dev
```

</details>

<details>
<summary><b>🔴 Cannot connect to API</b></summary>

**Verify backend is running:**
```bash
# Dev mode (mapped to port 8001)
curl http://localhost:8001/api/v1/health

# Production (behind Nginx on port 80)
curl http://localhost/api/v1/health
```

**Check CORS settings in `.env`:**
```env
# Comma-separated string (not JSON array)
CORS_ORIGINS_STR=http://localhost:3002,http://127.0.0.1:3002
```

</details>

---

## 🧪 Validation Errors

### Common molecule parsing errors

<details>
<summary><b>🔴 "Invalid SMILES string"</b></summary>

**Common causes:**
1. Typos in SMILES
2. Unclosed rings
3. Invalid atom symbols

**How to fix:**
```python
# Validate with RDKit first
from rdkit import Chem

mol = Chem.MolFromSmiles("your_smiles")
if mol is None:
    print("Invalid SMILES")
```

**Example fixes:**

| Invalid | Valid | Issue |
|---------|-------|-------|
| `C1CCC1` | `C1CCC1` | Ring not closed (should be C1CCCC1 for cyclopentane) |
| `CC(C)(C)(C)C` | `CC(C)(C)C` | Too many substituents on carbon |
| `c1cccc1` | `c1ccccc1` | Benzene needs 6 carbons |

</details>

<details>
<summary><b>🔴 "Kekulization failed"</b></summary>

**Cause:** Invalid aromatic system

**Solution:** Check aromatic ring electron count follows Hückel's rule (4n+2)

```
# Invalid - 8 pi electrons
c1cccccccc1

# Valid - 6 pi electrons
c1ccccc1
```

</details>

<details>
<summary><b>🔴 "Valence error on atom X"</b></summary>

**Cause:** Atom has too many or too few bonds

**Common examples:**

| Error | Cause | Fix |
|-------|-------|-----|
| Carbon with 5 bonds | Missing charge | Add `[C+]` or fix structure |
| Nitrogen with 4 bonds | Missing charge | Use `[N+]` for quaternary N |
| Oxygen with 3 bonds | Invalid structure | Check bonding |

</details>

---

## 📦 Batch Processing Issues

<details>
<summary><b>🔴 "File too large"</b></summary>

**Default limits (configurable per deployment profile):**
- Maximum file size: 500 MB (default), up to 1 GB
- Maximum molecules: 10,000 (default), up to 1,000,000

**Check your deployment profile limits:**
```bash
curl http://localhost:8001/api/v1/config
```

**If you need larger:**
Use a bigger deployment profile (`./deploy.sh large`) or split your file:
```bash
# Split SDF file
split -l 10000 large_file.sdf chunk_

# Split CSV file
split -l 100000 large_file.csv chunk_
```

</details>

<details>
<summary><b>🔴 Progress stuck at 0%</b></summary>

**Check Celery worker:**
```bash
docker-compose logs celery-worker
```

**Check Redis connection:**
```bash
docker-compose exec redis redis-cli ping
# Should return: PONG
```

**Restart workers:**
```bash
docker-compose restart celery-worker
```

</details>

<details>
<summary><b>🔴 CSV column not detected</b></summary>

**Ensure your CSV has headers:**
```csv
SMILES,Name,Activity
CCO,Ethanol,Active
```

**Supported SMILES column names:**
- `SMILES`
- `smiles`
- `Smiles`
- `CANONICAL_SMILES`
- `canonical_smiles`

</details>

<details>
<summary><b>🔴 Many molecules failing</b></summary>

**Common causes:**
1. Wrong file encoding (use UTF-8)
2. Corrupted SDF records
3. Invalid SMILES in CSV

**Debug steps:**
```bash
# Check file encoding
file your_file.csv

# Convert to UTF-8 if needed
iconv -f ISO-8859-1 -t UTF-8 input.csv > output.csv

# Validate SDF structure
grep -c '$$$$' your_file.sdf  # Count molecules
```

</details>

---

## 🔌 API Errors

<details>
<summary><b>🔴 HTTP 429 - Rate Limited</b></summary>

**You've exceeded the rate limit.**

**Default anonymous limits:**
| Endpoint | Limit |
|----------|-------|
| `/validate` | 10/min |
| `/validate/async` | 10/min |
| `/score` | 10/min |
| `/alerts` | 10/min |
| `/standardize` | 10/min |
| `/batch/upload` | 10/min |
| `/batch/{id}` (results) | 60/min |
| `/integrations/*` | 30/min |

**Solution:**
- Wait and retry with exponential backoff
- Use an API key for higher limits
- Use batch processing for many molecules

</details>

<details>
<summary><b>🔴 HTTP 500 - Internal Server Error</b></summary>

**Check backend logs:**
```bash
docker-compose logs backend
```

**Common causes:**
1. Database connection lost
2. Redis unavailable
3. Out of memory

**Recovery:**
```bash
docker-compose restart backend
```

</details>

<details>
<summary><b>🔴 HTTP 504 - Gateway Timeout</b></summary>

**Request took too long.**

**For single molecules:** Likely very complex structure

**For batch:** Normal for large files - use WebSocket for progress

**Increase timeout (if needed):**
Edit `nginx/nginx.conf`:
```nginx
proxy_read_timeout 600s;
```

</details>

---

## ⚡ Performance Issues

<details>
<summary><b>🔴 Slow validation responses</b></summary>

**Possible causes:**

1. **First request cold start** - RDKit initialization
   - Solution: Wait ~10s after startup

2. **Complex molecules** - Many rings, stereocenters
   - Expected for complex structures

3. **Resource constraints**
   ```bash
   # Check container resources
   docker stats
   ```

</details>

<details>
<summary><b>🔴 Batch processing too slow</b></summary>

**Scale workers (adjust CELERY_WORKERS in .env):**
```bash
# Production uses separate default and priority worker containers
# Increase concurrency via environment variable
CELERY_WORKERS=8 docker-compose -f docker-compose.prod.yml up -d celery-worker
```

**Check worker utilization:**
```bash
docker-compose exec celery-worker celery -A app.celery_app inspect active
```

**Tune PostgreSQL:**
```yaml
# docker-compose.yml
postgres:
  command: postgres -c shared_buffers=256MB -c max_connections=200
```

</details>

<details>
<summary><b>🔴 High memory usage</b></summary>

**RDKit caches molecules in memory.**

**Solutions:**
1. Restart containers periodically
2. Limit Redis memory:
   ```yaml
   redis:
     command: redis-server --maxmemory 512mb --maxmemory-policy allkeys-lru
   ```

</details>

---

## 🐳 Docker Issues

<details>
<summary><b>🔴 Images won't build</b></summary>

**Clear Docker cache:**
```bash
docker system prune -a
docker-compose build --no-cache
```

</details>

<details>
<summary><b>🔴 Container keeps restarting</b></summary>

**Check logs:**
```bash
docker-compose logs --tail=50 <service_name>
```

**Common fixes:**
```bash
# Recreate containers
docker-compose down
docker-compose up -d
```

</details>

<details>
<summary><b>🔴 Database connection refused</b></summary>

**Ensure postgres is healthy:**
```bash
docker-compose exec postgres pg_isready -U chemaudit
```

**Check connection string in `.env`:**
```env
DATABASE_URL=postgresql+asyncpg://chemaudit:password@postgres:5432/chemaudit
```

Note: Use `postgres` (service name) not `localhost` inside Docker.

</details>

---

## 🆘 Still Need Help?

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

### Get Support

1. **Search existing issues:** [GitHub Issues](https://github.com/Kohulan/ChemAudit/issues)
2. **Create new issue** with:
   - Description of the problem
   - Steps to reproduce
   - Debug information collected above
   - Screenshots (if applicable)

---

<div align="center">

**Can't find your issue?** [Open a GitHub Issue](https://github.com/Kohulan/ChemAudit/issues/new)

</div>
