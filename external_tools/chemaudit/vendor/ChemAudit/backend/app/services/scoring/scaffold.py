"""
Scaffold Extraction

Extracts Murcko scaffolds from molecules for structure analysis.
Provides both standard and generic (framework) scaffolds.
"""

from dataclasses import dataclass

from rdkit import Chem
from rdkit.Chem.Scaffolds import MurckoScaffold


@dataclass
class ScaffoldResult:
    """Result of scaffold extraction."""

    scaffold_smiles: str
    generic_scaffold_smiles: str
    has_scaffold: bool
    message: str
    details: dict


def extract_scaffold(mol: Chem.Mol) -> ScaffoldResult:
    """
    Extract Murcko scaffold from a molecule.

    The Murcko scaffold (Bemis-Murcko framework) consists of ring systems
    and linker atoms connecting them, with side chains removed.

    Note: RDKit's implementation differs slightly from the original
    Bemis-Murcko paper by retaining exocyclic double-bonded atoms (e.g., =O, =N).
    This is intentional as these atoms significantly impact molecular properties.

    The generic scaffold (cyclic skeleton/CSK) converts all atoms to carbons
    and all bonds to single bonds. This implementation correctly handles
    the conversion by running GetScaffoldForMol twice - once to get the
    standard scaffold, and again after MakeScaffoldGeneric to remove
    converted exocyclic substituents.

    Reference: https://github.com/rdkit/rdkit/discussions/6844

    Args:
        mol: RDKit molecule object

    Returns:
        ScaffoldResult with scaffold SMILES and metadata
    """
    if mol is None:
        return ScaffoldResult(
            scaffold_smiles="",
            generic_scaffold_smiles="",
            has_scaffold=False,
            message="Invalid molecule provided",
            details={"error": "null molecule"},
        )

    try:
        # Check if molecule has any rings
        ring_info = mol.GetRingInfo()
        num_rings = ring_info.NumRings()

        details = {
            "num_rings": num_rings,
            "original_smiles": Chem.MolToSmiles(mol),
        }

        if num_rings == 0:
            # No rings - return original molecule with message
            original_smiles = Chem.MolToSmiles(mol)
            return ScaffoldResult(
                scaffold_smiles=original_smiles,
                generic_scaffold_smiles=original_smiles,
                has_scaffold=False,
                message=(
                    "No ring system detected. Acyclic molecules do not have "
                    "a Murcko scaffold. Returning original structure."
                ),
                details=details,
            )

        # Extract Murcko scaffold (ring systems and linkers)
        try:
            scaffold = MurckoScaffold.GetScaffoldForMol(mol)
        except Exception as e:
            return ScaffoldResult(
                scaffold_smiles="",
                generic_scaffold_smiles="",
                has_scaffold=False,
                message=f"Scaffold extraction failed: {str(e)}",
                details={**details, "error": str(e)},
            )

        if scaffold is None:
            return ScaffoldResult(
                scaffold_smiles="",
                generic_scaffold_smiles="",
                has_scaffold=False,
                message="Scaffold extraction returned no result",
                details=details,
            )

        scaffold_smiles = Chem.MolToSmiles(scaffold)
        details["scaffold_atoms"] = scaffold.GetNumAtoms()

        # Create generic scaffold (all C, single bonds)
        # Note: MakeScaffoldGeneric converts double-bonded exocyclic atoms to
        # single-bonded substituents. We need to call GetScaffoldForMol again
        # to remove these and get the true Bemis-Murcko framework.
        # See: https://github.com/rdkit/rdkit/discussions/6844
        try:
            generic_scaffold = MurckoScaffold.MakeScaffoldGeneric(scaffold)
            # IMPORTANT: Call GetScaffoldForMol again to remove the converted
            # exocyclic substituents that MakeScaffoldGeneric created
            generic_scaffold = MurckoScaffold.GetScaffoldForMol(generic_scaffold)
            generic_scaffold_smiles = Chem.MolToSmiles(generic_scaffold)
            details["generic_scaffold_atoms"] = generic_scaffold.GetNumAtoms()
        except Exception as e:
            # If generic scaffold fails, use the standard scaffold
            generic_scaffold_smiles = scaffold_smiles
            details["generic_scaffold_error"] = str(e)

        # Count scaffold rings
        scaffold_ring_info = scaffold.GetRingInfo()
        details["scaffold_rings"] = scaffold_ring_info.NumRings()

        return ScaffoldResult(
            scaffold_smiles=scaffold_smiles,
            generic_scaffold_smiles=generic_scaffold_smiles,
            has_scaffold=True,
            message=(
                f"Murcko scaffold extracted with {scaffold_ring_info.NumRings()} "
                f"ring(s) and {scaffold.GetNumAtoms()} atoms"
            ),
            details=details,
        )

    except Exception as e:
        return ScaffoldResult(
            scaffold_smiles="",
            generic_scaffold_smiles="",
            has_scaffold=False,
            message=f"Unexpected error during scaffold extraction: {str(e)}",
            details={"error": str(e)},
        )
