"""
ML-Readiness Scorer

Calculates how suitable a molecule is for machine learning applications.
Score breakdown:
- Standard Descriptors (35 points): CalcMolDescriptors (217 descriptors)
- Additional Descriptors (5 points): AUTOCORR2D (192) + MQN (42)
- Fingerprints (40 points): 7 fingerprint types
- Size/Elements (20 points): Molecular weight and atom count constraints
"""

from dataclasses import dataclass, field
from typing import List, Optional

from rdkit import Chem
from rdkit.Avalon import pyAvalonTools
from rdkit.Chem import Descriptors, MACCSkeys, rdFingerprintGenerator, rdMolDescriptors


@dataclass
class MLReadinessBreakdown:
    """Breakdown of ML-readiness score components."""

    # Standard descriptors (CalcMolDescriptors - 217 descriptors)
    descriptors_score: float = 0.0
    descriptors_max: float = 35.0
    descriptors_successful: int = 0
    descriptors_total: int = 0
    failed_descriptors: List[str] = field(default_factory=list)

    # Additional descriptors (AUTOCORR2D + MQN)
    additional_descriptors_score: float = 0.0
    additional_descriptors_max: float = 5.0
    autocorr2d_successful: int = 0
    autocorr2d_total: int = 192
    mqn_successful: int = 0
    mqn_total: int = 42

    # Fingerprints (7 types)
    fingerprints_score: float = 0.0
    fingerprints_max: float = 40.0
    fingerprints_successful: List[str] = field(default_factory=list)
    fingerprints_failed: List[str] = field(default_factory=list)

    # Size constraints
    size_score: float = 0.0
    size_max: float = 20.0
    molecular_weight: Optional[float] = None
    num_atoms: Optional[int] = None
    size_category: str = "unknown"


@dataclass
class MLReadinessResult:
    """Result of ML-readiness scoring."""

    score: int
    breakdown: MLReadinessBreakdown
    interpretation: str
    failed_descriptors: list


class MLReadinessScorer:
    """
    Scores molecules for ML-readiness based on descriptor calculability,
    fingerprint generation success, and size constraints.

    Fingerprint Types:
    - Morgan (ECFP-like): Circular fingerprint based on atom connectivity
    - Morgan Features (FCFP-like): Circular fingerprint with pharmacophore features
    - MACCS: 166 structural keys
    - Atom Pair: Encodes pairs of atoms and topological distances
    - Topological Torsion: Encodes torsion angle patterns
    - RDKit: Daylight-like path enumeration fingerprint
    - Avalon: Fast substructure fingerprint

    Descriptor Types:
    - Standard: 217 RDKit CalcMolDescriptors (physical, topological, functional groups)
    - AUTOCORR2D: 192 2D autocorrelation descriptors
    - MQN: 42 Molecular Quantum Numbers (atom/bond type counts)
    """

    # Fingerprint types with their point allocations (total: 40 points)
    FINGERPRINT_TYPES = [
        ("morgan", 8),  # ECFP-like circular fingerprint
        ("morgan_features", 8),  # FCFP-like with pharmacophore features
        ("maccs", 8),  # 166 structural keys
        ("atompair", 4),  # Atom pair fingerprint
        ("topological_torsion", 4),  # Torsion patterns
        ("rdkit_fp", 4),  # Daylight-like paths
        ("avalon", 4),  # Avalon substructure fingerprint
    ]

    # Size thresholds
    OPTIMAL_MW_RANGE = (100, 900)
    OPTIMAL_ATOM_RANGE = (3, 100)
    ACCEPTABLE_MW_RANGE = (50, 1200)
    ACCEPTABLE_ATOM_RANGE = (1, 150)

    def __init__(self):
        """Initialize fingerprint generators using new API to avoid deprecation warnings."""
        self._morgan_gen = rdFingerprintGenerator.GetMorganGenerator(
            radius=2, fpSize=2048
        )
        self._morgan_feat_gen = rdFingerprintGenerator.GetMorganGenerator(
            radius=2,
            fpSize=2048,
            atomInvariantsGenerator=rdFingerprintGenerator.GetMorganFeatureAtomInvGen(),
        )
        self._atompair_gen = rdFingerprintGenerator.GetAtomPairGenerator(fpSize=2048)
        self._torsion_gen = rdFingerprintGenerator.GetTopologicalTorsionGenerator(
            fpSize=2048
        )
        self._rdkit_gen = rdFingerprintGenerator.GetRDKitFPGenerator(fpSize=2048)

    def score(self, mol: Chem.Mol) -> MLReadinessResult:
        """
        Calculate ML-readiness score for a molecule.

        Args:
            mol: RDKit molecule object

        Returns:
            MLReadinessResult with score, breakdown, and interpretation
        """
        breakdown = MLReadinessBreakdown()

        # Calculate standard descriptor score (35 points max)
        self._score_descriptors(mol, breakdown)

        # Calculate additional descriptor score (5 points max)
        self._score_additional_descriptors(mol, breakdown)

        # Calculate fingerprint score (40 points max)
        self._score_fingerprints(mol, breakdown)

        # Calculate size score (20 points max)
        self._score_size(mol, breakdown)

        # Calculate total score
        total_score = int(
            breakdown.descriptors_score
            + breakdown.additional_descriptors_score
            + breakdown.fingerprints_score
            + breakdown.size_score
        )
        total_score = max(0, min(100, total_score))

        # Generate interpretation
        interpretation = self._get_interpretation(total_score, breakdown)

        return MLReadinessResult(
            score=total_score,
            breakdown=breakdown,
            interpretation=interpretation,
            failed_descriptors=breakdown.failed_descriptors,
        )

    def _score_descriptors(
        self, mol: Chem.Mol, breakdown: MLReadinessBreakdown
    ) -> None:
        """Score standard descriptor calculability (35 points max)."""
        try:
            # Use CalcMolDescriptors with missingVal=None to track failures
            # The silent=True prevents RDKit warnings from cluttering output
            descriptors = Descriptors.CalcMolDescriptors(
                mol, missingVal=None, silent=True
            )

            total = len(descriptors)
            successful = 0
            failed = []

            for name, value in descriptors.items():
                if value is not None:
                    successful += 1
                else:
                    failed.append(name)

            breakdown.descriptors_total = total
            breakdown.descriptors_successful = successful
            breakdown.failed_descriptors = failed

            if total > 0:
                breakdown.descriptors_score = breakdown.descriptors_max * (
                    successful / total
                )

        except Exception as e:
            # If descriptor calculation completely fails
            breakdown.descriptors_score = 0.0
            breakdown.failed_descriptors = [f"CalcMolDescriptors error: {str(e)}"]

    def _score_additional_descriptors(
        self, mol: Chem.Mol, breakdown: MLReadinessBreakdown
    ) -> None:
        """Score additional 2D descriptors: AUTOCORR2D + MQN (5 points max)."""
        autocorr_success = 0
        mqn_success = 0

        # Calculate AUTOCORR2D (192 values)
        try:
            autocorr2d = rdMolDescriptors.CalcAUTOCORR2D(mol)
            # Count non-None/non-NaN values
            autocorr_success = sum(
                1
                for v in autocorr2d
                if v is not None and v == v  # v == v is False for NaN
            )
        except Exception:
            autocorr_success = 0

        # Calculate MQN (42 values)
        try:
            mqn = rdMolDescriptors.MQNs_(mol)
            # MQN returns integers, count successful calculations
            mqn_success = len([v for v in mqn if v is not None])
        except Exception:
            mqn_success = 0

        breakdown.autocorr2d_successful = autocorr_success
        breakdown.mqn_successful = mqn_success

        # Calculate score based on success rate
        total_additional = breakdown.autocorr2d_total + breakdown.mqn_total
        successful_additional = autocorr_success + mqn_success

        if total_additional > 0:
            breakdown.additional_descriptors_score = (
                breakdown.additional_descriptors_max
                * (successful_additional / total_additional)
            )

    def _score_fingerprints(
        self, mol: Chem.Mol, breakdown: MLReadinessBreakdown
    ) -> None:
        """Score fingerprint generation success (40 points max)."""
        total_score = 0.0
        successful = []
        failed = []

        for fp_name, points in self.FINGERPRINT_TYPES:
            try:
                fp = self._generate_fingerprint(mol, fp_name)

                # If we get here without exception, fingerprint was successful
                if fp is not None:
                    total_score += points
                    successful.append(fp_name)
                else:
                    failed.append(fp_name)

            except Exception:
                failed.append(fp_name)

        breakdown.fingerprints_score = total_score
        breakdown.fingerprints_successful = successful
        breakdown.fingerprints_failed = failed

    def _generate_fingerprint(self, mol: Chem.Mol, fp_name: str):
        """Generate a specific fingerprint type using new generator API."""
        if fp_name == "morgan":
            return self._morgan_gen.GetFingerprint(mol)
        elif fp_name == "morgan_features":
            return self._morgan_feat_gen.GetFingerprint(mol)
        elif fp_name == "maccs":
            return MACCSkeys.GenMACCSKeys(mol)
        elif fp_name == "atompair":
            return self._atompair_gen.GetFingerprint(mol)
        elif fp_name == "topological_torsion":
            return self._torsion_gen.GetFingerprint(mol)
        elif fp_name == "rdkit_fp":
            return self._rdkit_gen.GetFingerprint(mol)
        elif fp_name == "avalon":
            return pyAvalonTools.GetAvalonFP(mol)
        else:
            return None

    def _score_size(self, mol: Chem.Mol, breakdown: MLReadinessBreakdown) -> None:
        """Score molecule size constraints (20 points max)."""
        try:
            mw = Descriptors.MolWt(mol)
            num_atoms = mol.GetNumAtoms()

            breakdown.molecular_weight = mw
            breakdown.num_atoms = num_atoms

            # Check optimal range
            mw_optimal = self.OPTIMAL_MW_RANGE[0] <= mw <= self.OPTIMAL_MW_RANGE[1]
            atoms_optimal = (
                self.OPTIMAL_ATOM_RANGE[0] <= num_atoms <= self.OPTIMAL_ATOM_RANGE[1]
            )

            # Check acceptable range
            mw_acceptable = (
                self.ACCEPTABLE_MW_RANGE[0] <= mw <= self.ACCEPTABLE_MW_RANGE[1]
            )
            atoms_acceptable = (
                self.ACCEPTABLE_ATOM_RANGE[0]
                <= num_atoms
                <= self.ACCEPTABLE_ATOM_RANGE[1]
            )

            if mw_optimal and atoms_optimal:
                breakdown.size_score = 20.0
                breakdown.size_category = "optimal"
            elif mw_acceptable and atoms_acceptable:
                breakdown.size_score = 10.0
                breakdown.size_category = "acceptable"
            else:
                breakdown.size_score = 0.0
                breakdown.size_category = "out_of_range"

        except Exception:
            breakdown.size_score = 0.0
            breakdown.size_category = "error"

    def _get_interpretation(self, score: int, breakdown: MLReadinessBreakdown) -> str:
        """Generate human-readable interpretation of the score."""
        parts = []

        # Overall assessment
        if score >= 80:
            parts.append("Excellent ML-readiness.")
        elif score >= 60:
            parts.append("Good ML-readiness with minor limitations.")
        elif score >= 40:
            parts.append("Moderate ML-readiness; some calculations may fail.")
        else:
            parts.append("Poor ML-readiness; significant computation issues likely.")

        # Descriptor summary
        total_descriptors = (
            breakdown.descriptors_total
            + breakdown.autocorr2d_total
            + breakdown.mqn_total
        )
        successful_descriptors = (
            breakdown.descriptors_successful
            + breakdown.autocorr2d_successful
            + breakdown.mqn_successful
        )
        parts.append(
            f"{successful_descriptors}/{total_descriptors} descriptors calculated "
            f"(217 standard + 192 AUTOCORR2D + 42 MQN)."
        )

        # Fingerprint summary
        fp_count = len(breakdown.fingerprints_successful)
        total_fp = len(self.FINGERPRINT_TYPES)
        parts.append(f"{fp_count}/{total_fp} fingerprint types generated.")

        # Specific issues
        if breakdown.failed_descriptors:
            count = len(breakdown.failed_descriptors)
            parts.append(f"{count} standard descriptors failed.")

        if breakdown.fingerprints_failed:
            parts.append(
                f"Failed fingerprints: {', '.join(breakdown.fingerprints_failed)}."
            )

        if breakdown.size_category == "out_of_range":
            parts.append("Molecule size outside typical ML training ranges.")
        elif breakdown.size_category == "acceptable":
            parts.append("Molecule size is within acceptable but not optimal range.")

        return " ".join(parts)


# Module-level convenience function
_scorer = MLReadinessScorer()


def calculate_ml_readiness(mol: Chem.Mol) -> MLReadinessResult:
    """
    Calculate ML-readiness score for a molecule.

    Args:
        mol: RDKit molecule object

    Returns:
        MLReadinessResult with score (0-100), breakdown, and interpretation
    """
    return _scorer.score(mol)
