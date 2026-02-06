"""
Scoring Services

Provides comprehensive molecular scoring including:
- ML-readiness scoring
- NP-likeness scoring
- Scaffold extraction
- Drug-likeness scoring (Lipinski, QED, Veber, Ro3, Ghose, Egan, Muegge)
- Safety filters (PAINS, Brenk, NIH, ZINC, ChEMBL)
- ADMET predictions (SAscore, ESOL, Fsp3, CNS MPO, Pfizer/GSK rules)
- Aggregator likelihood prediction
"""

from app.services.scoring.admet import ADMETScorer, calculate_admet
from app.services.scoring.aggregator import (
    AggregatorScorer,
    calculate_aggregator_likelihood,
)
from app.services.scoring.druglikeness import DrugLikenessScorer, calculate_druglikeness
from app.services.scoring.ml_readiness import MLReadinessScorer, calculate_ml_readiness
from app.services.scoring.np_likeness import NPLikenessScorer, calculate_np_likeness
from app.services.scoring.safety_filters import (
    SafetyFilterScorer,
    calculate_safety_filters,
    get_pains_alerts,
    is_pains_clean,
)
from app.services.scoring.scaffold import extract_scaffold

__all__ = [
    # ML Readiness
    "MLReadinessScorer",
    "calculate_ml_readiness",
    # NP Likeness
    "NPLikenessScorer",
    "calculate_np_likeness",
    # Scaffold
    "extract_scaffold",
    # Drug-likeness
    "DrugLikenessScorer",
    "calculate_druglikeness",
    # Safety Filters
    "SafetyFilterScorer",
    "calculate_safety_filters",
    "get_pains_alerts",
    "is_pains_clean",
    # ADMET
    "ADMETScorer",
    "calculate_admet",
    # Aggregator
    "AggregatorScorer",
    "calculate_aggregator_likelihood",
]
