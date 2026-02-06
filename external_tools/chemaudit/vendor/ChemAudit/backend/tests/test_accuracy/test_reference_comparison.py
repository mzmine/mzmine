"""
Reference Accuracy Tests

Verifies ChemAudit validation results match RDKit reference implementation.
Target: 99%+ agreement with reference data.

These tests ensure our validation checks produce consistent, reliable results
that match expected RDKit behavior across diverse molecule types.
"""

import json
from pathlib import Path
from typing import Any, Dict, List

import pytest
from rdkit import Chem

from app.services.parser import parse_molecule
from app.services.validation.engine import validation_engine

# Load reference data
REFERENCE_DATA_PATH = Path(__file__).parent / "reference_data.json"


def load_reference_data() -> List[Dict[str, Any]]:
    """Load reference molecule data from JSON file."""
    with open(REFERENCE_DATA_PATH, "r") as f:
        data = json.load(f)
    return data["molecules"]


REFERENCE_MOLECULES = load_reference_data()


# Group molecules by category for organized testing
VALID_MOLECULES = [
    m for m in REFERENCE_MOLECULES if m["expected"].get("parsable", True)
]
INVALID_MOLECULES = [
    m for m in REFERENCE_MOLECULES if not m["expected"].get("parsable", True)
]


class TestReferenceDataIntegrity:
    """Verify reference data file integrity."""

    def test_reference_data_loads(self):
        """Reference data file should load successfully."""
        data = load_reference_data()
        assert len(data) > 0

    def test_minimum_molecules(self):
        """Should have at least 50 reference molecules."""
        data = load_reference_data()
        assert len(data) >= 50, f"Expected 50+ molecules, got {len(data)}"

    def test_molecule_has_required_fields(self):
        """Each molecule should have id, name, smiles, expected."""
        data = load_reference_data()
        for mol in data:
            assert "id" in mol, f"Molecule missing id: {mol}"
            assert "name" in mol, f"Molecule missing name: {mol}"
            assert "smiles" in mol, f"Molecule missing smiles: {mol}"
            assert "expected" in mol, f"Molecule missing expected: {mol}"

    def test_categories_represented(self):
        """Should have diverse category coverage."""
        data = load_reference_data()
        categories = set(mol.get("category", "unknown") for mol in data)

        required_categories = {
            "drug-like",
            "simple",
            "stereochemistry",
            "salt",
            "aromatic",
            "invalid",
        }
        missing = required_categories - categories
        assert not missing, f"Missing categories: {missing}"


class TestParsabilityAccuracy:
    """Test molecule parsing matches reference expectations."""

    @pytest.mark.parametrize(
        "molecule",
        VALID_MOLECULES,
        ids=lambda m: m["name"],
    )
    def test_valid_molecules_parse(self, molecule: Dict[str, Any]):
        """Valid molecules should parse successfully."""
        result = parse_molecule(molecule["smiles"])

        assert (
            result.success is True
        ), f"Failed to parse {molecule['name']}: {result.errors}"
        assert result.mol is not None

    @pytest.mark.parametrize(
        "molecule",
        INVALID_MOLECULES,
        ids=lambda m: m["name"],
    )
    def test_invalid_molecules_fail(self, molecule: Dict[str, Any]):
        """Invalid molecules should fail to parse."""
        result = parse_molecule(molecule["smiles"])

        # Empty input should definitely fail
        if molecule["smiles"] == "":
            assert (
                result.success is False
            ), f"{molecule['name']} should fail on empty input"
        elif "unclosed_ring" in molecule["expected"].get("parse_error", ""):
            # Unclosed ring should fail
            assert result.success is False or len(result.errors) > 0


class TestAtomCountAccuracy:
    """Test atom count matches reference."""

    @pytest.mark.parametrize(
        "molecule",
        [m for m in VALID_MOLECULES if "num_atoms" in m["expected"]],
        ids=lambda m: m["name"],
    )
    def test_atom_count_matches(self, molecule: Dict[str, Any]):
        """Atom count should match reference."""
        result = parse_molecule(molecule["smiles"])
        assert result.success is True

        expected = molecule["expected"]["num_atoms"]
        actual = result.mol.GetNumAtoms()

        assert (
            actual == expected
        ), f"{molecule['name']}: expected {expected} atoms, got {actual}"


class TestBondCountAccuracy:
    """Test bond count matches reference."""

    @pytest.mark.parametrize(
        "molecule",
        [m for m in VALID_MOLECULES if "num_bonds" in m["expected"]],
        ids=lambda m: m["name"],
    )
    def test_bond_count_matches(self, molecule: Dict[str, Any]):
        """Bond count should match reference."""
        result = parse_molecule(molecule["smiles"])
        assert result.success is True

        expected = molecule["expected"]["num_bonds"]
        actual = result.mol.GetNumBonds()

        assert (
            actual == expected
        ), f"{molecule['name']}: expected {expected} bonds, got {actual}"


class TestAromaticityAccuracy:
    """Test aromaticity detection matches reference."""

    @pytest.mark.parametrize(
        "molecule",
        [m for m in VALID_MOLECULES if "has_aromatic" in m["expected"]],
        ids=lambda m: m["name"],
    )
    def test_aromaticity_matches(self, molecule: Dict[str, Any]):
        """Aromaticity detection should match reference."""
        result = parse_molecule(molecule["smiles"])
        assert result.success is True

        expected = molecule["expected"]["has_aromatic"]
        actual = any(atom.GetIsAromatic() for atom in result.mol.GetAtoms())

        assert (
            actual == expected
        ), f"{molecule['name']}: expected aromatic={expected}, got {actual}"


class TestFragmentCountAccuracy:
    """Test fragment count matches reference."""

    @pytest.mark.parametrize(
        "molecule",
        [m for m in VALID_MOLECULES if "num_fragments" in m["expected"]],
        ids=lambda m: m["name"],
    )
    def test_fragment_count_matches(self, molecule: Dict[str, Any]):
        """Fragment count should match reference."""
        result = parse_molecule(molecule["smiles"])
        assert result.success is True

        expected = molecule["expected"]["num_fragments"]
        frags = Chem.GetMolFrags(result.mol, asMols=False, sanitizeFrags=False)
        actual = len(frags)

        assert (
            actual == expected
        ), f"{molecule['name']}: expected {expected} fragments, got {actual}"


class TestStereocenterAccuracy:
    """Test stereocenter detection matches reference."""

    @pytest.mark.parametrize(
        "molecule",
        [m for m in VALID_MOLECULES if "num_stereocenters" in m["expected"]],
        ids=lambda m: m["name"],
    )
    def test_stereocenter_count_matches(self, molecule: Dict[str, Any]):
        """Total stereocenter count should match reference."""
        result = parse_molecule(molecule["smiles"])
        assert result.success is True

        expected = molecule["expected"]["num_stereocenters"]
        chiral_centers = Chem.FindMolChiralCenters(
            result.mol, includeUnassigned=True, useLegacyImplementation=False
        )
        actual = len(chiral_centers)

        assert (
            actual == expected
        ), f"{molecule['name']}: expected {expected} stereocenters, got {actual}"

    @pytest.mark.parametrize(
        "molecule",
        [m for m in VALID_MOLECULES if "undefined_stereocenters" in m["expected"]],
        ids=lambda m: m["name"],
    )
    def test_undefined_stereocenter_count_matches(self, molecule: Dict[str, Any]):
        """Undefined stereocenter count should match reference."""
        result = parse_molecule(molecule["smiles"])
        assert result.success is True

        expected = molecule["expected"]["undefined_stereocenters"]
        chiral_centers = Chem.FindMolChiralCenters(
            result.mol, includeUnassigned=True, useLegacyImplementation=False
        )
        undefined = [c for c in chiral_centers if c[1] == "?"]
        actual = len(undefined)

        assert (
            actual == expected
        ), f"{molecule['name']}: expected {expected} undefined stereocenters, got {actual}"


class TestSanitizationAccuracy:
    """Test sanitization behavior matches reference."""

    @pytest.mark.parametrize(
        "molecule",
        [m for m in VALID_MOLECULES if "valid_sanitization" in m["expected"]],
        ids=lambda m: m["name"],
    )
    def test_sanitization_matches(self, molecule: Dict[str, Any]):
        """Sanitization result should match reference."""
        result = parse_molecule(molecule["smiles"])
        expected = molecule["expected"]["valid_sanitization"]

        if result.mol is None:
            actual = False
        else:
            # Try sanitizing
            mol_copy = Chem.Mol(result.mol)
            try:
                san_result = Chem.SanitizeMol(mol_copy, catchErrors=True)
                actual = san_result == Chem.SanitizeFlags.SANITIZE_NONE
            except Exception:
                actual = False

        assert (
            actual == expected
        ), f"{molecule['name']}: expected sanitization={expected}, got {actual}"


class TestInChIGenerationAccuracy:
    """Test InChI generation matches reference."""

    @pytest.mark.parametrize(
        "molecule",
        [m for m in VALID_MOLECULES if "inchi_generatable" in m["expected"]],
        ids=lambda m: m["name"],
    )
    def test_inchi_generation_matches(self, molecule: Dict[str, Any]):
        """InChI generability should match reference."""
        result = parse_molecule(molecule["smiles"])
        expected = molecule["expected"]["inchi_generatable"]

        if result.mol is None:
            actual = False
        else:
            try:
                inchi = Chem.MolToInchi(result.mol)
                actual = bool(inchi)
            except Exception:
                actual = False

        assert (
            actual == expected
        ), f"{molecule['name']}: expected InChI generatable={expected}, got {actual}"


class TestSMILESRoundtripAccuracy:
    """Test SMILES roundtrip consistency matches reference."""

    @pytest.mark.parametrize(
        "molecule",
        [m for m in VALID_MOLECULES if "smiles_roundtrip_consistent" in m["expected"]],
        ids=lambda m: m["name"],
    )
    def test_smiles_roundtrip_matches(self, molecule: Dict[str, Any]):
        """SMILES roundtrip consistency should match reference."""
        result = parse_molecule(molecule["smiles"])
        expected = molecule["expected"]["smiles_roundtrip_consistent"]

        if result.mol is None:
            actual = False
        else:
            try:
                # Get original InChIKey
                original_key = Chem.MolToInchiKey(result.mol)
                if not original_key:
                    actual = False
                else:
                    # Roundtrip through SMILES
                    smiles = Chem.MolToSmiles(result.mol)
                    roundtrip_mol = Chem.MolFromSmiles(smiles)
                    if roundtrip_mol is None:
                        actual = False
                    else:
                        roundtrip_key = Chem.MolToInchiKey(roundtrip_mol)
                        actual = original_key == roundtrip_key
            except Exception:
                actual = False

        assert (
            actual == expected
        ), f"{molecule['name']}: expected roundtrip consistent={expected}, got {actual}"


class TestValidationEngineAccuracy:
    """Test validation engine produces consistent results."""

    @pytest.mark.parametrize(
        "molecule",
        [m for m in VALID_MOLECULES if m["expected"].get("valid_sanitization", False)],
        ids=lambda m: m["name"],
    )
    def test_validation_engine_runs(self, molecule: Dict[str, Any]):
        """Validation engine should run without error on valid molecules."""
        result = parse_molecule(molecule["smiles"])
        assert result.success is True
        assert result.mol is not None

        # Run validation
        results, score = validation_engine.validate(result.mol)

        # Should have results for all checks
        assert len(results) > 0

        # Score should be 0-100
        assert 0 <= score <= 100

        # Parsability check should always pass for valid molecules
        parsability_result = next(
            (r for r in results if r.check_name == "parsability"), None
        )
        assert parsability_result is not None
        assert parsability_result.passed is True

    @pytest.mark.parametrize(
        "molecule",
        [m for m in VALID_MOLECULES if m["expected"].get("num_fragments", 1) > 1],
        ids=lambda m: m["name"],
    )
    def test_connectivity_check_detects_fragments(self, molecule: Dict[str, Any]):
        """Connectivity check should detect multi-fragment molecules."""
        result = parse_molecule(molecule["smiles"])
        assert result.success is True

        results, _ = validation_engine.validate(result.mol)

        connectivity_result = next(
            (r for r in results if r.check_name == "connectivity"), None
        )
        assert connectivity_result is not None
        assert connectivity_result.passed is False  # Multi-fragment = warning

        expected_frags = molecule["expected"]["num_fragments"]
        assert connectivity_result.details.get("num_fragments") == expected_frags

    @pytest.mark.parametrize(
        "molecule",
        [
            m
            for m in VALID_MOLECULES
            if m["expected"].get("undefined_stereocenters", 0) > 0
        ],
        ids=lambda m: m["name"],
    )
    def test_stereo_check_detects_undefined(self, molecule: Dict[str, Any]):
        """Stereochemistry check should detect undefined centers."""
        result = parse_molecule(molecule["smiles"])
        assert result.success is True

        results, _ = validation_engine.validate(result.mol)

        stereo_result = next(
            (r for r in results if r.check_name == "undefined_stereocenters"), None
        )
        assert stereo_result is not None

        expected_undefined = molecule["expected"]["undefined_stereocenters"]
        if expected_undefined > 0:
            assert stereo_result.passed is False


class TestOverallAccuracy:
    """Test overall accuracy metrics."""

    def test_parsing_accuracy(self):
        """Verify 99%+ parsing accuracy against reference."""
        correct = 0
        total = 0

        for mol in REFERENCE_MOLECULES:
            expected_parsable = mol["expected"].get("parsable", True)
            result = parse_molecule(mol["smiles"])

            # For valid molecules, check if parsing succeeded
            if expected_parsable:
                if result.success:
                    correct += 1
            else:
                # For invalid, we expect failure (or at least errors)
                if not result.success or len(result.errors) > 0:
                    correct += 1
            total += 1

        accuracy = correct / total if total > 0 else 0
        accuracy_pct = accuracy * 100

        assert accuracy_pct >= 99.0, (
            f"Parsing accuracy {accuracy_pct:.1f}% below 99% target "
            f"({correct}/{total} correct)"
        )

    def test_atom_count_accuracy(self):
        """Verify 99%+ atom count accuracy against reference."""
        correct = 0
        total = 0

        for mol in REFERENCE_MOLECULES:
            if "num_atoms" not in mol["expected"]:
                continue

            expected = mol["expected"]["num_atoms"]
            result = parse_molecule(mol["smiles"])

            if result.success and result.mol is not None:
                actual = result.mol.GetNumAtoms()
                if actual == expected:
                    correct += 1
            total += 1

        accuracy = correct / total if total > 0 else 0
        accuracy_pct = accuracy * 100

        assert accuracy_pct >= 99.0, (
            f"Atom count accuracy {accuracy_pct:.1f}% below 99% target "
            f"({correct}/{total} correct)"
        )

    def test_aromaticity_accuracy(self):
        """Verify 99%+ aromaticity detection accuracy against reference."""
        correct = 0
        total = 0

        for mol in REFERENCE_MOLECULES:
            if "has_aromatic" not in mol["expected"]:
                continue

            expected = mol["expected"]["has_aromatic"]
            result = parse_molecule(mol["smiles"])

            if result.success and result.mol is not None:
                actual = any(atom.GetIsAromatic() for atom in result.mol.GetAtoms())
                if actual == expected:
                    correct += 1
            total += 1

        accuracy = correct / total if total > 0 else 0
        accuracy_pct = accuracy * 100

        assert accuracy_pct >= 99.0, (
            f"Aromaticity accuracy {accuracy_pct:.1f}% below 99% target "
            f"({correct}/{total} correct)"
        )

    def test_fragment_count_accuracy(self):
        """Verify 99%+ fragment count accuracy against reference."""
        correct = 0
        total = 0

        for mol in REFERENCE_MOLECULES:
            if "num_fragments" not in mol["expected"]:
                continue

            expected = mol["expected"]["num_fragments"]
            result = parse_molecule(mol["smiles"])

            if result.success and result.mol is not None:
                frags = Chem.GetMolFrags(result.mol, asMols=False, sanitizeFrags=False)
                actual = len(frags)
                if actual == expected:
                    correct += 1
            total += 1

        accuracy = correct / total if total > 0 else 0
        accuracy_pct = accuracy * 100

        assert accuracy_pct >= 99.0, (
            f"Fragment count accuracy {accuracy_pct:.1f}% below 99% target "
            f"({correct}/{total} correct)"
        )

    def test_stereocenter_accuracy(self):
        """Verify 99%+ stereocenter detection accuracy against reference."""
        correct = 0
        total = 0

        for mol in REFERENCE_MOLECULES:
            if "num_stereocenters" not in mol["expected"]:
                continue

            expected = mol["expected"]["num_stereocenters"]
            result = parse_molecule(mol["smiles"])

            if result.success and result.mol is not None:
                chiral_centers = Chem.FindMolChiralCenters(
                    result.mol, includeUnassigned=True, useLegacyImplementation=False
                )
                actual = len(chiral_centers)
                if actual == expected:
                    correct += 1
            total += 1

        accuracy = correct / total if total > 0 else 0
        accuracy_pct = accuracy * 100

        assert accuracy_pct >= 99.0, (
            f"Stereocenter accuracy {accuracy_pct:.1f}% below 99% target "
            f"({correct}/{total} correct)"
        )

    def test_overall_validation_accuracy(self):
        """Verify overall validation accuracy meets 99% target."""
        metrics = {
            "parsing": {"correct": 0, "total": 0},
            "atoms": {"correct": 0, "total": 0},
            "bonds": {"correct": 0, "total": 0},
            "aromatic": {"correct": 0, "total": 0},
            "fragments": {"correct": 0, "total": 0},
            "stereocenters": {"correct": 0, "total": 0},
            "inchi": {"correct": 0, "total": 0},
        }

        for mol in REFERENCE_MOLECULES:
            result = parse_molecule(mol["smiles"])
            expected = mol["expected"]

            # Parsing
            expected_parsable = expected.get("parsable", True)
            metrics["parsing"]["total"] += 1
            if expected_parsable == result.success:
                metrics["parsing"]["correct"] += 1

            if not result.success or result.mol is None:
                continue

            # Atoms
            if "num_atoms" in expected:
                metrics["atoms"]["total"] += 1
                if result.mol.GetNumAtoms() == expected["num_atoms"]:
                    metrics["atoms"]["correct"] += 1

            # Bonds
            if "num_bonds" in expected:
                metrics["bonds"]["total"] += 1
                if result.mol.GetNumBonds() == expected["num_bonds"]:
                    metrics["bonds"]["correct"] += 1

            # Aromatic
            if "has_aromatic" in expected:
                metrics["aromatic"]["total"] += 1
                has_aromatic = any(a.GetIsAromatic() for a in result.mol.GetAtoms())
                if has_aromatic == expected["has_aromatic"]:
                    metrics["aromatic"]["correct"] += 1

            # Fragments
            if "num_fragments" in expected:
                metrics["fragments"]["total"] += 1
                frags = Chem.GetMolFrags(result.mol, asMols=False, sanitizeFrags=False)
                if len(frags) == expected["num_fragments"]:
                    metrics["fragments"]["correct"] += 1

            # Stereocenters
            if "num_stereocenters" in expected:
                metrics["stereocenters"]["total"] += 1
                centers = Chem.FindMolChiralCenters(
                    result.mol, includeUnassigned=True, useLegacyImplementation=False
                )
                if len(centers) == expected["num_stereocenters"]:
                    metrics["stereocenters"]["correct"] += 1

            # InChI
            if "inchi_generatable" in expected:
                metrics["inchi"]["total"] += 1
                try:
                    inchi = Chem.MolToInchi(result.mol)
                    can_generate = bool(inchi)
                except Exception:
                    can_generate = False
                if can_generate == expected["inchi_generatable"]:
                    metrics["inchi"]["correct"] += 1

        # Calculate overall accuracy
        total_correct = sum(m["correct"] for m in metrics.values())
        total_tests = sum(m["total"] for m in metrics.values())
        overall_accuracy = (total_correct / total_tests * 100) if total_tests > 0 else 0

        # Print detailed breakdown
        print("\n=== Accuracy Report ===")
        for name, m in metrics.items():
            if m["total"] > 0:
                acc = m["correct"] / m["total"] * 100
                print(f"  {name}: {acc:.1f}% ({m['correct']}/{m['total']})")
        print(f"  OVERALL: {overall_accuracy:.1f}% ({total_correct}/{total_tests})")

        assert (
            overall_accuracy >= 99.0
        ), f"Overall accuracy {overall_accuracy:.1f}% below 99% target"
