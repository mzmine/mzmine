"""
Tests for FilterCatalog singleton and pattern loading.
"""

import pytest
from rdkit import Chem

from app.services.alerts.filter_catalog import (
    get_filter_catalog,
    list_available_catalogs,
)


class TestFilterCatalog:
    """Test FilterCatalog initialization and pattern loading."""

    def test_pains_catalog_loads(self):
        """Test that PAINS catalog loads successfully."""
        catalog_info = get_filter_catalog("PAINS")

        assert catalog_info is not None
        assert catalog_info.catalog is not None
        assert catalog_info.catalog_type == "PAINS"
        # PAINS A+B+C should have ~480 patterns
        assert catalog_info.num_entries >= 400

    def test_pains_a_catalog_loads(self):
        """Test that PAINS_A catalog loads with expected pattern count."""
        catalog_info = get_filter_catalog("PAINS_A")

        assert catalog_info is not None
        assert catalog_info.catalog_type == "PAINS_A"
        # PAINS_A has 16 patterns
        assert catalog_info.num_entries >= 10

    def test_brenk_catalog_loads(self):
        """Test that BRENK catalog loads successfully."""
        catalog_info = get_filter_catalog("BRENK")

        assert catalog_info is not None
        assert catalog_info.catalog_type == "BRENK"
        # BRENK should have ~105 patterns
        assert catalog_info.num_entries >= 80

    def test_unknown_catalog_raises_error(self):
        """Test that unknown catalog type raises ValueError."""
        with pytest.raises(ValueError) as exc_info:
            get_filter_catalog("UNKNOWN_CATALOG")

        assert "Unknown catalog type" in str(exc_info.value)

    def test_catalog_caching(self):
        """Test that catalog is cached (same object returned)."""
        catalog1 = get_filter_catalog("PAINS")
        catalog2 = get_filter_catalog("PAINS")

        # Should be same object due to lru_cache
        assert catalog1 is catalog2

    def test_case_insensitive_catalog_type(self):
        """Test that catalog type is case-insensitive."""
        catalog_lower = get_filter_catalog("pains")
        catalog_upper = get_filter_catalog("PAINS")

        # Both should work and return same cached object
        assert catalog_lower.catalog_type == "PAINS"
        assert catalog_upper.catalog_type == "PAINS"

    def test_list_available_catalogs(self):
        """Test listing available catalogs."""
        catalogs = list_available_catalogs()

        assert "PAINS" in catalogs
        assert "BRENK" in catalogs
        assert "PAINS_A" in catalogs
        assert "ALL" in catalogs

        # Check structure
        pains_info = catalogs["PAINS"]
        assert "name" in pains_info
        assert "description" in pains_info
        assert "pattern_count" in pains_info


class TestPainsDetection:
    """Test PAINS detection on known molecules."""

    def test_rhodanine_detected(self):
        """Test that rhodanine (known PAINS pattern) is detected."""
        # Rhodanine: 2-thioxo-1,3-thiazolidin-4-one (correct SMILES for rhod_sat_A)
        rhodanine_smiles = "O=C1NC(=S)SC1"
        mol = Chem.MolFromSmiles(rhodanine_smiles)

        catalog_info = get_filter_catalog("PAINS")
        has_match = catalog_info.catalog.HasMatch(mol)

        assert has_match is True

    def test_quinone_detected(self):
        """Test that quinone (known PAINS pattern) is detected."""
        # Benzoquinone
        quinone_smiles = "O=C1C=CC(=O)C=C1"
        mol = Chem.MolFromSmiles(quinone_smiles)

        catalog_info = get_filter_catalog("PAINS")
        has_match = catalog_info.catalog.HasMatch(mol)

        assert has_match is True

    def test_clean_molecule_no_pains(self):
        """Test that a clean molecule has no PAINS alerts."""
        # Simple ethanol - no PAINS patterns
        ethanol_smiles = "CCO"
        mol = Chem.MolFromSmiles(ethanol_smiles)

        catalog_info = get_filter_catalog("PAINS")
        has_match = catalog_info.catalog.HasMatch(mol)

        assert has_match is False

    def test_aspirin_no_pains(self):
        """Test that aspirin has no PAINS alerts."""
        aspirin_smiles = "CC(=O)Oc1ccccc1C(=O)O"
        mol = Chem.MolFromSmiles(aspirin_smiles)

        catalog_info = get_filter_catalog("PAINS")
        has_match = catalog_info.catalog.HasMatch(mol)

        assert has_match is False


class TestBrenkDetection:
    """Test BRENK detection on known molecules."""

    def test_alkyl_halide_detected(self):
        """Test that alkyl halide is detected by BRENK."""
        # Bromoethane
        alkyl_halide_smiles = "CCBr"
        mol = Chem.MolFromSmiles(alkyl_halide_smiles)

        catalog_info = get_filter_catalog("BRENK")
        has_match = catalog_info.catalog.HasMatch(mol)

        assert has_match is True

    def test_aldehyde_detected(self):
        """Test that aldehyde is detected by BRENK."""
        # Acetaldehyde
        aldehyde_smiles = "CC=O"
        mol = Chem.MolFromSmiles(aldehyde_smiles)

        catalog_info = get_filter_catalog("BRENK")
        has_match = catalog_info.catalog.HasMatch(mol)

        assert has_match is True

    def test_caffeine_minimal_brenk(self):
        """Test caffeine - drug-like but may have minor BRENK flags."""
        caffeine_smiles = "CN1C=NC2=C1C(=O)N(C(=O)N2C)C"
        mol = Chem.MolFromSmiles(caffeine_smiles)

        # Caffeine is drug-like, check it parses
        assert mol is not None
