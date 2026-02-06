"""
Tests for ADMET predictions module.
"""

from rdkit import Chem

from app.services.scoring.admet import (
    ADMETScorer,
    calculate_admet,
)


class TestSyntheticAccessibility:
    """Tests for synthetic accessibility score."""

    def test_simple_molecule_low_sa(self):
        """Simple molecule should have low SA score."""
        mol = Chem.MolFromSmiles("CCO")  # Ethanol
        result = calculate_admet(mol)

        assert result.synthetic_accessibility.score < 4
        assert result.synthetic_accessibility.classification == "easy"

    def test_aspirin_easy_to_synthesize(self):
        """Aspirin should be easy to synthesize."""
        mol = Chem.MolFromSmiles("CC(=O)OC1=CC=CC=C1C(=O)O")
        result = calculate_admet(mol)

        assert result.synthetic_accessibility.score < 3
        assert result.synthetic_accessibility.classification == "easy"

    def test_complex_np_moderate_sa(self):
        """Complex natural product should have moderate/high SA."""
        mol = Chem.MolFromSmiles(
            "CN1CCC23C4C1CC5=C2C(=C(C=C5)O)OC3C(C=C4)O"
        )  # Morphine
        result = calculate_admet(mol)

        # Morphine has bridged rings and multiple stereocenters
        assert result.synthetic_accessibility.score > 3

    def test_sa_score_range(self):
        """SA score should be in valid range."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_admet(mol)

        assert 1 <= result.synthetic_accessibility.score <= 10

    def test_sa_interpretation(self):
        """SA interpretation should be meaningful."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_admet(mol)

        assert "synthesize" in result.synthetic_accessibility.interpretation.lower()


class TestSolubility:
    """Tests for ESOL solubility prediction."""

    def test_ethanol_highly_soluble(self):
        """Ethanol should be highly soluble."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_admet(mol)

        assert result.solubility.log_s > -1
        assert result.solubility.classification in ["highly_soluble", "soluble"]

    def test_caffeine_soluble(self):
        """Caffeine should be soluble."""
        mol = Chem.MolFromSmiles("CN1C=NC2=C1C(=O)N(C(=O)N2C)C")
        result = calculate_admet(mol)

        assert result.solubility.log_s > -2
        assert "soluble" in result.solubility.classification

    def test_lipophilic_poorly_soluble(self):
        """Highly lipophilic molecule should be poorly soluble."""
        mol = Chem.MolFromSmiles("CCCCCCCCCCCCCCCC")  # Hexadecane
        result = calculate_admet(mol)

        assert result.solubility.log_s < -4
        assert result.solubility.classification in ["poor", "insoluble"]

    def test_solubility_mg_ml_positive(self):
        """Solubility in mg/mL should be positive."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_admet(mol)

        assert result.solubility.solubility_mg_ml > 0


class TestComplexity:
    """Tests for molecular complexity metrics."""

    def test_ethanol_low_fsp3(self):
        """Ethanol has high Fsp3 (all sp3 carbons)."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_admet(mol)

        assert result.complexity.fsp3 == 1.0
        assert result.complexity.classification == "3d"

    def test_benzene_zero_fsp3(self):
        """Benzene has zero Fsp3 (all aromatic)."""
        mol = Chem.MolFromSmiles("c1ccccc1")
        result = calculate_admet(mol)

        assert result.complexity.fsp3 == 0.0
        assert result.complexity.classification == "flat"

    def test_fsp3_range(self):
        """Fsp3 should be between 0 and 1."""
        mol = Chem.MolFromSmiles("CC(=O)OC1=CC=CC=C1C(=O)O")
        result = calculate_admet(mol)

        assert 0 <= result.complexity.fsp3 <= 1

    def test_morphine_has_stereocenters(self):
        """Morphine should have stereocenters."""
        mol = Chem.MolFromSmiles("CN1CCC23C4C1CC5=C2C(=C(C=C5)O)OC3C(C=C4)O")
        result = calculate_admet(mol)

        assert result.complexity.num_stereocenters > 0

    def test_ring_counts(self):
        """Ring counts should be accurate."""
        mol = Chem.MolFromSmiles("c1ccc2ccccc2c1")  # Naphthalene
        result = calculate_admet(mol)

        assert result.complexity.num_rings == 2
        assert result.complexity.num_aromatic_rings == 2

    def test_bertz_complexity(self):
        """Bertz complexity should be calculated."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_admet(mol)

        assert result.complexity.bertz_ct >= 0


class TestCNSMPO:
    """Tests for CNS MPO score."""

    def test_cns_mpo_range(self):
        """CNS MPO should be in valid range."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_admet(mol)

        assert 0 <= result.cns_mpo.score <= 6

    def test_caffeine_cns_penetrant(self):
        """Caffeine should be CNS penetrant."""
        mol = Chem.MolFromSmiles("CN1C=NC2=C1C(=O)N(C(=O)N2C)C")
        result = calculate_admet(mol)

        # Caffeine is known to cross BBB
        assert result.cns_mpo is not None
        assert result.cns_mpo.score > 0

    def test_cns_mpo_components(self):
        """CNS MPO should have component scores."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_admet(mol)

        assert "mw" in result.cns_mpo.components
        assert "logp" in result.cns_mpo.components
        assert "tpsa" in result.cns_mpo.components
        assert "hbd" in result.cns_mpo.components

    def test_cns_mpo_optional(self):
        """CNS MPO should be optional."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_admet(mol, include_cns_mpo=False)

        assert result.cns_mpo is None


class TestBioavailability:
    """Tests for bioavailability indicators."""

    def test_aspirin_oral_bioavailable(self):
        """Aspirin should be orally bioavailable."""
        mol = Chem.MolFromSmiles("CC(=O)OC1=CC=CC=C1C(=O)O")
        result = calculate_admet(mol)

        assert result.bioavailability.oral_absorption_likely is True

    def test_bioavailability_properties(self):
        """Bioavailability should include key properties."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_admet(mol)

        assert result.bioavailability.tpsa >= 0
        assert result.bioavailability.rotatable_bonds >= 0
        assert result.bioavailability.hbd >= 0
        assert result.bioavailability.hba >= 0
        assert result.bioavailability.mw > 0
        assert result.bioavailability.logp is not None

    def test_large_molecule_not_oral(self):
        """Very large molecule may not be orally bioavailable."""
        # Large peptide-like structure
        smiles = "NCCCC(=O)NCCCC(=O)NCCCC(=O)NCCCC(=O)NCCCC(=O)NCCCC(=O)NCCCCC"
        mol = Chem.MolFromSmiles(smiles)
        result = calculate_admet(mol)

        # Should have bioavailability concerns
        assert (
            result.bioavailability.mw > 500
            or result.bioavailability.rotatable_bonds > 10
        )


class TestOverallInterpretation:
    """Tests for overall ADMET interpretation."""

    def test_interpretation_not_empty(self):
        """Interpretation should not be empty."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_admet(mol)

        assert result.interpretation
        assert len(result.interpretation) > 10

    def test_interpretation_mentions_key_metrics(self):
        """Interpretation should mention key metrics."""
        mol = Chem.MolFromSmiles("CCO")
        result = calculate_admet(mol)

        # Should mention at least some key metrics
        interp_lower = result.interpretation.lower()
        assert any(
            term in interp_lower
            for term in ["synthesis", "solubility", "complexity", "bioavailability"]
        )


class TestEdgeCases:
    """Tests for edge cases."""

    def test_single_atom(self):
        """Single atom should be handled."""
        mol = Chem.MolFromSmiles("[Na]")
        result = calculate_admet(mol)

        assert result.synthetic_accessibility is not None
        assert result.solubility is not None
        assert result.complexity is not None

    def test_large_molecule(self):
        """Large molecule should be handled."""
        mol = Chem.MolFromSmiles("C" * 50)  # Long alkane
        result = calculate_admet(mol)

        assert result is not None
        assert result.synthetic_accessibility.score > 1

    def test_aromatic_only(self):
        """Fully aromatic molecule should be handled."""
        mol = Chem.MolFromSmiles("c1ccc2ccccc2c1")  # Naphthalene
        result = calculate_admet(mol)

        assert result.complexity.fsp3 == 0.0
        assert result.complexity.classification == "flat"

    def test_scorer_reuse(self):
        """Scorer should be reusable."""
        scorer = ADMETScorer()

        mol1 = Chem.MolFromSmiles("CCO")
        mol2 = Chem.MolFromSmiles("c1ccccc1")

        result1 = scorer.score(mol1)
        result2 = scorer.score(mol2)

        assert result1 is not None
        assert result2 is not None
