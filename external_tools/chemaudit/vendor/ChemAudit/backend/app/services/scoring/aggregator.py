"""
Aggregator Likelihood Scorer

Predicts the likelihood of a compound forming colloidal aggregates in assays.
Aggregators can cause false positives in HTS campaigns.

Based on:
- Shoichet Lab aggregator advisor (aggregator.orchard.ucsf.edu)
- Irwin et al. (2015) - "An Aggregation Advisor for Ligand Discovery"
"""

from dataclasses import dataclass, field
from typing import List

from rdkit import Chem
from rdkit.Chem import Crippen, Descriptors


@dataclass
class AggregatorLikelihoodResult:
    """Aggregator likelihood prediction result."""

    likelihood: str  # "low", "moderate", "high"
    risk_score: float  # 0-1 scale
    logp: float
    tpsa: float
    mw: float
    aromatic_rings: int
    risk_factors: List[str] = field(default_factory=list)
    interpretation: str = ""


# Known aggregator SMILES patterns (representative examples)
# These are simplified SMARTS patterns for common aggregator scaffolds
AGGREGATOR_PATTERNS = [
    # Rhodanines
    ("[#6]1=[#6]C(=O)N([#6,#1])C1=S", "Rhodanine"),
    # Quinones
    ("O=C1C=CC(=O)C=C1", "Quinone"),
    # Catechols (can aggregate at high concentrations)
    ("c1cc(O)c(O)cc1", "Catechol"),
    # Curcumin-like
    ("CC(=O)C=Cc1ccc(O)c(OC)c1", "Curcumin-like"),
    # Phenol-sulfonamides with extended conjugation
    ("c1ccc(S(=O)(=O)N)cc1", "Sulfonamide"),
    # Flavonoids (some can aggregate)
    ("O=C1CC(c2ccccc2)Oc2ccccc12", "Flavone scaffold"),
    # Long chain fatty acids/lipids
    ("CCCCCCCCCCCC", "Long aliphatic chain"),
]


class AggregatorScorer:
    """
    Predicts likelihood of compound aggregation.

    Uses multiple indicators:
    1. Lipophilicity (LogP) - high LogP increases aggregation risk
    2. Molecular size - larger molecules may aggregate more
    3. Aromatic content - highly aromatic compounds may stack
    4. Structural patterns - known aggregator scaffolds
    5. TPSA - low TPSA (high hydrophobicity) increases risk
    """

    # Thresholds based on literature
    LOGP_HIGH_RISK = 4.0  # LogP > 4 increases aggregation risk
    LOGP_MODERATE_RISK = 3.0
    TPSA_LOW_RISK = 40  # TPSA < 40 increases risk
    MW_HIGH = 500  # Large molecules may aggregate
    AROMATIC_RINGS_HIGH = 4  # Many aromatic rings increase stacking

    def __init__(self):
        """Initialize aggregator patterns."""
        self._patterns = []
        for smarts, name in AGGREGATOR_PATTERNS:
            try:
                pattern = Chem.MolFromSmarts(smarts)
                if pattern:
                    self._patterns.append((pattern, name))
            except Exception:
                pass

    def score(self, mol: Chem.Mol) -> AggregatorLikelihoodResult:
        """
        Predict aggregator likelihood for a molecule.

        Args:
            mol: RDKit molecule object

        Returns:
            AggregatorLikelihoodResult with prediction
        """
        risk_factors = []
        risk_score = 0.0

        # Calculate descriptors
        logp = Crippen.MolLogP(mol)
        tpsa = Descriptors.TPSA(mol)
        mw = Descriptors.MolWt(mol)
        aromatic_rings = Descriptors.NumAromaticRings(mol)

        # LogP risk assessment
        if logp > self.LOGP_HIGH_RISK:
            risk_score += 0.3
            risk_factors.append(f"High lipophilicity (LogP={logp:.1f}>4)")
        elif logp > self.LOGP_MODERATE_RISK:
            risk_score += 0.15
            risk_factors.append(f"Moderate lipophilicity (LogP={logp:.1f}>3)")

        # TPSA risk assessment (low TPSA = high hydrophobicity)
        if tpsa < self.TPSA_LOW_RISK:
            risk_score += 0.2
            risk_factors.append(f"Low polarity (TPSA={tpsa:.0f}<40)")

        # Aromatic ring stacking risk
        if aromatic_rings >= self.AROMATIC_RINGS_HIGH:
            risk_score += 0.2
            risk_factors.append(f"Many aromatic rings ({aromatic_rings})")
        elif aromatic_rings >= 3:
            risk_score += 0.1
            risk_factors.append(f"Multiple aromatic rings ({aromatic_rings})")

        # Molecular size
        if mw > self.MW_HIGH:
            risk_score += 0.1
            risk_factors.append(f"Large molecule (MW={mw:.0f}>500)")

        # Check for known aggregator scaffolds
        matched_patterns = self._check_aggregator_patterns(mol)
        if matched_patterns:
            risk_score += 0.2 * min(len(matched_patterns), 2)
            for pattern_name in matched_patterns[:3]:
                risk_factors.append(f"Contains {pattern_name} scaffold")

        # Calculate heavy atom count and check for highly conjugated systems
        num_atoms = mol.GetNumHeavyAtoms()
        num_aromatic_atoms = sum(1 for atom in mol.GetAtoms() if atom.GetIsAromatic())
        aromatic_fraction = num_aromatic_atoms / num_atoms if num_atoms > 0 else 0

        if aromatic_fraction > 0.7 and num_atoms > 20:
            risk_score += 0.15
            risk_factors.append("Highly conjugated aromatic system")

        # Cap risk score at 1.0
        risk_score = min(1.0, risk_score)

        # Determine likelihood category
        if risk_score >= 0.6:
            likelihood = "high"
            interpretation = (
                "High aggregation risk. This compound has multiple features "
                "associated with colloidal aggregation. Consider counter-screening "
                "with detergent or dynamic light scattering."
            )
        elif risk_score >= 0.3:
            likelihood = "moderate"
            interpretation = (
                "Moderate aggregation risk. Some features suggest possible "
                "aggregation. Validate hits with orthogonal assays."
            )
        else:
            likelihood = "low"
            interpretation = (
                "Low aggregation risk. Compound properties are within ranges "
                "typically associated with non-aggregating compounds."
            )

        return AggregatorLikelihoodResult(
            likelihood=likelihood,
            risk_score=round(risk_score, 2),
            logp=round(logp, 2),
            tpsa=round(tpsa, 2),
            mw=round(mw, 2),
            aromatic_rings=aromatic_rings,
            risk_factors=risk_factors,
            interpretation=interpretation,
        )

    def _check_aggregator_patterns(self, mol: Chem.Mol) -> List[str]:
        """Check for known aggregator scaffold patterns."""
        return [
            name for pattern, name in self._patterns if mol.HasSubstructMatch(pattern)
        ]


# Module-level instance
_scorer = AggregatorScorer()


def calculate_aggregator_likelihood(mol: Chem.Mol) -> AggregatorLikelihoodResult:
    """
    Predict aggregator likelihood for a molecule.

    Args:
        mol: RDKit molecule object

    Returns:
        AggregatorLikelihoodResult with prediction
    """
    return _scorer.score(mol)
