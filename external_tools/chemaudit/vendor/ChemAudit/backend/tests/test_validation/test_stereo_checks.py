"""
Tests for stereochemistry validation checks.
"""

from rdkit import Chem

from app.schemas.common import Severity
from app.services.validation.checks.stereo import (
    ConflictingStereoCheck,
    UndefinedDoubleBondStereoCheck,
    UndefinedStereoCentersCheck,
)


class TestUndefinedStereoCentersCheck:
    """Test undefined stereocenter detection."""

    def test_no_stereocenters(self):
        """Molecules without chiral centers should pass."""
        mol = Chem.MolFromSmiles("CCO")  # Ethanol, no chiral centers
        check = UndefinedStereoCentersCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO
        assert "No chiral centers" in result.message

    def test_defined_stereocenter(self):
        """Molecules with defined stereocenters should pass."""
        mol = Chem.MolFromSmiles("C[C@H](O)CC")  # Defined R/S
        check = UndefinedStereoCentersCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO
        assert "properly defined" in result.message

    def test_undefined_stereocenter(self):
        """Molecules with undefined stereocenters should fail with WARNING."""
        mol = Chem.MolFromSmiles("CC(O)CC")  # Chiral center but not specified
        check = UndefinedStereoCentersCheck()
        result = check.run(mol)

        assert not result.passed
        assert result.severity == Severity.WARNING
        assert "undefined stereocenter" in result.message
        assert len(result.affected_atoms) > 0

    def test_multiple_undefined(self):
        """Should detect multiple undefined centers."""
        # Two chiral centers, neither defined
        mol = Chem.MolFromSmiles("CC(O)C(O)C")
        check = UndefinedStereoCentersCheck()
        result = check.run(mol)

        assert not result.passed
        assert result.severity == Severity.WARNING
        assert result.details["undefined_count"] == 2

    def test_mixed_defined_undefined(self):
        """Should detect only undefined centers when mixed."""
        # One defined, one undefined
        mol = Chem.MolFromSmiles("C[C@H](O)C(O)C")
        check = UndefinedStereoCentersCheck()
        result = check.run(mol)

        assert not result.passed
        assert result.severity == Severity.WARNING
        assert result.details["undefined_count"] == 1
        assert result.details["total_centers"] == 2

    def test_none_molecule(self):
        """Should handle None molecule."""
        check = UndefinedStereoCentersCheck()
        result = check.run(None)

        assert not result.passed
        assert result.severity == Severity.ERROR


class TestUndefinedDoubleBondStereoCheck:
    """Test undefined double bond stereochemistry detection."""

    def test_no_double_bonds(self):
        """Molecules without double bonds should pass."""
        mol = Chem.MolFromSmiles("CCC")  # Propane
        check = UndefinedDoubleBondStereoCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO
        assert "No double bonds" in result.message

    def test_defined_ez_stereo(self):
        """Double bonds with defined E/Z should pass."""
        mol = Chem.MolFromSmiles("C/C=C/C")  # Trans-2-butene
        check = UndefinedDoubleBondStereoCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO

    def test_undefined_ez_stereo(self):
        """Double bonds without E/Z specification should fail with WARNING."""
        mol = Chem.MolFromSmiles("CC=CC")  # 2-butene, no E/Z specified
        check = UndefinedDoubleBondStereoCheck()
        result = check.run(mol)

        assert not result.passed
        assert result.severity == Severity.WARNING
        assert "undefined E/Z" in result.message
        assert len(result.affected_atoms) >= 2

    def test_terminal_double_bond(self):
        """Terminal double bonds (no E/Z possible) should pass."""
        mol = Chem.MolFromSmiles("C=CC")  # Propene, no E/Z possible
        check = UndefinedDoubleBondStereoCheck()
        result = check.run(mol)

        # Terminal double bond has no E/Z, so should pass
        assert result.passed
        assert result.severity == Severity.INFO

    def test_cyclic_double_bond(self):
        """Double bonds in rings should be ignored."""
        mol = Chem.MolFromSmiles("C1=CC=CC=C1")  # Benzene
        check = UndefinedDoubleBondStereoCheck()
        result = check.run(mol)

        # All double bonds in ring, should pass
        assert result.passed
        assert result.severity == Severity.INFO

    def test_none_molecule(self):
        """Should handle None molecule."""
        check = UndefinedDoubleBondStereoCheck()
        result = check.run(None)

        assert not result.passed
        assert result.severity == Severity.ERROR


class TestConflictingStereoCheck:
    """Test conflicting stereochemistry detection."""

    def test_no_conflicts(self):
        """Valid molecules should pass."""
        mol = Chem.MolFromSmiles("C[C@H](O)CC")
        check = ConflictingStereoCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO
        assert "No stereochemistry conflicts" in result.message

    def test_simple_molecule(self):
        """Simple molecules should pass."""
        mol = Chem.MolFromSmiles("CCO")
        check = ConflictingStereoCheck()
        result = check.run(mol)

        assert result.passed
        assert result.severity == Severity.INFO

    def test_none_molecule(self):
        """Should handle None molecule."""
        check = ConflictingStereoCheck()
        result = check.run(None)

        assert not result.passed
        assert result.severity == Severity.ERROR


class TestRegistration:
    """Test that checks are properly registered."""

    def test_checks_registered(self):
        """Verify all stereo checks are in registry."""
        from app.services.validation.registry import CheckRegistry

        registered = CheckRegistry.list_names()

        assert "undefined_stereocenters" in registered
        assert "undefined_doublebond_stereo" in registered
        assert "conflicting_stereo" in registered
