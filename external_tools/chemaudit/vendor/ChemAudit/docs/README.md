<div align="center">

<img src="assets/logo.png" alt="ChemAudit" width="100" />

# Documentation

### ChemAudit Documentation Hub

</div>

---

## 🗂️ Quick Navigation

| Document | Description |
|----------|-------------|
| [**Getting Started**](GETTING_STARTED.md) | Installation and first steps |
| [**User Guide**](USER_GUIDE.md) | Complete feature walkthrough |
| [**API Reference**](API_REFERENCE.md) | REST API documentation |
| [**Deployment**](DEPLOYMENT.md) | Production deployment guide |
| [**Troubleshooting**](TROUBLESHOOTING.md) | Common issues and solutions |

---

## 🎯 Quick Start

```bash
# Clone and start
git clone https://github.com/Kohulan/ChemAudit.git
cd chemaudit

# Create .env with required secrets
cp .env.example .env
# Edit .env to set POSTGRES_PASSWORD, SECRET_KEY, API_KEY_ADMIN_SECRET, CSRF_SECRET_KEY

# Development mode
docker-compose up -d
```

**Access (Development):**
- 🌐 Web UI: http://localhost:3002
- 📖 API Docs: http://localhost:8001/api/v1/docs
- 📖 API ReDoc: http://localhost:8001/api/v1/redoc

**Access (Production):**
- 🌐 Web UI + API: http://localhost (behind Nginx)

---

## ✨ Key Features

| Feature | Description |
|---------|-------------|
| **Single Validation** | Validate SMILES, InChI, or MOL blocks with detailed structural checks |
| **Batch Processing** | Process up to 1M molecules with real-time WebSocket progress |
| **Structural Alerts** | Screen against PAINS, BRENK, NIH, ZINC, and ChEMBL catalogs (~1500+ patterns) |
| **Scoring** | ML-readiness, drug-likeness (Lipinski/QED/Veber/Ghose/Egan/Muegge), ADMET, NP-likeness, scaffold analysis, aggregator likelihood |
| **Standardization** | ChEMBL-compatible pipeline: sanitize, get parent, remove salts, optional tautomer canonicalization |
| **Database Lookup** | Cross-reference PubChem, ChEMBL, and COCONUT |
| **Export** | CSV, Excel, SDF, JSON, and PDF report formats |

---

## 📖 Documentation Overview

### For Users

| Guide | What You'll Learn |
|-------|-------------------|
| [Getting Started](GETTING_STARTED.md) | Install, configure, validate your first molecule |
| [User Guide](USER_GUIDE.md) | All features: batch processing, alerts, scoring, standardization, database lookup, export |
| [Troubleshooting](TROUBLESHOOTING.md) | Fix common issues |

### For Developers

| Guide | What You'll Learn |
|-------|-------------------|
| [API Reference](API_REFERENCE.md) | All endpoints, parameters, request/response schemas, rate limits, WebSocket, API key auth |
| [Deployment](DEPLOYMENT.md) | Production setup with Docker, Nginx, deployment profiles, SSL, monitoring |

---

## 🔗 External Resources

- [RDKit Documentation](https://www.rdkit.org/docs/)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [React Documentation](https://react.dev/)

---

<div align="center">

**Questions?** [Open an Issue](https://github.com/Kohulan/ChemAudit/issues)

</div>
