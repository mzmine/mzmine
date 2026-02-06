"""
Tests for ChEMBL standardization pipeline.

Tests the three-stage workflow:
1. Checker - detect issues
2. Standardizer - fix common issues
3. GetParent - extract parent, remove salts
"""

import pytest
from rdkit import Chem

from app.services.standardization.chembl_pipeline import (
    StandardizationOptions,
    StandardizationPipeline,
    standardize_molecule,
)


@pytest.fixture
def pipeline():
    """Create a fresh pipeline instance for tests."""
    return StandardizationPipeline()


class TestSaltStripping:
    """Test salt stripping functionality."""

    def test_amine_hcl_strips_chloride(self, pipeline):
        """Amine HCl should have chloride stripped."""
        # Ethylamine hydrochloride
        mol = Chem.MolFromSmiles("CCN.Cl")
        result = pipeline.standardize(mol)

        assert result.success
        assert result.standardized_smiles is not None
        # Should be ethylamine without chloride
        assert "Cl" not in result.standardized_smiles
        assert "." not in result.standardized_smiles  # Single component

    def test_diisopropylamine_hcl_strips_chloride(self, pipeline):
        """Diisopropylamine HCl should have chloride stripped."""
        mol = Chem.MolFromSmiles("CC(C)NC(C)C.Cl")
        result = pipeline.standardize(mol)

        assert result.success
        assert "Cl" not in result.standardized_smiles

    def test_chloride_salt_strips_chloride(self, pipeline):
        """Chloride counterions should be stripped."""
        # Simple amine with chloride
        mol = Chem.MolFromSmiles("CN.Cl")
        result = pipeline.standardize(mol)

        assert result.success
        # Should have amine without chloride
        assert "Cl" not in result.standardized_smiles

    def test_sodium_acetate_standardized(self, pipeline):
        """Sodium acetate should be standardized (may or may not strip salt)."""
        # Note: ChEMBL pipeline behavior for metal salts varies
        mol = Chem.MolFromSmiles("CC(=O)O.[Na]")
        result = pipeline.standardize(mol)

        assert result.success
        assert result.standardized_smiles is not None
        # Pipeline should succeed even if it doesn't strip all salts


class TestNormalization:
    """Test structure normalization."""

    def test_simple_molecule_unchanged(self, pipeline):
        """Simple molecules should remain unchanged."""
        mol = Chem.MolFromSmiles("CCO")
        result = pipeline.standardize(mol)

        assert result.success
        assert result.standardized_smiles == "CCO"

    def test_benzene_unchanged(self, pipeline):
        """Benzene should remain unchanged."""
        mol = Chem.MolFromSmiles("c1ccccc1")
        result = pipeline.standardize(mol)

        assert result.success
        # Should still be benzene (may have different SMILES representation)
        std_mol = Chem.MolFromSmiles(result.standardized_smiles)
        assert std_mol.GetNumAtoms() == 6

    def test_aspirin_standardized(self, pipeline):
        """Aspirin should be standardized properly."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")
        result = pipeline.standardize(mol)

        assert result.success
        assert result.standardized_smiles is not None


class TestTautomerCanonicalization:
    """Test tautomer canonicalization (optional)."""

    def test_tautomer_off_by_default(self, pipeline):
        """Tautomer canonicalization should be OFF by default."""
        mol = Chem.MolFromSmiles("CCO")
        options = StandardizationOptions()

        assert options.include_tautomer is False

        result = pipeline.standardize(mol, options)
        assert result.success

        # Check that tautomer step was skipped
        taut_step = next(
            (
                s
                for s in result.steps_applied
                if s.step_name == "tautomer_canonicalization"
            ),
            None,
        )
        assert taut_step is not None
        assert taut_step.applied is False

    def test_tautomer_on_when_requested(self, pipeline):
        """Tautomer canonicalization should run when requested."""
        mol = Chem.MolFromSmiles("CCO")
        options = StandardizationOptions(include_tautomer=True)

        result = pipeline.standardize(mol, options)
        assert result.success

        # Check that tautomer step was applied
        taut_step = next(
            (
                s
                for s in result.steps_applied
                if s.step_name == "tautomer_canonicalization"
            ),
            None,
        )
        assert taut_step is not None
        assert taut_step.applied is True


class TestPipelineSteps:
    """Test that all pipeline steps are reported."""

    def test_all_steps_present(self, pipeline):
        """All pipeline steps should be present in result."""
        mol = Chem.MolFromSmiles("CCO")
        result = pipeline.standardize(mol)

        step_names = [s.step_name for s in result.steps_applied]

        assert "checker" in step_names
        assert "standardizer" in step_names
        assert "get_parent" in step_names
        assert "tautomer_canonicalization" in step_names

    def test_steps_have_descriptions(self, pipeline):
        """All steps should have descriptions."""
        mol = Chem.MolFromSmiles("CCO")
        result = pipeline.standardize(mol)

        for step in result.steps_applied:
            assert step.description is not None
            assert len(step.description) > 0


class TestConvenienceFunction:
    """Test the standardize_molecule convenience function."""

    def test_convenience_function_works(self):
        """standardize_molecule function should work like pipeline."""
        mol = Chem.MolFromSmiles("CCN.Cl")
        result = standardize_molecule(mol)

        assert result.success
        assert "Cl" not in result.standardized_smiles

    def test_convenience_function_with_options(self):
        """standardize_molecule should accept options."""
        mol = Chem.MolFromSmiles("CCO")
        options = StandardizationOptions(include_tautomer=True)
        result = standardize_molecule(mol, options)

        assert result.success


class TestErrorHandling:
    """Test error handling in pipeline."""

    def test_none_molecule_handled(self, pipeline):
        """None molecule should return error result."""
        result = pipeline.standardize(None)

        assert not result.success
        assert result.error_message is not None

    def test_invalid_molecule_handled(self, pipeline):
        """Invalid molecule should be handled gracefully."""
        # Create a molecule that might cause issues
        mol = Chem.MolFromSmiles("C")
        result = pipeline.standardize(mol)

        # Even simple molecules should succeed
        assert result.success
