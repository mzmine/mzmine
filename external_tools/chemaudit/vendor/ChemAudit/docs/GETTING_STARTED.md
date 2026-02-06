<div align="center">

<img src="assets/logo.png" alt="ChemAudit" width="80" />

# Getting Started

### Your First Steps with ChemAudit

</div>

---

## Table of Contents

- [Installation](#installation)
- [Your First Validation](#your-first-validation)
- [Understanding Results](#understanding-results)
- [Next Steps](#next-steps)

---

## Installation

### Option 1: Docker (Recommended)

The fastest way to get started:

```bash
# Clone the repository
git clone https://github.com/Kohulan/ChemAudit.git
cd chemaudit

# Create a .env file with required secrets
cp .env.example .env
# Edit .env to set POSTGRES_PASSWORD, SECRET_KEY, API_KEY_ADMIN_SECRET, CSRF_SECRET_KEY, GRAFANA_PASSWORD

# Start all services (development mode)
docker-compose up -d

# Wait for services to be ready
docker-compose logs -f
```

**Access Points:**

| Service | URL |
|---------|-----|
| **Web Interface** | http://localhost:3002 |
| **API Documentation** | http://localhost:8001/api/v1/docs |
| **API ReDoc** | http://localhost:8001/api/v1/redoc |

### Option 1b: Production Deployment

For production use with configurable batch limits:

```bash
# Interactive deployment - select a profile
./deploy.sh

# Or specify profile directly
./deploy.sh medium   # 10K molecules, 4 workers
./deploy.sh large    # 50K molecules, 8 workers
./deploy.sh coconut  # 1M molecules, 16 workers
```

See [Deployment Guide](DEPLOYMENT.md) for full production setup.

### Option 2: Manual Installation

<details>
<summary><b>Backend Setup</b></summary>

**Requirements:** Python 3.11+, PostgreSQL, Redis

```bash
cd backend

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -e .

# Install dev dependencies (optional)
pip install -e ".[dev]"

# Start development server
uvicorn app.main:app --reload --port 8000
```

</details>

<details>
<summary><b>Frontend Setup</b></summary>

**Requirements:** Node.js 18+, npm

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

Frontend runs on port 3002 by default.

</details>

---

## Your First Validation

### Using the Web Interface

1. **Open** http://localhost:3002
2. **Enter** a SMILES string in the input field:
   ```
   CC(=O)Oc1ccccc1C(=O)O
   ```
   *(This is Aspirin)*
3. **Click** "Validate"
4. **View** your results including validation score, structural checks, alerts, scoring, and standardization.

### Using the API

```bash
curl -X POST http://localhost:8001/api/v1/validate \
  -H "Content-Type: application/json" \
  -d '{
    "molecule": "CC(=O)Oc1ccccc1C(=O)O",
    "format": "smiles"
  }'
```

<details>
<summary><b>Example Response</b></summary>

```json
{
  "status": "completed",
  "molecule_info": {
    "input_smiles": "CC(=O)Oc1ccccc1C(=O)O",
    "canonical_smiles": "CC(=O)OC1=CC=CC=C1C(O)=O",
    "inchi": "InChI=1S/C9H8O4/c1-6(10)13-8-5-3-2-4-7(8)9(11)12/h2-5H,1H3,(H,11,12)",
    "inchikey": "BSYNRYMUTXBXSQ-UHFFFAOYSA-N",
    "molecular_formula": "C9H8O4",
    "molecular_weight": 180.16,
    "num_atoms": 13
  },
  "overall_score": 95,
  "issues": [],
  "all_checks": [
    {
      "check_name": "valence_check",
      "passed": true,
      "severity": "critical",
      "message": "All atoms have valid valence",
      "affected_atoms": [],
      "details": {}
    }
  ],
  "execution_time_ms": 12,
  "cached": false
}
```

</details>

### Using Python

```python
import requests

response = requests.post(
    "http://localhost:8001/api/v1/validate",
    json={
        "molecule": "CC(=O)Oc1ccccc1C(=O)O",
        "format": "smiles"
    }
)

result = response.json()
print(f"Score: {result['overall_score']}")
print(f"Issues: {len(result['issues'])}")
print(f"Formula: {result['molecule_info']['molecular_formula']}")
```

---

## Understanding Results

### Validation Score

| Score Range | Quality | Recommendation |
|-------------|---------|----------------|
| **90-100** | Excellent | Ready for use |
| **70-89** | Good | Minor issues, review recommended |
| **50-69** | Fair | Needs attention |
| **0-49** | Poor | Significant issues |

### Check Severities

| Severity | Meaning |
|----------|---------|
| **Critical** | Must be fixed - structure is invalid |
| **Warning** | Should be reviewed - may affect results |
| **Info** | Informational - no action required |

### Common Validation Checks

| Check | What It Does |
|-------|--------------|
| **Valence** | Verifies all atoms have correct number of bonds |
| **Kekulization** | Tests if aromatic rings can be kekulized |
| **Sanitization** | Validates RDKit can sanitize the molecule |
| **Stereochemistry** | Checks undefined stereocenters and consistency |
| **Representation** | Validates SMILES length and InChI generation |

---

## Next Steps

Now that you've completed your first validation, explore more features:

<table>
<tr>
<td width="50%" valign="top">

### Learn More

- [User Guide](USER_GUIDE.md) - Complete feature walkthrough
- [API Reference](API_REFERENCE.md) - Full API documentation
- [Troubleshooting](TROUBLESHOOTING.md) - Common issues & solutions

</td>
<td width="50%" valign="top">

### Try Features

- **Batch Processing** - Validate thousands of molecules at once
- **Structural Alerts** - Screen for PAINS, BRENK, NIH, ZINC, ChEMBL
- **Scoring** - Drug-likeness, ML-readiness, ADMET, NP-likeness
- **Standardization** - ChEMBL-compatible structure cleanup
- **Database Lookup** - Search PubChem, ChEMBL, COCONUT
- **Export** - Download results as CSV, Excel, SDF, JSON, or PDF

</td>
</tr>
</table>

---

## Sample Molecules to Try

| Name | SMILES | Description |
|------|--------|-------------|
| Aspirin | `CC(=O)Oc1ccccc1C(=O)O` | Common pain reliever |
| Caffeine | `Cn1cnc2c1c(=O)n(c(=O)n2C)C` | Stimulant |
| Ibuprofen | `CC(C)Cc1ccc(cc1)C(C)C(=O)O` | Anti-inflammatory |
| Penicillin G | `CC1(C)SC2C(NC(=O)Cc3ccccc3)C(=O)N2C1C(=O)O` | Antibiotic |
| Morphine | `CN1CCC23C4C1CC5=C2C(=C(C=C5)O)OC3C(C=C4)O` | Analgesic |

---

<div align="center">

**Questions?** Check the [FAQ](TROUBLESHOOTING.md) or [Open an Issue](https://github.com/Kohulan/ChemAudit/issues)

</div>
