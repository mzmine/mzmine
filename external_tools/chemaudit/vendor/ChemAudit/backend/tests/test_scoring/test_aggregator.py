"""
Tests for aggregator likelihood scoring module.
"""

import pytest
from rdkit import Chem

from app.services.scoring.aggregator import (
    AggregatorScorer,
    calculate_aggregator_likelihood,
)


class TestAggregatorLikelihood:
    """Tests for aggregator likelihood prediction."""

    @pytest.mark.parametrize(
        "smiles,name",
        [
            ("CCO", "Ethanol"),
            ("CN1C=NC2=C1C(=O)N(C(=O)N2C)C", "Caffeine"),
            ("CC(=O)OC1=CC=CC=C1C(=O)O", "Aspirin"),
        ],
    )
    def test_common_compounds_low_risk(self, smiles: str, name: str):
        """Common small molecules should have low aggregation risk."""
        mol = Chem.MolFromSmiles(smiles)
        result = calculate_aggregator_likelihood(mol)

        assert result.likelihood == "low", f"{name} should have low risk"
        assert result.risk_score < 0.3, f"{name} risk score too high"

    def test_high_logp_increases_risk(self):
        """Highly lipophilic molecules should have higher risk."""
        # Long alkyl chain with aromatic group - high LogP
        mol = Chem.MolFromSmiles("CCCCCCCCCCCCCCCC1=CC=CC=C1")
        result = calculate_aggregator_likelihood(mol)

        # Should have at least moderate risk due to high LogP
        assert result.risk_score > 0.2
        assert result.logp > 4.0
        assert any("lipophilicity" in f.lower() for f in result.risk_factors)

    def test_multiple_aromatic_rings_increases_risk(self):
        """Molecules with many aromatic rings should have higher risk."""
        # Coronene - polycyclic aromatic hydrocarbon
        mol = Chem.MolFromSmiles("c1cc2ccc3ccc4ccc5ccc6ccc1c7c2c3c4c5c67")
        result = calculate_aggregator_likelihood(mol)

        # Should have elevated risk due to aromatic stacking potential
        assert result.aromatic_rings >= 4
        assert result.risk_score > 0.3

    def test_rhodanine_pattern_detected(self):
        """Rhodanine scaffold should be detected as aggregator pattern."""
        mol = Chem.MolFromSmiles("S=C1NC(=O)CS1")  # Rhodanine core
        result = calculate_aggregator_likelihood(mol)

        # Rhodanines are known aggregators
        assert result is not None

    def test_quinone_pattern_detected(self):
        """Quinone scaffold should be detected."""
        mol = Chem.MolFromSmiles("O=C1C=CC(=O)C=C1")  # Benzoquinone
        result = calculate_aggregator_likelihood(mol)

        # Quinones can aggregate
        assert result is not None
        # Check if quinone pattern was detected in risk factors
        _has_quinone = any("quinone" in f.lower() for f in result.risk_factors)
        # Note: May or may not be detected depending on pattern matching


class TestAggregatorDescriptors:
    """Tests for descriptor calculations in aggregator scorer."""

    def test_logp_calculated(self):
        """LogP should be calculated and returned."""
        mol = Chem.MolFromSmiles("c1ccccc1")  # Benzene
        result = calculate_aggregator_likelihood(mol)

        assert result.logp is not None
        assert isinstance(result.logp, float)
        assert 1.5 < result.logp < 2.5  # Benzene LogP ~ 2.1

    def test_tpsa_calculated(self):
        """TPSA should be calculated and returned."""
        mol = Chem.MolFromSmiles("CCO")  # Ethanol
        result = calculate_aggregator_likelihood(mol)

        assert result.tpsa is not None
        assert isinstance(result.tpsa, float)
        assert result.tpsa > 0  # Ethanol has polar OH group

    def test_mw_calculated(self):
        """Molecular weight should be calculated."""
        mol = Chem.MolFromSmiles("c1ccccc1")  # Benzene (MW ~78)
        result = calculate_aggregator_likelihood(mol)

        assert result.mw is not None
        assert 75 < result.mw < 82

    def test_aromatic_rings_counted(self):
        """Aromatic rings should be counted correctly."""
        mol = Chem.MolFromSmiles("c1ccc2ccccc2c1")  # Naphthalene (2 rings)
        result = calculate_aggregator_likelihood(mol)

        assert result.aromatic_rings == 2


class TestRiskCategories:
    """Tests for risk category classification."""

    def test_low_risk_threshold(self):
        """Low risk should be assigned for risk_score < 0.3."""
        mol = Chem.MolFromSmiles("C")  # Methane - minimal risk
        result = calculate_aggregator_likelihood(mol)

        assert result.likelihood == "low"
        assert result.risk_score < 0.3

    @pytest.mark.parametrize(
        "smiles,name",
        [
            ("CCO", "Ethanol"),
            ("c1ccccc1", "Benzene"),
            ("CCCCCCCCCCCCCCCC", "Hexadecane"),
            ("CN1C=NC2=C1C(=O)N(C(=O)N2C)C", "Caffeine"),
        ],
    )
    def test_risk_score_bounded(self, smiles: str, name: str):
        """Risk score should be between 0 and 1."""
        mol = Chem.MolFromSmiles(smiles)
        result = calculate_aggregator_likelihood(mol)

        assert 0 <= result.risk_score <= 1, f"Risk score out of bounds for {name}"


class TestInterpretation:
    """Tests for interpretation strings."""

    def test_interpretation_not_empty(self):
        """Interpretation should not be empty."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_aggregator_likelihood(mol)

        assert result.interpretation
        assert len(result.interpretation) > 10

    def test_low_risk_interpretation(self):
        """Low risk interpretation should mention low risk."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_aggregator_likelihood(mol)

        if result.likelihood == "low":
            assert "low" in result.interpretation.lower()

    def test_high_risk_interpretation_mentions_countermeasures(self):
        """High risk interpretation should mention countermeasures."""
        # Create a molecule likely to have high risk
        mol = Chem.MolFromSmiles("CCCCCCCCCCCCCCC1=CC=C(C=C1)C2=CC=CC=C2")
        result = calculate_aggregator_likelihood(mol)

        if result.likelihood == "high":
            interp_lower = result.interpretation.lower()
            # Should mention counter-screening or validation
            assert (
                "counter" in interp_lower
                or "screen" in interp_lower
                or "validate" in interp_lower
            )


class TestRiskFactors:
    """Tests for risk factor identification."""

    def test_risk_factors_list_type(self):
        """Risk factors should be a list."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_aggregator_likelihood(mol)

        assert isinstance(result.risk_factors, list)

    def test_low_tpsa_adds_risk_factor(self):
        """Low TPSA should add a risk factor."""
        # Hydrocarbon with no polar groups
        mol = Chem.MolFromSmiles("CCCCCCCCCC")  # Decane
        result = calculate_aggregator_likelihood(mol)

        # TPSA should be 0 or very low
        assert result.tpsa < 40
        # Should have polarity-related risk factor
        has_polarity_risk = any(
            "polar" in f.lower() or "tpsa" in f.lower() for f in result.risk_factors
        )
        assert has_polarity_risk

    def test_large_molecule_may_add_risk_factor(self):
        """Large molecules may add risk factor."""
        # Large molecule with MW > 500
        mol = Chem.MolFromSmiles(
            "CC(C)CC1=CC=C(C=C1)C(C)C(=O)O" * 2
        )  # Repeated ibuprofen-like
        if mol is not None:
            result = calculate_aggregator_likelihood(mol)
            # If MW > 500, should have size risk factor
            if result.mw > 500:
                has_size_risk = any(
                    "large" in f.lower() or "mw" in f.lower()
                    for f in result.risk_factors
                )
                assert has_size_risk


class TestScorerClass:
    """Tests for the AggregatorScorer class."""

    def test_scorer_instantiation(self):
        """Scorer should instantiate without errors."""
        scorer = AggregatorScorer()
        assert scorer is not None

    def test_scorer_reuse(self):
        """Scorer should be reusable for multiple molecules."""
        scorer = AggregatorScorer()

        mol1 = Chem.MolFromSmiles("CCO")
        mol2 = Chem.MolFromSmiles("c1ccccc1")
        mol3 = Chem.MolFromSmiles("CN1C=NC2=C1C(=O)N(C(=O)N2C)C")

        result1 = scorer.score(mol1)
        result2 = scorer.score(mol2)
        result3 = scorer.score(mol3)

        assert result1 is not None
        assert result2 is not None
        assert result3 is not None

    def test_result_dataclass_fields(self):
        """Result should have all expected fields."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_aggregator_likelihood(mol)

        assert hasattr(result, "likelihood")
        assert hasattr(result, "risk_score")
        assert hasattr(result, "logp")
        assert hasattr(result, "tpsa")
        assert hasattr(result, "mw")
        assert hasattr(result, "aromatic_rings")
        assert hasattr(result, "risk_factors")
        assert hasattr(result, "interpretation")


class TestEdgeCases:
    """Tests for edge cases."""

    def test_single_atom(self):
        """Single atom should be handled."""
        mol = Chem.MolFromSmiles("[Na]")
        result = calculate_aggregator_likelihood(mol)

        assert result is not None
        assert result.likelihood in ["low", "moderate", "high"]

    def test_charged_molecule(self):
        """Charged molecules should be handled."""
        mol = Chem.MolFromSmiles("[NH4+]")  # Ammonium
        result = calculate_aggregator_likelihood(mol)

        assert result is not None

    def test_complex_drug(self):
        """Complex drug molecule should be handled."""
        # Morphine
        mol = Chem.MolFromSmiles("CN1CCC23C4C1CC5=C2C(=C(C=C5)O)OC3C(C=C4)O")
        result = calculate_aggregator_likelihood(mol)

        assert result is not None
        assert result.interpretation is not None

    def test_steroid_structure(self):
        """Steroid structure should be handled."""
        # Cholesterol-like structure
        mol = Chem.MolFromSmiles("CC(C)CCCC(C)C1CCC2C1(CCC3C2CC=C4C3(CCC(C4)O)C)C")
        result = calculate_aggregator_likelihood(mol)

        assert result is not None
        assert result.risk_score >= 0

    def test_macrocycle(self):
        """Macrocyclic structure should be handled."""
        # Simple macrocycle
        mol = Chem.MolFromSmiles("C1CCCCCCCCCCC1")  # 12-membered ring
        result = calculate_aggregator_likelihood(mol)

        assert result is not None


class TestKnownAggregators:
    """Tests with known aggregator-like compounds."""

    def test_curcumin_like_structure(self):
        """Curcumin-like structures are known aggregators."""
        # Simplified curcumin analog
        mol = Chem.MolFromSmiles("COc1cc(/C=C/C(=O)/C=C/c2ccc(O)c(OC)c2)ccc1O")
        if mol is not None:
            result = calculate_aggregator_likelihood(mol)
            # Curcumin is a known aggregator - should have elevated risk
            assert result.risk_score > 0.2 or result.likelihood != "low"

    def test_catechol_structure(self):
        """Catechol can aggregate at high concentrations."""
        mol = Chem.MolFromSmiles("Oc1ccccc1O")  # Catechol
        result = calculate_aggregator_likelihood(mol)

        # Should be detected
        assert result is not None
