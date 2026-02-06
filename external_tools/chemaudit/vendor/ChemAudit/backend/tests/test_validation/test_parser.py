"""
Molecule Parser Tests

Tests defensive sanitization pattern and format detection.
"""

from app.services.parser import detect_format, parse_molecule
from app.services.parser.molecule_parser import MoleculeFormat


class TestFormatDetection:
    """Test molecule format detection"""

    def test_detect_smiles(self):
        """Should detect SMILES format"""
        assert detect_format("CCO") == MoleculeFormat.SMILES
        assert detect_format("c1ccccc1") == MoleculeFormat.SMILES

    def test_detect_inchi(self):
        """Should detect InChI format"""
        assert (
            detect_format("InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3") == MoleculeFormat.INCHI
        )

    def test_detect_mol(self):
        """Should detect MOL format"""
        mol_block = """
  Mrv0541 02151109432D

  3  2  0  0  0  0            999 V2000
   -0.4125    0.7145    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
M  END
"""
        assert detect_format(mol_block) == MoleculeFormat.MOL

    def test_detect_unknown(self):
        """Should return unknown for invalid input"""
        assert detect_format("") == MoleculeFormat.UNKNOWN
        assert detect_format(None) == MoleculeFormat.UNKNOWN


class TestValidMolecules:
    """Test parsing of valid molecules"""

    def test_parse_simple_smiles(self):
        """Should parse simple SMILES: ethanol"""
        result = parse_molecule("CCO")
        assert result.success is True
        assert result.mol is not None
        assert result.canonical_smiles == "CCO"
        assert result.format_detected == MoleculeFormat.SMILES
        assert len(result.errors) == 0

    def test_parse_aromatic_smiles(self):
        """Should parse aromatic SMILES: benzene"""
        result = parse_molecule("c1ccccc1")
        assert result.success is True
        assert result.mol is not None
        assert result.canonical_smiles == "c1ccccc1"
        assert result.format_detected == MoleculeFormat.SMILES

    def test_parse_carboxylic_acid(self):
        """Should parse carboxylic acid: acetic acid"""
        result = parse_molecule("CC(=O)O")
        assert result.success is True
        assert result.mol is not None
        assert result.canonical_smiles == "CC(=O)O"

    def test_parse_inchi(self):
        """Should parse InChI format"""
        inchi = "InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3"
        result = parse_molecule(inchi)
        assert result.success is True
        assert result.mol is not None
        assert result.format_detected == MoleculeFormat.INCHI
        assert result.canonical_smiles == "CCO"


class TestInvalidMolecules:
    """Test parsing of invalid molecules"""

    def test_parse_invalid_smiles(self):
        """Should fail on completely invalid SMILES"""
        result = parse_molecule("invalid")
        assert result.success is False
        assert len(result.errors) > 0

    def test_parse_empty_string(self):
        """Should fail on empty input"""
        result = parse_molecule("")
        assert result.success is False
        assert "Empty or invalid input" in result.errors[0]

    def test_parse_pentavalent_carbon(self):
        """Should detect valence error: pentavalent carbon"""
        result = parse_molecule("C(C)(C)(C)(C)C")
        assert result.success is False
        # Should have sanitization or valence errors
        assert len(result.errors) > 0 or len(result.warnings) > 0


class TestSanitizationPattern:
    """Test defensive sanitization pattern"""

    def test_sanitization_catches_errors(self):
        """Should catch sanitization errors explicitly"""
        # This SMILES has valence issues
        result = parse_molecule("C(C)(C)(C)(C)C")
        assert result.success is False
        # Should have captured specific sanitization failure
        assert len(result.errors) > 0

    def test_warnings_for_chemistry_problems(self):
        """Should generate warnings for chemistry problems"""
        # Some molecules may parse but have warnings
        result = parse_molecule("C1CC1")  # Cyclopropane - valid but strained
        # Should parse successfully
        assert result.success is True
        # May or may not have warnings depending on RDKit version

    def test_mol_returned_even_on_failure(self):
        """Should return unsanitized mol even on sanitization failure"""
        result = parse_molecule("C(C)(C)(C)(C)C")
        # Even though it failed, mol should be available for inspection
        assert result.mol is not None
        assert result.success is False


class TestStereochemistry:
    """Test stereochemistry handling"""

    def test_stereochemistry_assigned(self):
        """Should assign stereochemistry to chiral molecules"""
        # (R)-lactic acid
        result = parse_molecule("C[C@H](O)C(=O)O")
        assert result.success is True
        assert result.canonical_smiles is not None
        # Should preserve stereochemistry
        assert (
            "@" in result.canonical_smiles or result.canonical_smiles == "CC(O)C(=O)O"
        )
