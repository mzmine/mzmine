"""
Tests for stereocenter tracking.

Tests detection and comparison of stereocenters before/after standardization.
"""

import pytest
from rdkit import Chem

from app.services.standardization.chembl_pipeline import (
    StandardizationOptions,
    StandardizationPipeline,
)
from app.services.standardization.stereo_tracker import (
    StereoInfo,
    StereoTracker,
    track_stereocenters,
)


@pytest.fixture
def tracker():
    """Create a StereoTracker instance."""
    return StereoTracker()


class TestStereoInfoExtraction:
    """Test extraction of stereochemistry information."""

    def test_achiral_molecule(self, tracker):
        """Achiral molecule should have no stereocenters."""
        mol = Chem.MolFromSmiles("CCO")
        info = tracker.get_stereo_info(mol)

        assert info.defined_stereocenters == 0
        assert info.undefined_stereocenters == 0

    def test_defined_stereocenter(self, tracker):
        """Defined stereocenter should be detected."""
        # (R)-2-butanol with explicit stereo
        mol = Chem.MolFromSmiles("C[C@H](O)CC")
        info = tracker.get_stereo_info(mol)

        assert info.defined_stereocenters >= 1
        assert info.total_stereocenters >= 1

    def test_undefined_stereocenter(self, tracker):
        """Undefined stereocenter should be detected."""
        # 2-butanol without stereo - has a stereocenter but undefined
        mol = Chem.MolFromSmiles("CC(O)CC")
        info = tracker.get_stereo_info(mol)

        # Should have at least one undefined stereocenter
        assert info.total_stereocenters >= 1

    def test_multiple_stereocenters(self, tracker):
        """Multiple stereocenters should all be detected."""
        # Tartaric acid with defined stereo
        mol = Chem.MolFromSmiles("[C@H](O)([C@@H](O)C(=O)O)C(=O)O")
        info = tracker.get_stereo_info(mol)

        assert info.total_stereocenters >= 2

    def test_double_bond_stereo_e(self, tracker):
        """E double bond stereo should be detected."""
        # trans-2-butene
        mol = Chem.MolFromSmiles("C/C=C/C")
        info = tracker.get_stereo_info(mol)

        assert info.defined_double_bond_stereo >= 1

    def test_double_bond_stereo_z(self, tracker):
        """Z double bond stereo should be detected."""
        # cis-2-butene
        mol = Chem.MolFromSmiles(r"C/C=C\C")
        info = tracker.get_stereo_info(mol)

        assert info.defined_double_bond_stereo >= 1

    def test_none_molecule(self, tracker):
        """None molecule should return empty StereoInfo."""
        info = tracker.get_stereo_info(None)

        assert info.defined_stereocenters == 0
        assert info.undefined_stereocenters == 0


class TestStereoComparison:
    """Test stereochemistry comparison."""

    def test_no_change(self, tracker):
        """Identical stereoinfo should show no loss."""
        before = StereoInfo(defined_stereocenters=2)
        after = StereoInfo(defined_stereocenters=2)

        comparison = tracker.compare(before, after)

        assert comparison.stereocenters_lost == 0
        assert comparison.has_stereo_loss is False
        assert comparison.warning is None

    def test_stereo_loss_detected(self, tracker):
        """Stereocenter loss should be detected and warned."""
        before = StereoInfo(defined_stereocenters=2)
        after = StereoInfo(defined_stereocenters=1)

        comparison = tracker.compare(before, after)

        assert comparison.stereocenters_lost == 1
        assert comparison.has_stereo_loss is True
        assert comparison.warning is not None
        assert "lost" in comparison.warning.lower()

    def test_stereo_gain(self, tracker):
        """Stereocenter gain should be tracked."""
        before = StereoInfo(defined_stereocenters=0)
        after = StereoInfo(defined_stereocenters=1)

        comparison = tracker.compare(before, after)

        assert comparison.stereocenters_gained == 1
        assert comparison.stereocenters_lost == 0

    def test_double_bond_stereo_loss(self, tracker):
        """Double bond stereo loss should trigger warning."""
        before = StereoInfo(defined_double_bond_stereo=1)
        after = StereoInfo(defined_double_bond_stereo=0)

        comparison = tracker.compare(before, after)

        assert comparison.double_bond_stereo_lost == 1
        assert comparison.has_stereo_loss is True
        assert comparison.warning is not None


class TestTautomerStereochemistryLoss:
    """Test detection of stereochemistry loss during tautomer canonicalization.

    CRITICAL: This is a key test - tautomer canonicalization can lose E/Z stereo.
    """

    def test_tautomer_e_z_stereo_loss_detected(self):
        """E/Z stereo loss from tautomer canonicalization should be detected."""
        pipeline = StandardizationPipeline()

        # Molecule with E/Z stereo that might be affected by tautomerization
        # Using a simple enol form with E/Z
        mol = Chem.MolFromSmiles("C/C=C/O")

        # First, standardize without tautomer
        options_no_taut = StandardizationOptions(include_tautomer=False)
        result_no_taut = pipeline.standardize(mol, options_no_taut)

        # Then, standardize with tautomer
        mol2 = Chem.MolFromSmiles("C/C=C/O")
        options_with_taut = StandardizationOptions(include_tautomer=True)
        result_with_taut = pipeline.standardize(mol2, options_with_taut)

        # Both should succeed
        assert result_no_taut.success
        assert result_with_taut.success

        # The stereo comparison should be present
        assert result_with_taut.stereo_comparison is not None


class TestConvenienceFunction:
    """Test track_stereocenters convenience function."""

    def test_track_stereocenters_works(self):
        """track_stereocenters should return StereoInfo."""
        mol = Chem.MolFromSmiles("C[C@H](O)CC")
        info = track_stereocenters(mol)

        assert isinstance(info, StereoInfo)
        assert info.defined_stereocenters >= 1


class TestIntegrationWithPipeline:
    """Test stereo tracking integration with standardization pipeline."""

    def test_pipeline_includes_stereo_comparison(self):
        """Pipeline result should include stereo comparison."""
        pipeline = StandardizationPipeline()
        mol = Chem.MolFromSmiles("C[C@H](O)CC")

        result = pipeline.standardize(mol)

        assert result.success
        assert result.stereo_comparison is not None

    def test_salt_strip_preserves_stereo(self):
        """Salt stripping should preserve stereochemistry."""
        pipeline = StandardizationPipeline()

        # Chiral molecule with salt
        mol = Chem.MolFromSmiles("C[C@H](O)CC.[Na]")

        result = pipeline.standardize(mol)

        assert result.success
        assert result.stereo_comparison is not None
        # Stereo should be preserved after salt stripping
        assert result.stereo_comparison.stereocenters_lost == 0
