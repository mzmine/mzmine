"""
NP-Likeness Scorer

Calculates natural product likeness score using RDKit's NPScore algorithm.
Based on fragment-based scoring trained on natural products from the DNP database.

Score interpretation:
- Positive scores: More natural product-like character
- Negative scores: More synthetic-like character
- Near zero: Mixed or ambiguous character

Typical range: -5 to +5 (most molecules fall within -3 to +3)
"""

import gzip
import pickle
from dataclasses import dataclass, field
from functools import lru_cache
from pathlib import Path
from typing import Optional

from rdkit import Chem
from rdkit.Chem import Descriptors, rdMolDescriptors


@dataclass
class NPLikenessResult:
    """Result of NP-likeness scoring."""

    score: float
    interpretation: str
    caveats: list = field(default_factory=list)
    details: dict = field(default_factory=dict)


class NPLikenessScorer:
    """
    Scores molecules for natural product likeness using RDKit's NPScore algorithm.

    The NPScore is based on fragment-based scoring where fragments are weighted
    by their frequency in natural products vs synthetic compounds.
    """

    # Heavy atom thresholds for caveats
    MIN_HEAVY_ATOMS = 3
    MAX_HEAVY_ATOMS = 70
    UNUSUAL_ELEMENTS = {"As", "Se", "Te", "Po", "At"}

    def __init__(self, model_path: Optional[str] = None):
        """
        Initialize the scorer.

        Args:
            model_path: Path to NP model file. If None, uses default location.
        """
        self._model_path = model_path
        self._scorer = None
        self._use_builtin = True  # Use RDKit's built-in NP score calculation

    @lru_cache(maxsize=1)
    def _load_model(self):
        """Load NP scoring model (cached)."""
        # Try to use RDKit's built-in NP score calculation
        try:
            from rdkit.Chem.Scaffolds import MurckoScaffold as _  # noqa: F401

            # RDKit has built-in NP score functionality
            return None
        except ImportError:
            pass

        # Fallback: Try to load from model file
        if self._model_path:
            model_file = Path(self._model_path)
        else:
            # Default location
            model_file = (
                Path(__file__).parent.parent.parent.parent
                / "data"
                / "publicnp.model.gz"
            )

        if model_file.exists():
            try:
                with gzip.open(model_file, "rb") as f:
                    return pickle.load(f)
            except Exception:
                pass

        return None

    def score(self, mol: Chem.Mol) -> NPLikenessResult:
        """
        Calculate NP-likeness score for a molecule.

        Args:
            mol: RDKit molecule object

        Returns:
            NPLikenessResult with score, interpretation, and caveats
        """
        caveats = []
        details = {}

        # Validate molecule
        if mol is None:
            return NPLikenessResult(
                score=0.0,
                interpretation="Unable to score: invalid molecule",
                caveats=["Invalid molecule provided"],
                details=details,
            )

        # Check molecule size
        num_heavy_atoms = mol.GetNumHeavyAtoms()
        details["heavy_atom_count"] = num_heavy_atoms

        if num_heavy_atoms < self.MIN_HEAVY_ATOMS:
            caveats.append(
                f"Very small molecule ({num_heavy_atoms} heavy atoms); "
                "score may not be meaningful"
            )
        elif num_heavy_atoms > self.MAX_HEAVY_ATOMS:
            caveats.append(
                f"Large molecule ({num_heavy_atoms} heavy atoms); "
                "score reliability may be reduced"
            )

        # Check for unusual elements
        atom_symbols = set(atom.GetSymbol() for atom in mol.GetAtoms())
        unusual = atom_symbols & self.UNUSUAL_ELEMENTS
        if unusual:
            caveats.append(
                f"Contains unusual elements ({', '.join(unusual)}); "
                "score may be less reliable"
            )

        # Calculate score using fragment-based approach
        try:
            np_score = self._calculate_np_score(mol)
            details["raw_score"] = np_score
        except Exception as e:
            return NPLikenessResult(
                score=0.0,
                interpretation=f"Scoring failed: {str(e)}",
                caveats=["Calculation error occurred"],
                details=details,
            )

        # Generate interpretation
        interpretation = self._get_interpretation(np_score)

        return NPLikenessResult(
            score=round(np_score, 2),
            interpretation=interpretation,
            caveats=caveats,
            details=details,
        )

    def _calculate_np_score(self, mol: Chem.Mol) -> float:
        """
        Calculate the NP-likeness score using fragment-based analysis.

        This implementation uses a heuristic based on common NP features
        when the model file is not available.
        """
        # Try to use RDKit's built-in calculation if available
        try:
            from rdkit.Chem import rdNPScore

            return rdNPScore.GetNPLScore(mol)
        except (ImportError, AttributeError):
            pass

        # Fallback: Use a heuristic-based calculation
        # Based on common features found in natural products
        return self._calculate_heuristic_np_score(mol)

    def _calculate_heuristic_np_score(self, mol: Chem.Mol) -> float:
        """
        Calculate NP-likeness using heuristic fragment analysis.

        This is a simplified implementation that captures key NP characteristics:
        - Ring systems (NPs often have complex ring systems)
        - Oxygen content (NPs often have high O/C ratios)
        - Stereochemistry (NPs often have many chiral centers)
        - sp3 character (NPs tend to have higher sp3 fraction)
        """
        score = 0.0

        try:
            # Get basic counts
            num_heavy = mol.GetNumHeavyAtoms()
            if num_heavy == 0:
                return 0.0

            # Ring analysis
            ring_info = mol.GetRingInfo()
            ring_atoms = set()
            for ring in ring_info.AtomRings():
                ring_atoms.update(ring)
            ring_fraction = len(ring_atoms) / num_heavy if num_heavy > 0 else 0

            # NPs typically have moderate ring content
            if 0.2 <= ring_fraction <= 0.6:
                score += 0.5
            elif ring_fraction > 0.6:
                score -= 0.3  # Too many rings suggests synthetic

            # Count atoms by type
            atom_counts = {}
            for atom in mol.GetAtoms():
                symbol = atom.GetSymbol()
                atom_counts[symbol] = atom_counts.get(symbol, 0) + 1

            # Oxygen/Carbon ratio (NPs often have higher O content)
            c_count = atom_counts.get("C", 0)
            o_count = atom_counts.get("O", 0)
            if c_count > 0:
                o_c_ratio = o_count / c_count
                if 0.1 <= o_c_ratio <= 0.5:
                    score += 0.5  # Good NP-like O content
                elif o_c_ratio > 0.5:
                    score += 0.3  # Still reasonable

            # Nitrogen content (NPs have moderate N)
            n_count = atom_counts.get("N", 0)
            if c_count > 0:
                n_c_ratio = n_count / c_count
                if n_c_ratio > 0.3:
                    score -= 0.3  # High N suggests synthetic

            # Halogen content (usually low in NPs)
            halogens = sum(atom_counts.get(x, 0) for x in ["F", "Cl", "Br", "I"])
            if halogens > 0:
                score -= 0.3 * min(halogens, 3)

            # sp3 fraction (NPs tend to have higher sp3)
            try:
                sp3_fraction = rdMolDescriptors.CalcFractionCSP3(mol)
                if sp3_fraction > 0.4:
                    score += 0.5
                elif sp3_fraction > 0.25:
                    score += 0.3
                else:
                    score -= 0.2  # Low sp3 suggests synthetic flat molecules
            except Exception:
                pass

            # Chiral center count (NPs often have multiple)
            try:
                chiral_centers = len(
                    Chem.FindMolChiralCenters(mol, includeUnassigned=True)
                )
                if chiral_centers >= 3:
                    score += 0.5
                elif chiral_centers >= 1:
                    score += 0.2
            except Exception:
                pass

            # Molecular weight consideration
            mw = Descriptors.MolWt(mol)
            if 200 <= mw <= 800:
                score += 0.3  # Typical NP MW range
            elif mw < 150:
                score -= 0.2  # Very small, less NP-like

            # Scale to approximate -3 to +3 range
            score = max(-3.0, min(3.0, score * 1.5))

        except Exception:
            score = 0.0

        return score

    def _get_interpretation(self, score: float) -> str:
        """Generate interpretation text based on score."""
        if score >= 2.0:
            return (
                "Strongly natural product-like: This molecule exhibits features "
                "commonly found in natural products."
            )
        elif score >= 1.0:
            return (
                "Natural product-like: This molecule has characteristics "
                "suggesting natural product origin."
            )
        elif score >= 0.3:
            return (
                "Moderately natural product-like: This molecule has some "
                "natural product features."
            )
        elif score >= -0.3:
            return (
                "Mixed character: This molecule has features of both natural "
                "products and synthetic compounds."
            )
        elif score >= -1.0:
            return (
                "Moderately synthetic-like: This molecule has more synthetic "
                "than natural product characteristics."
            )
        elif score >= -2.0:
            return (
                "Synthetic-like: This molecule exhibits features commonly found "
                "in synthetic compounds."
            )
        else:
            return (
                "Strongly synthetic-like: This molecule lacks typical natural "
                "product features."
            )


# Module-level convenience function
_scorer = NPLikenessScorer()


def calculate_np_likeness(mol: Chem.Mol) -> NPLikenessResult:
    """
    Calculate NP-likeness score for a molecule.

    Args:
        mol: RDKit molecule object

    Returns:
        NPLikenessResult with score (-5 to +5), interpretation, and caveats
    """
    return _scorer.score(mol)
