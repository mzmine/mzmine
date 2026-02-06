"""
ChEMBL-compatible Standardization Pipeline.

Three-stage workflow:
1. Checker: Detect structure issues before standardization
2. Standardizer: Fix nitro groups, explicit H, metals, sulphoxides, allenes
3. GetParent: Extract parent molecule, remove salts/solvents

CRITICAL: Tautomer canonicalization is OFF by default because it can
remove double-bond stereochemistry (E/Z isomerism). Users must explicitly
opt-in when they understand the implications.
"""

from dataclasses import dataclass, field
from typing import List, Optional, Tuple

from chembl_structure_pipeline import checker, get_parent_mol, standardizer
from rdkit import Chem
from rdkit.Chem.MolStandardize import rdMolStandardize

from app.services.standardization.comparison import (
    StructureComparison,
    compare_structures,
)
from app.services.standardization.stereo_tracker import StereoComparison, StereoTracker


@dataclass
class StandardizationOptions:
    """Options for standardization pipeline."""

    # Include tautomer canonicalization (OFF by default - can lose stereochemistry)
    include_tautomer: bool = False

    # Preserve stereochemistry during standardization
    preserve_stereo: bool = True

    # Return excluded fragments (salts, solvents)
    return_excluded_fragments: bool = True


@dataclass
class StandardizationStep:
    """A single step in the standardization pipeline."""

    step_name: str
    applied: bool
    description: str
    changes: str = ""


@dataclass
class StandardizationResult:
    """Result of the standardization pipeline."""

    # Input/output SMILES
    original_smiles: str
    standardized_smiles: Optional[str] = None

    # Success flag
    success: bool = False
    error_message: Optional[str] = None

    # Pipeline steps
    steps_applied: List[StandardizationStep] = field(default_factory=list)

    # Checker issues found before standardization
    checker_issues: List[Tuple[int, str]] = field(default_factory=list)

    # Excluded fragments (salts, solvents removed)
    excluded_fragments: List[str] = field(default_factory=list)

    # Stereochemistry comparison
    stereo_comparison: Optional[StereoComparison] = None

    # Structure comparison
    structure_comparison: Optional[StructureComparison] = None

    @property
    def mass_change_percent(self) -> float:
        """Get mass change percentage from structure comparison."""
        if self.structure_comparison:
            return self.structure_comparison.mass_change_percent
        return 0.0


class StandardizationPipeline:
    """
    ChEMBL-compatible standardization pipeline.

    Usage:
        pipeline = StandardizationPipeline()
        result = pipeline.standardize(mol, options)
    """

    def __init__(self):
        """Initialize the standardization pipeline."""
        # Tautomer enumerator for optional tautomer canonicalization
        self._tautomer_enumerator = rdMolStandardize.TautomerEnumerator()

    def standardize(
        self, mol: Chem.Mol, options: Optional[StandardizationOptions] = None
    ) -> StandardizationResult:
        """
        Standardize a molecule using ChEMBL pipeline.

        Three-stage workflow:
        1. Checker: Detect structure issues
        2. Standardizer: Fix common issues
        3. GetParent: Extract parent molecule

        Args:
            mol: RDKit molecule object
            options: Standardization options (defaults used if None)

        Returns:
            StandardizationResult with standardized molecule and details
        """
        if options is None:
            options = StandardizationOptions()

        # Initialize result
        try:
            original_smiles = Chem.MolToSmiles(mol)
        except Exception:
            original_smiles = ""

        result = StandardizationResult(original_smiles=original_smiles)

        if mol is None:
            result.error_message = "Input molecule is None"
            return result

        # Track stereochemistry before standardization
        stereo_before = StereoTracker.get_stereo_info(mol)

        try:
            # Stage 1: Checker
            checker_step = self._run_checker(mol, result)
            result.steps_applied.append(checker_step)

            # Stage 2: Standardizer
            std_mol, std_step = self._run_standardizer(mol)
            result.steps_applied.append(std_step)

            if std_mol is None:
                result.error_message = "Standardization failed"
                return result

            # Stage 3: GetParent (salt stripping)
            parent_mol, parent_step, excluded = self._run_get_parent(
                std_mol, options.return_excluded_fragments
            )
            result.steps_applied.append(parent_step)
            result.excluded_fragments = excluded

            if parent_mol is None:
                # Use standardized mol if parent extraction failed
                parent_mol = std_mol

            # Stage 4 (Optional): Tautomer canonicalization
            if options.include_tautomer:
                final_mol, taut_step = self._run_tautomer_canonicalization(parent_mol)
                result.steps_applied.append(taut_step)
            else:
                final_mol = parent_mol
                result.steps_applied.append(
                    StandardizationStep(
                        step_name="tautomer_canonicalization",
                        applied=False,
                        description="Tautomer canonicalization (skipped by default to preserve E/Z stereo)",
                        changes="Not applied - enable with include_tautomer=True",
                    )
                )

            # Generate final SMILES
            result.standardized_smiles = Chem.MolToSmiles(final_mol)
            result.success = True

            # Track stereochemistry after standardization
            stereo_after = StereoTracker.get_stereo_info(final_mol)
            result.stereo_comparison = StereoTracker.compare(
                stereo_before, stereo_after
            )

            # Compare structures
            result.structure_comparison = compare_structures(mol, final_mol)

        except Exception as e:
            result.error_message = f"Standardization error: {str(e)}"
            result.success = False

        return result

    def _run_checker(
        self, mol: Chem.Mol, result: StandardizationResult
    ) -> StandardizationStep:
        """
        Run ChEMBL checker to detect structure issues.

        Args:
            mol: RDKit molecule
            result: StandardizationResult to populate checker_issues

        Returns:
            StandardizationStep for checker stage
        """
        try:
            molblock = Chem.MolToMolBlock(mol)
            issues = checker.check_molblock(molblock)

            if issues:
                result.checker_issues = [(score, msg) for score, msg in issues]
                return StandardizationStep(
                    step_name="checker",
                    applied=True,
                    description="Structure checker (detect issues)",
                    changes=f"Found {len(issues)} issue(s)",
                )
            else:
                return StandardizationStep(
                    step_name="checker",
                    applied=True,
                    description="Structure checker (detect issues)",
                    changes="No issues found",
                )
        except Exception as e:
            return StandardizationStep(
                step_name="checker",
                applied=False,
                description="Structure checker (detect issues)",
                changes=f"Error: {str(e)}",
            )

    def _run_standardizer(
        self, mol: Chem.Mol
    ) -> Tuple[Optional[Chem.Mol], StandardizationStep]:
        """
        Run ChEMBL standardizer.

        Fixes:
        - Nitro groups
        - Explicit hydrogens
        - Metal disconnection
        - Sulphoxides
        - Allenes

        Args:
            mol: RDKit molecule

        Returns:
            Tuple of (standardized molecule, StandardizationStep)
        """
        try:
            # Get SMILES before standardization
            before_smiles = Chem.MolToSmiles(mol)

            # Run standardizer
            std_mol = standardizer.standardize_mol(mol)

            if std_mol is None:
                return None, StandardizationStep(
                    step_name="standardizer",
                    applied=False,
                    description="ChEMBL standardizer (fix common issues)",
                    changes="Failed to standardize",
                )

            # Get SMILES after standardization
            after_smiles = Chem.MolToSmiles(std_mol)

            if before_smiles != after_smiles:
                return std_mol, StandardizationStep(
                    step_name="standardizer",
                    applied=True,
                    description="ChEMBL standardizer (fix common issues)",
                    changes="Structure modified",
                )
            else:
                return std_mol, StandardizationStep(
                    step_name="standardizer",
                    applied=True,
                    description="ChEMBL standardizer (fix common issues)",
                    changes="No changes needed",
                )

        except Exception as e:
            return None, StandardizationStep(
                step_name="standardizer",
                applied=False,
                description="ChEMBL standardizer (fix common issues)",
                changes=f"Error: {str(e)}",
            )

    def _run_get_parent(
        self, mol: Chem.Mol, return_excluded: bool = True
    ) -> Tuple[Optional[Chem.Mol], StandardizationStep, List[str]]:
        """
        Run ChEMBL GetParent to extract parent molecule.

        Removes salts, solvents, and other non-parent components.

        Args:
            mol: RDKit molecule
            return_excluded: Whether to return excluded fragments

        Returns:
            Tuple of (parent molecule, StandardizationStep, excluded fragments)
        """
        excluded_fragments = []

        try:
            # Get SMILES before
            before_smiles = Chem.MolToSmiles(mol)

            # Run get_parent_mol
            parent_mol, exclude = get_parent_mol(mol)

            if parent_mol is None:
                return (
                    None,
                    StandardizationStep(
                        step_name="get_parent",
                        applied=False,
                        description="Extract parent molecule (remove salts/solvents)",
                        changes="Failed to extract parent",
                    ),
                    excluded_fragments,
                )

            # Get SMILES after
            after_smiles = Chem.MolToSmiles(parent_mol)

            # Process excluded fragments
            if return_excluded and exclude:
                # exclude is a flag or can be processed differently
                # In chembl_structure_pipeline, it returns information about what was removed
                # We need to reconstruct excluded fragments by comparing
                if before_smiles != after_smiles:
                    # Find removed fragments by parsing the original as multiple components
                    original_parts = before_smiles.split(".")
                    parent_parts = after_smiles.split(".")
                    for part in original_parts:
                        if part not in parent_parts:
                            excluded_fragments.append(part)

            if excluded_fragments:
                return (
                    parent_mol,
                    StandardizationStep(
                        step_name="get_parent",
                        applied=True,
                        description="Extract parent molecule (remove salts/solvents)",
                        changes=f"Removed {len(excluded_fragments)} fragment(s)",
                    ),
                    excluded_fragments,
                )
            elif before_smiles != after_smiles:
                return (
                    parent_mol,
                    StandardizationStep(
                        step_name="get_parent",
                        applied=True,
                        description="Extract parent molecule (remove salts/solvents)",
                        changes="Structure modified",
                    ),
                    excluded_fragments,
                )
            else:
                return (
                    parent_mol,
                    StandardizationStep(
                        step_name="get_parent",
                        applied=True,
                        description="Extract parent molecule (remove salts/solvents)",
                        changes="No fragments to remove",
                    ),
                    excluded_fragments,
                )

        except Exception as e:
            return (
                None,
                StandardizationStep(
                    step_name="get_parent",
                    applied=False,
                    description="Extract parent molecule (remove salts/solvents)",
                    changes=f"Error: {str(e)}",
                ),
                excluded_fragments,
            )

    def _run_tautomer_canonicalization(
        self, mol: Chem.Mol
    ) -> Tuple[Optional[Chem.Mol], StandardizationStep]:
        """
        Run tautomer canonicalization.

        WARNING: This can remove double-bond stereochemistry (E/Z isomerism).
        Only run when user explicitly requests it.

        Args:
            mol: RDKit molecule

        Returns:
            Tuple of (canonicalized molecule, StandardizationStep)
        """
        try:
            before_smiles = Chem.MolToSmiles(mol)

            # Get canonical tautomer
            canon_mol = self._tautomer_enumerator.Canonicalize(mol)

            if canon_mol is None:
                return mol, StandardizationStep(
                    step_name="tautomer_canonicalization",
                    applied=False,
                    description="Tautomer canonicalization (WARNING: may lose E/Z stereo)",
                    changes="Failed to canonicalize",
                )

            after_smiles = Chem.MolToSmiles(canon_mol)

            if before_smiles != after_smiles:
                return canon_mol, StandardizationStep(
                    step_name="tautomer_canonicalization",
                    applied=True,
                    description="Tautomer canonicalization (WARNING: may lose E/Z stereo)",
                    changes="Tautomer changed",
                )
            else:
                return canon_mol, StandardizationStep(
                    step_name="tautomer_canonicalization",
                    applied=True,
                    description="Tautomer canonicalization (WARNING: may lose E/Z stereo)",
                    changes="No tautomer change needed",
                )

        except Exception as e:
            return mol, StandardizationStep(
                step_name="tautomer_canonicalization",
                applied=False,
                description="Tautomer canonicalization (WARNING: may lose E/Z stereo)",
                changes=f"Error: {str(e)}",
            )


# Singleton instance
_pipeline = StandardizationPipeline()


def standardize_molecule(
    mol: Chem.Mol, options: Optional[StandardizationOptions] = None
) -> StandardizationResult:
    """
    Convenience function to standardize a molecule.

    Args:
        mol: RDKit molecule object
        options: Standardization options

    Returns:
        StandardizationResult with standardized molecule and details
    """
    return _pipeline.standardize(mol, options)
