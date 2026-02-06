---
sidebar_position: 1
title: Introduction
slug: /
description: ChemAudit - Comprehensive chemical structure validation suite for drug discovery and research
---

# ChemAudit

**ChemAudit** is a comprehensive web-based chemical structure validation suite designed for drug discovery, medicinal chemistry, and cheminformatics research. It combines powerful validation, standardization, and scoring capabilities into an intuitive interface.

## What ChemAudit Does

ChemAudit helps you ensure chemical structure quality across your research workflow:

- **Validate** molecules with detailed structural checks
- **Screen** for problematic substructures using industry-standard alert catalogs
- **Score** molecules for ML-readiness, drug-likeness, and ADMET properties
- **Standardize** structures using the ChEMBL-compatible pipeline
- **Process** batches of up to 1 million molecules with real-time progress tracking
- **Export** results in multiple formats (CSV, Excel, SDF, JSON, PDF)

## Key Features

### Single Molecule Validation

Validate SMILES, InChI, or MOL blocks with detailed structural checks including valence, kekulization, sanitization, stereochemistry, and representation quality.

### Batch Processing

Process large datasets with up to 1 million molecules depending on deployment configuration. Real-time WebSocket progress updates keep you informed during processing.

### Structural Alerts

Screen against over 1,500 patterns from PAINS, BRENK, NIH, ZINC, and ChEMBL catalogs. Identify potential assay interference compounds and unwanted chemical moieties before investing time and resources.

### Comprehensive Scoring

Evaluate molecules across multiple dimensions:

- **ML-readiness**: Descriptor calculability, fingerprint generation, size constraints
- **Drug-likeness**: Lipinski, QED, Veber, Rule of Three, Ghose, Egan, Muegge filters
- **ADMET**: Synthetic accessibility, solubility, CNS penetration, bioavailability
- **NP-likeness**: Natural product vs. synthetic classification
- **Scaffold analysis**: Murcko scaffold extraction
- **Aggregator likelihood**: Colloidal aggregation risk assessment

### ChEMBL-Compatible Standardization

Standardize structures using a pipeline compatible with ChEMBL's curation workflow:

- Structural issue detection
- Salt and solvent removal
- Parent molecule extraction
- Optional tautomer canonicalization

### Database Integration

Cross-reference molecules against:

- **PubChem**: Properties, synonyms, IUPAC names
- **ChEMBL**: Bioactivity data, targets, clinical phase
- **COCONUT**: Natural product sources and organisms

## Quick Start

Get ChemAudit running in minutes with Docker:

```bash
# Clone the repository
git clone https://github.com/Kohulan/ChemAudit.git
cd chemaudit

# Create environment file
cp .env.example .env
# Edit .env to set required secrets (POSTGRES_PASSWORD, SECRET_KEY, etc.)

# Start all services
docker-compose up -d
```

**Access Points (Development):**

- Web UI: http://localhost:3002
- API Documentation: http://localhost:8001/api/v1/docs
- API ReDoc: http://localhost:8001/api/v1/redoc

**Access Points (Production):**

- Web UI and API: http://localhost (behind Nginx)

## Navigation

Ready to get started? Here's where to go next:

- **[Getting Started](/docs/getting-started/installation)** - Install and configure ChemAudit
- **[User Guide](/docs/user-guide/single-validation)** - Learn all features
- **[API Reference](/docs/api/overview)** - Integrate ChemAudit into your workflow
- **[Deployment](/docs/deployment/docker)** - Deploy to production
- **[Troubleshooting](/docs/troubleshooting)** - Solve common issues

## External Resources

- [RDKit Documentation](https://www.rdkit.org/docs/)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [React Documentation](https://react.dev/)

## Support

Questions or issues? [Open an issue on GitHub](https://github.com/Kohulan/ChemAudit/issues)
