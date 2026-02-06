"""
Stereocenter tracking for standardization.

Tracks stereocenters and double bond stereochemistry before and after
standardization to detect any stereochemistry loss.

CRITICAL: Stereochemistry loss is a common pitfall in standardization.
Always warn users when defined stereocenters are lost.
"""

from dataclasses import dataclass, field
from typing import List, Optional, Tuple

from rdkit import Chem


@dataclass
class StereoInfo:
    """Stereochemistry information for a molecule."""

    # Chiral centers: list of (atom_idx, label) where label is R/S/?
    chiral_centers: List[Tuple[int, str]] = field(default_factory=list)

    # Double bond stereo: list of (bond_idx, stereo_type) where stereo is E/Z/NONE
    double_bond_stereo: List[Tuple[int, str]] = field(default_factory=list)

    # Counts
    defined_stereocenters: int = 0
    undefined_stereocenters: int = 0
    defined_double_bond_stereo: int = 0
    undefined_double_bond_stereo: int = 0

    @property
    def total_stereocenters(self) -> int:
        """Total number of stereocenters (defined + undefined)."""
        return self.defined_stereocenters + self.undefined_stereocenters

    @property
    def total_double_bond_stereo(self) -> int:
        """Total number of double bond stereo (defined + undefined)."""
        return self.defined_double_bond_stereo + self.undefined_double_bond_stereo


@dataclass
class StereoComparison:
    """Comparison of stereochemistry before and after standardization."""

    before: StereoInfo
    after: StereoInfo

    # Changes
    stereocenters_lost: int = 0
    stereocenters_gained: int = 0
    double_bond_stereo_lost: int = 0
    double_bond_stereo_gained: int = 0

    # Warning message if any stereocenters lost
    warning: Optional[str] = None

    @property
    def has_stereo_loss(self) -> bool:
        """Returns True if any defined stereochemistry was lost."""
        return self.stereocenters_lost > 0 or self.double_bond_stereo_lost > 0


class StereoTracker:
    """
    Track stereochemistry before and after standardization.

    CRITICAL: Tautomer canonicalization can remove double-bond stereochemistry.
    This tracker helps detect such losses and warn users.
    """

    @staticmethod
    def get_stereo_info(mol: Chem.Mol) -> StereoInfo:
        """
        Extract stereochemistry information from a molecule.

        Args:
            mol: RDKit molecule object

        Returns:
            StereoInfo with chiral centers and double bond stereo
        """
        if mol is None:
            return StereoInfo()

        info = StereoInfo()

        # Get chiral centers (includeUnassigned=True to catch undefined centers)
        try:
            chiral_centers = Chem.FindMolChiralCenters(
                mol,
                includeUnassigned=True,
                includeCIP=True,
                useLegacyImplementation=False,
            )

            for atom_idx, label in chiral_centers:
                info.chiral_centers.append((atom_idx, label))
                if label in ("R", "S"):
                    info.defined_stereocenters += 1
                else:
                    info.undefined_stereocenters += 1
        except Exception:
            # Some molecules may cause issues with stereo detection
            pass

        # Get double bond stereochemistry
        # Note: RDKit uses both E/Z and CIS/TRANS stereo labels
        # STEREOE, STEREOZ = absolute (E/Z) stereo
        # STEREOCIS, STEREOTRANS = relative (cis/trans) stereo
        # All of these are considered "defined" stereo
        defined_stereo_types = (
            Chem.BondStereo.STEREOE,
            Chem.BondStereo.STEREOZ,
            Chem.BondStereo.STEREOCIS,
            Chem.BondStereo.STEREOTRANS,
        )
        try:
            for bond in mol.GetBonds():
                if bond.GetBondType() == Chem.BondType.DOUBLE:
                    stereo = bond.GetStereo()
                    if stereo != Chem.BondStereo.STEREONONE:
                        stereo_str = str(stereo).replace("STEREO", "")
                        info.double_bond_stereo.append((bond.GetIdx(), stereo_str))

                        if stereo in defined_stereo_types:
                            info.defined_double_bond_stereo += 1
                        else:
                            info.undefined_double_bond_stereo += 1
        except Exception:
            pass

        return info

    @staticmethod
    def compare(before: StereoInfo, after: StereoInfo) -> StereoComparison:
        """
        Compare stereochemistry before and after standardization.

        Args:
            before: StereoInfo before standardization
            after: StereoInfo after standardization

        Returns:
            StereoComparison with changes and warnings
        """
        comparison = StereoComparison(before=before, after=after)

        # Calculate stereocenter changes
        comparison.stereocenters_lost = max(
            0, before.defined_stereocenters - after.defined_stereocenters
        )
        comparison.stereocenters_gained = max(
            0, after.defined_stereocenters - before.defined_stereocenters
        )

        # Calculate double bond stereo changes
        comparison.double_bond_stereo_lost = max(
            0, before.defined_double_bond_stereo - after.defined_double_bond_stereo
        )
        comparison.double_bond_stereo_gained = max(
            0, after.defined_double_bond_stereo - before.defined_double_bond_stereo
        )

        # Generate warning if any defined stereochemistry was lost
        warnings = []
        if comparison.stereocenters_lost > 0:
            warnings.append(f"{comparison.stereocenters_lost} stereocenter(s) lost")
        if comparison.double_bond_stereo_lost > 0:
            warnings.append(
                f"{comparison.double_bond_stereo_lost} E/Z stereo bond(s) lost"
            )

        if warnings:
            comparison.warning = (
                f"Warning: {', '.join(warnings)} during standardization. "
                "Review the standardized structure carefully."
            )

        return comparison


def track_stereocenters(mol: Chem.Mol) -> StereoInfo:
    """
    Convenience function to get stereo info for a molecule.

    Args:
        mol: RDKit molecule object

    Returns:
        StereoInfo with stereochemistry details
    """
    return StereoTracker.get_stereo_info(mol)
