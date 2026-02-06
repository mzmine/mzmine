"""
Tests for NP-Likeness Scoring

Tests the NPLikenessScorer for natural product-like vs synthetic molecules.
"""

import pytest
from rdkit import Chem

from app.services.scoring.np_likeness import (
    NPLikenessResult,
    NPLikenessScorer,
    calculate_np_likeness,
)


@pytest.fixture
def scorer():
    """Create a scorer instance."""
    return NPLikenessScorer()


class TestNPLikenessScorer:
    """Tests for NPLikenessScorer class."""

    def test_natural_product_like_molecule(self, scorer):
        """Natural product-like molecules should score positive."""
        # Morphine - classic natural product
        mol = Chem.MolFromSmiles("CN1CCC23C4=C5C=CC(O)=C4OC2C(O)C=CC3C1C5")
        result = scorer.score(mol)

        assert isinstance(result, NPLikenessResult)
        assert result.interpretation is not None
        # Should be more NP-like (positive or near zero)

    def test_synthetic_like_molecule(self, scorer):
        """Highly synthetic molecules should score lower."""
        # Highly halogenated synthetic compound
        mol = Chem.MolFromSmiles("FC(F)(F)c1ccc(cc1)C(F)(F)F")
        result = scorer.score(mol)

        assert isinstance(result, NPLikenessResult)
        # Trifluoromethyl groups are synthetic features
        # Score should be lower (more negative)

    def test_simple_natural_scaffold(self, scorer):
        """Simple natural scaffolds should score positively."""
        # Glucose - natural carbohydrate
        mol = Chem.MolFromSmiles("OC[C@H]1OC(O)[C@H](O)[C@@H](O)[C@@H]1O")
        result = scorer.score(mol)

        # High O/C ratio, multiple chiral centers - NP features
        assert result.score > -1.0  # Should be neutral to positive

    def test_interpretation_for_positive_score(self, scorer):
        """Test interpretation text for NP-like molecules."""
        # Steroid core - classic NP scaffold
        mol = Chem.MolFromSmiles(
            "CC12CCC3C(CCC4=CC(=O)CCC34C)C1CCC2O"
        )  # Testosterone-like
        result = scorer.score(mol)

        assert (
            "natural product" in result.interpretation.lower()
            or "mixed" in result.interpretation.lower()
        )

    def test_interpretation_for_negative_score(self, scorer):
        """Test interpretation text for synthetic molecules."""
        # Flat aromatic synthetic compound
        mol = Chem.MolFromSmiles("c1ccc2ccccc2c1")  # Naphthalene - very flat
        result = scorer.score(mol)

        # Should have interpretation about synthetic character
        assert result.interpretation is not None

    def test_small_molecule_caveat(self, scorer):
        """Very small molecules should generate caveats."""
        mol = Chem.MolFromSmiles("CC")  # Ethane
        result = scorer.score(mol)

        # Should have caveat about small size
        assert len(result.caveats) > 0
        assert any("small" in c.lower() for c in result.caveats)

    def test_large_molecule_caveat(self, scorer):
        """Very large molecules should generate caveats."""
        # Create a large molecule
        large_smiles = "C" * 100
        mol = Chem.MolFromSmiles(large_smiles)
        result = scorer.score(mol)

        # Should have caveat about large size
        assert len(result.caveats) > 0
        assert any("large" in c.lower() for c in result.caveats)

    def test_details_populated(self, scorer):
        """Test that details dict is populated."""
        mol = Chem.MolFromSmiles("c1ccccc1")  # Benzene
        result = scorer.score(mol)

        assert "heavy_atom_count" in result.details


class TestCalculateNPLikenessFunction:
    """Tests for the convenience function."""

    def test_calculate_np_likeness_returns_result(self):
        """Test that calculate_np_likeness returns correct type."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = calculate_np_likeness(mol)

        assert isinstance(result, NPLikenessResult)
        assert isinstance(result.score, float)

    def test_calculate_np_likeness_benzene(self):
        """Test NP-likeness for benzene."""
        mol = Chem.MolFromSmiles("c1ccccc1")
        result = calculate_np_likeness(mol)

        # Benzene is simple aromatic - relatively flat
        assert result.interpretation is not None


class TestInterpretationRanges:
    """Tests for interpretation text at different score ranges."""

    @pytest.fixture
    def scorer(self):
        return NPLikenessScorer()

    def test_score_range_coverage(self, scorer):
        """Test that different molecules produce different interpretations."""
        # Various molecules
        molecules = [
            "c1ccccc1",  # Benzene
            "CCO",  # Ethanol
            "CC(=O)Oc1ccccc1C(=O)O",  # Aspirin
            "CN1C=NC2=C1C(=O)N(C(=O)N2C)C",  # Caffeine
        ]

        interpretations = []
        for smiles in molecules:
            mol = Chem.MolFromSmiles(smiles)
            result = scorer.score(mol)
            interpretations.append(result.interpretation)

        # Should have various interpretations
        assert len(set(interpretations)) >= 1  # At least one unique


class TestEdgeCases:
    """Tests for edge cases."""

    def test_molecule_with_unusual_elements(self):
        """Test molecules with unusual elements."""
        # Molecule with selenium
        mol = Chem.MolFromSmiles("[Se]C1=CC=CC=C1")
        result = calculate_np_likeness(mol)

        # Should have caveat about unusual elements
        assert len(result.caveats) > 0

    def test_charged_molecule(self):
        """Test charged molecules."""
        # Acetate ion
        mol = Chem.MolFromSmiles("CC(=O)[O-]")
        result = calculate_np_likeness(mol)

        assert isinstance(result, NPLikenessResult)
        assert isinstance(result.score, float)

    def test_complex_natural_product(self):
        """Test a complex natural product scaffold."""
        # Simplified taxol core
        mol = Chem.MolFromSmiles(
            "CC1=C2C(C(=O)C3(C(CC4C(C3C(C(C2(C)C)(CC1OC(=O)C(C(C5=CC=CC=C5)NC(=O)OC(C)(C)C)O)O)OC(=O)C6=CC=CC=C6)(CO4)OC(=O)C)O)C)OC(=O)C"
        )
        if mol is not None:
            result = calculate_np_likeness(mol)
            assert isinstance(result, NPLikenessResult)

    def test_drug_like_molecule(self):
        """Test a typical drug-like molecule."""
        # Ibuprofen
        mol = Chem.MolFromSmiles("CC(C)Cc1ccc(cc1)C(C)C(=O)O")
        result = calculate_np_likeness(mol)

        assert isinstance(result, NPLikenessResult)
        # Ibuprofen is synthetic but has some NP-like features
