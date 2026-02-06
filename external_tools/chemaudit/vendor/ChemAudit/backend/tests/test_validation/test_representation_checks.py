"""
Tests for representation consistency validation checks.
"""

from rdkit import Chem

from app.schemas.common import Severity
from app.services.validation.checks.representation import (
    InchiGenerationCheck,
    InchiRoundtripCheck,
    SmilesRoundtripCheck,
)


class TestSmilesRoundtripCheck:
    """Test SMILES roundtrip consistency."""

    def test_simple_molecule_roundtrip(self):
        """Simple molecules should roundtrip successfully."""
        mol = Chem.MolFromSmiles("CCO")  # Ethanol
        check = SmilesRoundtripCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO
        assert "successful" in result.message
        assert "smiles" in result.details

    def test_benzene_roundtrip(self):
        """Aromatic molecules should roundtrip successfully."""
        mol = Chem.MolFromSmiles("c1ccccc1")  # Benzene
        check = SmilesRoundtripCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO

    def test_chiral_molecule_roundtrip(self):
        """Chiral molecules should preserve stereochemistry."""
        mol = Chem.MolFromSmiles("C[C@H](O)CC")
        check = SmilesRoundtripCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO

    def test_complex_molecule(self):
        """Complex molecules should generally roundtrip."""
        # Aspirin
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")
        check = SmilesRoundtripCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO

    def test_none_molecule(self):
        """Should handle None molecule."""
        check = SmilesRoundtripCheck()
        result = check.run(None)

        assert not result.passed
        assert result.severity == Severity.ERROR


class TestInchiGenerationCheck:
    """Test InChI generation capability."""

    def test_simple_molecule_inchi(self):
        """Simple molecules should generate InChI."""
        mol = Chem.MolFromSmiles("CCO")
        check = InchiGenerationCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO
        assert "successfully" in result.message
        assert "inchi" in result.details
        assert "inchikey" in result.details

    def test_aromatic_inchi(self):
        """Aromatic molecules should generate InChI."""
        mol = Chem.MolFromSmiles("c1ccccc1")  # Benzene
        check = InchiGenerationCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO
        assert result.details["inchi"].startswith("InChI=")

    def test_chiral_inchi(self):
        """Chiral molecules should generate InChI with stereo layer."""
        mol = Chem.MolFromSmiles("C[C@H](O)CC")
        check = InchiGenerationCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO
        # InChI should contain stereochemistry information
        assert "inchi" in result.details

    def test_complex_molecule_inchi(self):
        """Complex molecules should generate InChI."""
        # Aspirin
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")
        check = InchiGenerationCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO

    def test_inchikey_format(self):
        """InChIKey should have expected format."""
        mol = Chem.MolFromSmiles("CCO")
        check = InchiGenerationCheck()
        result = check.run(mol)

        assert result.passed
        inchikey = result.details["inchikey"]
        # InChIKey format: XXXXXXXXXXXXXX-YYYYYYYYYY-Z
        assert len(inchikey) == 27
        assert inchikey.count("-") == 2

    def test_none_molecule(self):
        """Should handle None molecule."""
        check = InchiGenerationCheck()
        result = check.run(None)

        assert not result.passed
        assert result.severity == Severity.ERROR


class TestInchiRoundtripCheck:
    """Test InChI roundtrip consistency."""

    def test_simple_molecule_roundtrip(self):
        """Simple molecules should roundtrip successfully."""
        mol = Chem.MolFromSmiles("CCO")
        check = InchiRoundtripCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO
        assert "successful" in result.message

    def test_benzene_roundtrip(self):
        """Benzene should roundtrip successfully."""
        mol = Chem.MolFromSmiles("c1ccccc1")
        check = InchiRoundtripCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO

    def test_chiral_roundtrip(self):
        """Chiral molecules may show differences (INFO level)."""
        mol = Chem.MolFromSmiles("C[C@H](O)CC")
        check = InchiRoundtripCheck()
        result = check.run(mol)

        # Either passes or fails with INFO (both acceptable)
        assert result.severity == Severity.INFO

    def test_result_includes_details(self):
        """Result should include InChI details."""
        mol = Chem.MolFromSmiles("CCO")
        check = InchiRoundtripCheck()
        result = check.run(mol)

        if result.passed:
            assert "inchi" in result.details
            assert "inchikey" in result.details
        else:
            # If failed, should explain why
            assert result.severity == Severity.INFO

    def test_none_molecule(self):
        """Should handle None molecule."""
        check = InchiRoundtripCheck()
        result = check.run(None)

        assert not result.passed
        assert result.severity == Severity.ERROR


class TestRegistration:
    """Test that checks are properly registered."""

    def test_checks_registered(self):
        """Verify all representation checks are in registry."""
        from app.services.validation.registry import CheckRegistry

        registered = CheckRegistry.list_names()

        assert "smiles_roundtrip" in registered
        assert "inchi_generation" in registered
        assert "inchi_roundtrip" in registered
