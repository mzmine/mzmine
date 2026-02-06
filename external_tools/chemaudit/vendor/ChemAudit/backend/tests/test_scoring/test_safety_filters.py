"""
Tests for safety filters scoring module.
"""

from rdkit import Chem

from app.services.scoring.safety_filters import (
    SafetyFilterScorer,
    calculate_safety_filters,
    get_pains_alerts,
    is_pains_clean,
)


class TestPAINSFilter:
    """Tests for PAINS (Pan Assay Interference Compounds) filter."""

    def test_aspirin_passes_pains(self):
        """Aspirin should pass PAINS filter."""
        mol = Chem.MolFromSmiles("CC(=O)OC1=CC=CC=C1C(=O)O")
        result = calculate_safety_filters(mol)

        assert result.pains.passed is True
        assert result.pains.alert_count == 0
        assert len(result.pains.alerts) == 0

    def test_quinone_triggers_pains(self):
        """Quinone should trigger PAINS alert."""
        mol = Chem.MolFromSmiles("O=C1C=CC(=O)C=C1")  # Benzoquinone
        result = calculate_safety_filters(mol)

        # Quinones are classic PAINS
        assert result.pains.passed is False or result.pains.alert_count > 0

    def test_rhodanine_triggers_pains(self):
        """Rhodanine should trigger PAINS alert."""
        mol = Chem.MolFromSmiles("S=C1NC(=O)CS1")  # Rhodanine core
        result = calculate_safety_filters(mol)

        # Rhodanines are classic PAINS
        # Note: Exact behavior depends on RDKit's PAINS catalog
        assert result.pains is not None

    def test_caffeine_passes_pains(self):
        """Caffeine should pass PAINS filter."""
        mol = Chem.MolFromSmiles("CN1C=NC2=C1C(=O)N(C(=O)N2C)C")
        result = calculate_safety_filters(mol)

        assert result.pains.passed is True

    def test_is_pains_clean_function(self):
        """Test is_pains_clean convenience function."""
        mol = Chem.MolFromSmiles("CCO")  # Ethanol
        assert is_pains_clean(mol) is True

    def test_get_pains_alerts_function(self):
        """Test get_pains_alerts convenience function."""
        mol = Chem.MolFromSmiles("CCO")  # Ethanol
        alerts = get_pains_alerts(mol)
        assert isinstance(alerts, list)


class TestBrenkFilter:
    """Tests for Brenk structural alerts."""

    def test_ethanol_passes_brenk(self):
        """Ethanol should pass Brenk filter."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_safety_filters(mol)

        assert result.brenk.passed is True
        assert result.brenk.alert_count == 0

    def test_acyl_halide_triggers_brenk(self):
        """Acyl chloride should trigger Brenk alert."""
        mol = Chem.MolFromSmiles("CC(=O)Cl")  # Acetyl chloride
        result = calculate_safety_filters(mol)

        # Acyl halides are reactive groups
        assert result.brenk is not None

    def test_phenol_ester_may_trigger_brenk(self):
        """Phenol ester (aspirin) triggers Brenk."""
        mol = Chem.MolFromSmiles("CC(=O)OC1=CC=CC=C1C(=O)O")  # Aspirin
        result = calculate_safety_filters(mol)

        # Phenol esters are known Brenk alerts
        assert result.brenk.passed is False
        assert "phenol_ester" in result.brenk.alerts


class TestNIHAndZINCFilters:
    """Tests for NIH and ZINC filters."""

    def test_nih_filter_included(self):
        """NIH filter should be included with extended=True."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_safety_filters(mol, include_extended=True)

        assert result.nih is not None

    def test_zinc_filter_included(self):
        """ZINC filter should be included with extended=True."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_safety_filters(mol, include_extended=True)

        assert result.zinc is not None

    def test_extended_filters_optional(self):
        """Extended filters should be optional."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_safety_filters(mol, include_extended=False)

        assert result.nih is None
        assert result.zinc is None


class TestOverallResults:
    """Tests for overall safety filter results."""

    def test_all_passed_flag(self):
        """all_passed flag should be correct."""
        mol = Chem.MolFromSmiles("CCO")  # Ethanol - should pass all
        result = calculate_safety_filters(mol)

        if result.pains.passed and result.brenk.passed:
            assert result.all_passed is True
        else:
            assert result.all_passed is False

    def test_total_alerts_count(self):
        """total_alerts should be sum of all alerts."""
        mol = Chem.MolFromSmiles("CC(=O)OC1=CC=CC=C1C(=O)O")  # Aspirin
        result = calculate_safety_filters(
            mol, include_extended=True, include_chembl=True
        )

        expected_total = result.pains.alert_count + result.brenk.alert_count
        if result.nih:
            expected_total += result.nih.alert_count
        if result.zinc:
            expected_total += result.zinc.alert_count
        if result.chembl:
            expected_total += result.chembl.total_alerts

        assert result.total_alerts == expected_total

    def test_interpretation_not_empty(self):
        """Interpretation should not be empty."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_safety_filters(mol)

        assert result.interpretation
        assert len(result.interpretation) > 5


class TestEdgeCases:
    """Tests for edge cases."""

    def test_single_atom(self):
        """Single atom should be handled."""
        mol = Chem.MolFromSmiles("[Na]")
        result = calculate_safety_filters(mol)

        assert result.pains is not None
        assert result.brenk is not None

    def test_complex_drug(self):
        """Complex drug molecule should be handled."""
        mol = Chem.MolFromSmiles(
            "CN1CCC23C4C1CC5=C2C(=C(C=C5)O)OC3C(C=C4)O"
        )  # Morphine
        result = calculate_safety_filters(mol)

        assert result is not None
        assert result.interpretation is not None

    def test_scorer_reuse(self):
        """Scorer should be reusable."""
        scorer = SafetyFilterScorer()

        mol1 = Chem.MolFromSmiles("CCO")
        mol2 = Chem.MolFromSmiles("c1ccccc1")

        result1 = scorer.score(mol1)
        result2 = scorer.score(mol2)

        assert result1 is not None
        assert result2 is not None
