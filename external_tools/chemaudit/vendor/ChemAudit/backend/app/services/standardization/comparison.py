"""
Structure comparison utilities for standardization.

Compares original and standardized structures to summarize changes.
"""

from dataclasses import dataclass, field
from typing import List, Optional

from rdkit import Chem
from rdkit.Chem import Descriptors, rdMolDescriptors
from rdkit.Chem import inchi as rdkit_inchi


@dataclass
class StructureComparison:
    """Comparison between original and standardized structures."""

    # Atom and bond counts
    original_atom_count: int = 0
    standardized_atom_count: int = 0
    original_bond_count: int = 0
    standardized_bond_count: int = 0

    # Molecular formulas
    original_formula: Optional[str] = None
    standardized_formula: Optional[str] = None

    # InChIKeys for identity comparison
    original_inchikey: Optional[str] = None
    standardized_inchikey: Optional[str] = None

    # Molecular weights
    original_mw: Optional[float] = None
    standardized_mw: Optional[float] = None

    # Change summary
    atoms_removed: int = 0
    atoms_added: int = 0
    bonds_removed: int = 0
    bonds_added: int = 0

    # Flags
    is_identical: bool = False
    significant_change: bool = False
    diff_summary: List[str] = field(default_factory=list)

    @property
    def mass_change_percent(self) -> float:
        """Calculate percentage change in molecular weight."""
        if self.original_mw is None or self.original_mw == 0:
            return 0.0
        if self.standardized_mw is None:
            return 0.0
        return ((self.standardized_mw - self.original_mw) / self.original_mw) * 100


def compare_structures(
    original: Chem.Mol, standardized: Chem.Mol
) -> StructureComparison:
    """
    Compare original and standardized molecules.

    Args:
        original: Original RDKit molecule
        standardized: Standardized RDKit molecule

    Returns:
        StructureComparison with detailed change information
    """
    comparison = StructureComparison()

    if original is None or standardized is None:
        comparison.diff_summary.append("Cannot compare: one or both molecules are None")
        return comparison

    # Get atom and bond counts
    comparison.original_atom_count = original.GetNumAtoms()
    comparison.standardized_atom_count = standardized.GetNumAtoms()
    comparison.original_bond_count = original.GetNumBonds()
    comparison.standardized_bond_count = standardized.GetNumBonds()

    # Calculate changes
    comparison.atoms_removed = max(
        0, comparison.original_atom_count - comparison.standardized_atom_count
    )
    comparison.atoms_added = max(
        0, comparison.standardized_atom_count - comparison.original_atom_count
    )
    comparison.bonds_removed = max(
        0, comparison.original_bond_count - comparison.standardized_bond_count
    )
    comparison.bonds_added = max(
        0, comparison.standardized_bond_count - comparison.original_bond_count
    )

    # Get molecular formulas
    try:
        comparison.original_formula = rdMolDescriptors.CalcMolFormula(original)
    except Exception:
        comparison.original_formula = None

    try:
        comparison.standardized_formula = rdMolDescriptors.CalcMolFormula(standardized)
    except Exception:
        comparison.standardized_formula = None

    # Get molecular weights
    try:
        comparison.original_mw = Descriptors.MolWt(original)
    except Exception:
        comparison.original_mw = None

    try:
        comparison.standardized_mw = Descriptors.MolWt(standardized)
    except Exception:
        comparison.standardized_mw = None

    # Get InChIKeys for identity comparison
    try:
        inchi = rdkit_inchi.MolToInchi(original)
        if inchi:
            comparison.original_inchikey = rdkit_inchi.MolToInchiKey(original)
    except Exception:
        comparison.original_inchikey = None

    try:
        inchi = rdkit_inchi.MolToInchi(standardized)
        if inchi:
            comparison.standardized_inchikey = rdkit_inchi.MolToInchiKey(standardized)
    except Exception:
        comparison.standardized_inchikey = None

    # Check if structures are identical
    comparison.is_identical = (
        comparison.original_inchikey is not None
        and comparison.standardized_inchikey is not None
        and comparison.original_inchikey == comparison.standardized_inchikey
    )

    # Determine if change is significant (different InChIKey = different compound)
    comparison.significant_change = (
        comparison.original_inchikey is not None
        and comparison.standardized_inchikey is not None
        and comparison.original_inchikey != comparison.standardized_inchikey
    )

    # Generate diff summary
    if comparison.is_identical:
        comparison.diff_summary.append("No structural changes")
    else:
        if comparison.atoms_removed > 0:
            comparison.diff_summary.append(
                f"{comparison.atoms_removed} atom(s) removed"
            )
        if comparison.atoms_added > 0:
            comparison.diff_summary.append(f"{comparison.atoms_added} atom(s) added")
        if comparison.bonds_removed > 0:
            comparison.diff_summary.append(
                f"{comparison.bonds_removed} bond(s) removed"
            )
        if comparison.bonds_added > 0:
            comparison.diff_summary.append(f"{comparison.bonds_added} bond(s) added")

        if comparison.original_formula != comparison.standardized_formula:
            comparison.diff_summary.append(
                f"Formula changed: {comparison.original_formula} -> {comparison.standardized_formula}"
            )

        mass_change = abs(comparison.mass_change_percent)
        if mass_change > 1.0:
            comparison.diff_summary.append(
                f"Mass changed by {comparison.mass_change_percent:.1f}%"
            )

    return comparison
