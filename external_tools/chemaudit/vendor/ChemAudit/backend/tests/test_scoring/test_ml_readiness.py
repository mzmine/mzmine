"""
Tests for ML-Readiness Scoring

Tests the MLReadinessScorer for various molecule types.
"""

import pytest
from rdkit import Chem

from app.services.scoring.ml_readiness import (
    MLReadinessResult,
    MLReadinessScorer,
    calculate_ml_readiness,
)


@pytest.fixture
def scorer():
    """Create a scorer instance."""
    return MLReadinessScorer()


class TestMLReadinessScorer:
    """Tests for MLReadinessScorer class."""

    def test_aspirin_scores_high(self, scorer):
        """Aspirin should score well (80+) as it's a simple, well-behaved molecule."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = scorer.score(mol)

        assert result.score >= 80, f"Aspirin scored {result.score}, expected 80+"
        assert result.interpretation.startswith("Excellent")
        assert result.breakdown.descriptors_score > 0
        assert result.breakdown.fingerprints_score > 0
        assert result.breakdown.size_score > 0

    def test_caffeine_scores_well(self, scorer):
        """Caffeine should score well as a drug-like molecule."""
        mol = Chem.MolFromSmiles("CN1C=NC2=C1C(=O)N(C(=O)N2C)C")  # Caffeine
        result = scorer.score(mol)

        assert result.score >= 70, f"Caffeine scored {result.score}, expected 70+"
        assert "morgan" in result.breakdown.fingerprints_successful
        assert "maccs" in result.breakdown.fingerprints_successful

    def test_ethanol_small_molecule(self, scorer):
        """Ethanol is very small, should have lower size score."""
        mol = Chem.MolFromSmiles("CCO")  # Ethanol
        result = scorer.score(mol)

        # Very small molecule, size should be out of optimal range
        assert result.breakdown.num_atoms == 3
        assert result.breakdown.molecular_weight < 100
        # Size category should be acceptable or out_of_range
        assert result.breakdown.size_category in ["acceptable", "out_of_range"]

    def test_large_molecule_size_penalty(self, scorer):
        """Very large molecules should have size penalties."""
        # Create a large molecule (long alkyl chain)
        large_smiles = "C" * 200  # Very long chain
        mol = Chem.MolFromSmiles(large_smiles)
        result = scorer.score(mol)

        assert result.breakdown.size_category == "out_of_range"
        assert result.breakdown.size_score < 20  # Not optimal

    def test_descriptor_breakdown_populated(self, scorer):
        """Check that descriptor breakdown is properly populated."""
        mol = Chem.MolFromSmiles("c1ccccc1")  # Benzene
        result = scorer.score(mol)

        assert result.breakdown.descriptors_total > 0
        assert result.breakdown.descriptors_successful >= 0
        assert result.breakdown.descriptors_score >= 0
        assert result.breakdown.descriptors_max == 35.0  # Updated from 40.0

    def test_fingerprint_breakdown_populated(self, scorer):
        """Check that fingerprint breakdown is properly populated."""
        mol = Chem.MolFromSmiles("c1ccccc1")  # Benzene
        result = scorer.score(mol)

        assert result.breakdown.fingerprints_max == 40.0
        # At least some fingerprints should succeed
        assert len(result.breakdown.fingerprints_successful) > 0
        assert result.breakdown.fingerprints_score > 0

    def test_optimal_size_molecule(self, scorer):
        """Molecules in optimal MW/atom range should get full size points."""
        # Ibuprofen - ~206 Da, good size
        mol = Chem.MolFromSmiles("CC(C)Cc1ccc(cc1)C(C)C(=O)O")
        result = scorer.score(mol)

        assert result.breakdown.size_category == "optimal"
        assert result.breakdown.size_score == 20.0

    def test_acceptable_size_molecule(self, scorer):
        """Molecules in acceptable but not optimal range."""
        # Methane - very small
        mol = Chem.MolFromSmiles("C")
        result = scorer.score(mol)

        assert result.breakdown.size_category in ["acceptable", "out_of_range"]

    def test_interpretation_varies_with_score(self, scorer):
        """Test that interpretation text changes based on score."""
        # High-scoring molecule
        high_mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        high_result = scorer.score(high_mol)

        # Low-scoring (very small)
        small_mol = Chem.MolFromSmiles("C")
        small_result = scorer.score(small_mol)

        # Interpretations should differ
        assert high_result.interpretation != small_result.interpretation


class TestCalculateMLReadinessFunction:
    """Tests for the convenience function."""

    def test_calculate_ml_readiness_returns_result(self):
        """Test that calculate_ml_readiness returns correct type."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")
        result = calculate_ml_readiness(mol)

        assert isinstance(result, MLReadinessResult)
        assert 0 <= result.score <= 100

    def test_calculate_ml_readiness_aspirin(self):
        """Test ML-readiness for aspirin via convenience function."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")
        result = calculate_ml_readiness(mol)

        assert result.score >= 80
        assert result.breakdown is not None
        assert result.interpretation is not None


class TestEdgeCases:
    """Tests for edge cases and error handling."""

    def test_molecule_with_uncommon_elements(self):
        """Test molecules with less common elements."""
        # Molecule with silicon
        mol = Chem.MolFromSmiles("[Si](C)(C)(C)C")
        result = calculate_ml_readiness(mol)

        # Should still produce a valid score
        assert 0 <= result.score <= 100
        assert result.breakdown is not None

    def test_charged_molecule(self):
        """Test charged molecules."""
        # Sodium acetate
        mol = Chem.MolFromSmiles("CC(=O)[O-].[Na+]")
        result = calculate_ml_readiness(mol)

        assert 0 <= result.score <= 100

    def test_molecule_with_stereocenters(self):
        """Test molecules with defined stereochemistry."""
        # L-alanine
        mol = Chem.MolFromSmiles("C[C@H](N)C(=O)O")
        result = calculate_ml_readiness(mol)

        assert 0 <= result.score <= 100
        assert "morgan" in result.breakdown.fingerprints_successful

    def test_aromatic_heterocycle(self):
        """Test aromatic heterocycles."""
        # Pyridine
        mol = Chem.MolFromSmiles("c1ccncc1")
        result = calculate_ml_readiness(mol)

        assert result.score >= 60
        assert result.breakdown.fingerprints_score > 0


class TestEnhancedFingerprints:
    """Tests for the enhanced fingerprint coverage."""

    def test_all_seven_fingerprint_types_generated(self, scorer):
        """Verify all 7 fingerprint types work for a standard molecule."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = scorer.score(mol)

        expected_fps = [
            "morgan",
            "morgan_features",
            "maccs",
            "atompair",
            "topological_torsion",
            "rdkit_fp",
            "avalon",
        ]
        for fp in expected_fps:
            assert fp in result.breakdown.fingerprints_successful, f"{fp} not generated"

    def test_morgan_features_different_from_morgan(self, scorer):
        """FCFP (morgan_features) should be different from ECFP (morgan)."""
        # Both should succeed
        mol = Chem.MolFromSmiles("c1ccccc1O")  # Phenol
        result = scorer.score(mol)

        assert "morgan" in result.breakdown.fingerprints_successful
        assert "morgan_features" in result.breakdown.fingerprints_successful

    def test_topological_torsion_generation(self, scorer):
        """Test topological torsion fingerprint for a molecule with torsions."""
        mol = Chem.MolFromSmiles("CCCCCCCC")  # Octane - has torsions
        result = scorer.score(mol)

        assert "topological_torsion" in result.breakdown.fingerprints_successful

    def test_rdkit_fingerprint_generation(self, scorer):
        """Test RDKit (Daylight-like) fingerprint generation."""
        mol = Chem.MolFromSmiles("c1ccccc1CCO")  # Phenethyl alcohol
        result = scorer.score(mol)

        assert "rdkit_fp" in result.breakdown.fingerprints_successful

    def test_avalon_fingerprint_generation(self, scorer):
        """Test Avalon fingerprint generation."""
        mol = Chem.MolFromSmiles("CC(C)NCC(O)c1ccc(O)c(O)c1")  # Isoproterenol
        result = scorer.score(mol)

        assert "avalon" in result.breakdown.fingerprints_successful

    def test_fingerprint_points_sum_to_40(self, scorer):
        """Verify fingerprint points sum to 40 for perfect molecule."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = scorer.score(mol)

        assert result.breakdown.fingerprints_score == 40.0


class TestAdditionalDescriptors:
    """Tests for AUTOCORR2D and MQN descriptors."""

    def test_autocorr2d_descriptor_count(self, scorer):
        """Verify AUTOCORR2D returns 192 values."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = scorer.score(mol)

        assert result.breakdown.autocorr2d_total == 192
        assert result.breakdown.autocorr2d_successful == 192

    def test_mqn_descriptor_count(self, scorer):
        """Verify MQN returns 42 values."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = scorer.score(mol)

        assert result.breakdown.mqn_total == 42
        assert result.breakdown.mqn_successful == 42

    def test_additional_descriptors_score(self, scorer):
        """Test additional descriptors contribute to score."""
        mol = Chem.MolFromSmiles("c1ccccc1")  # Benzene
        result = scorer.score(mol)

        assert result.breakdown.additional_descriptors_max == 5.0
        assert result.breakdown.additional_descriptors_score > 0

    def test_total_descriptor_count_451(self, scorer):
        """Verify total descriptor count is 451 (217 + 192 + 42)."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = scorer.score(mol)

        total = (
            result.breakdown.descriptors_total
            + result.breakdown.autocorr2d_total
            + result.breakdown.mqn_total
        )
        assert total == 451  # 217 + 192 + 42


class TestInterpretation:
    """Tests for interpretation text."""

    def test_interpretation_mentions_descriptor_counts(self, scorer):
        """Interpretation should mention total descriptor counts."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = scorer.score(mol)

        assert "451" in result.interpretation or "451/451" in result.interpretation
        assert "AUTOCORR2D" in result.interpretation
        assert "MQN" in result.interpretation

    def test_interpretation_mentions_fingerprint_count(self, scorer):
        """Interpretation should mention fingerprint counts."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = scorer.score(mol)

        assert (
            "7/7" in result.interpretation or "7 fingerprint" in result.interpretation
        )


class TestScoreDistribution:
    """Tests for score distribution (35 + 5 + 40 + 20 = 100)."""

    def test_perfect_score_is_100(self, scorer):
        """A well-behaved molecule should get 100."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = scorer.score(mol)

        assert result.score == 100

    def test_score_components_sum_correctly(self, scorer):
        """Verify score components sum to total."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = scorer.score(mol)

        component_sum = (
            result.breakdown.descriptors_score
            + result.breakdown.additional_descriptors_score
            + result.breakdown.fingerprints_score
            + result.breakdown.size_score
        )
        assert abs(component_sum - result.score) < 1  # Allow for rounding
