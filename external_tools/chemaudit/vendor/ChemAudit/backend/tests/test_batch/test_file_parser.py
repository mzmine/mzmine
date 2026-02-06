"""
Tests for batch file parsing (SDF and CSV).
"""

import pytest

from app.services.batch.file_parser import (
    detect_csv_columns,
    parse_csv,
    parse_sdf,
)


class TestParseSDF:
    """Tests for SDF file parsing."""

    def test_parse_valid_sdf(self):
        """Test parsing a valid SDF with multiple molecules."""
        # Create a simple SDF content with 2 molecules
        sdf_content = b"""
     RDKit          3D

  3  2  0  0  0  0  0  0  0  0999 V2000
    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
    1.5000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
    3.0000    0.0000    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0
  1  2  1  0
  2  3  1  0
M  END
> <_Name>
Ethanol

$$$$

     RDKit          3D

  2  1  0  0  0  0  0  0  0  0999 V2000
    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
    1.5000    0.0000    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0
  1  2  1  0
M  END
> <_Name>
Methanol

$$$$
"""
        molecules = parse_sdf(sdf_content)

        assert len(molecules) == 2
        assert molecules[0].name == "Ethanol"
        assert molecules[0].smiles != ""
        assert molecules[0].parse_error is None
        assert molecules[1].name == "Methanol"
        assert molecules[1].smiles != ""

    def test_parse_sdf_with_invalid_entry(self):
        """Test that invalid molecules don't crash the entire batch."""
        # SDF with one valid and one that will fail to parse
        sdf_content = b"""
     RDKit          3D

  2  1  0  0  0  0  0  0  0  0999 V2000
    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
    1.5000    0.0000    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0
  1  2  1  0
M  END
> <_Name>
Methanol

$$$$
INVALID MOLECULE DATA
$$$$
"""
        molecules = parse_sdf(sdf_content)

        # Should get at least the valid molecule
        assert len(molecules) >= 1
        valid_mols = [m for m in molecules if m.parse_error is None]
        assert len(valid_mols) >= 1
        assert valid_mols[0].name == "Methanol"

    def test_parse_sdf_generates_index_names(self):
        """Test that molecules without names get index-based names."""
        sdf_content = b"""
     RDKit          3D

  2  1  0  0  0  0  0  0  0  0999 V2000
    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
    1.5000    0.0000    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0
  1  2  1  0
M  END
$$$$
"""
        molecules = parse_sdf(sdf_content)

        assert len(molecules) == 1
        assert molecules[0].name == "mol_0"
        assert molecules[0].index == 0


class TestParseCSV:
    """Tests for CSV file parsing."""

    def test_parse_valid_csv_with_smiles_column(self):
        """Test parsing CSV with standard SMILES column."""
        csv_content = b"""SMILES,Name,MolWeight
CCO,Ethanol,46.07
C,Methane,16.04
CC(=O)O,AceticAcid,60.05
"""
        molecules = parse_csv(csv_content)

        assert len(molecules) == 3
        assert molecules[0].smiles == "CCO"
        assert molecules[0].name == "Ethanol"
        # Note: CSV parser only reads SMILES and Name columns for efficiency
        # Properties are not populated from CSV files
        assert molecules[0].properties == {}

    def test_parse_csv_with_custom_smiles_column(self):
        """Test parsing CSV with non-standard SMILES column name."""
        csv_content = b"""compound_smiles,compound_id
CCO,ETH001
C,MTH001
"""
        molecules = parse_csv(csv_content, smiles_column="compound_smiles")

        assert len(molecules) == 2
        assert molecules[0].smiles == "CCO"

    def test_parse_csv_case_insensitive_column(self):
        """Test that SMILES column detection is case-insensitive."""
        csv_content = b"""smiles,name
CCO,Ethanol
"""
        molecules = parse_csv(csv_content, smiles_column="SMILES")

        assert len(molecules) == 1
        assert molecules[0].smiles == "CCO"

    def test_parse_csv_missing_smiles_column_raises(self):
        """Test that missing SMILES column raises ValueError."""
        csv_content = b"""Name,MolWeight
Ethanol,46.07
"""
        with pytest.raises(ValueError) as exc_info:
            parse_csv(csv_content, smiles_column="SMILES")

        assert "SMILES column 'SMILES' not found" in str(exc_info.value)

    def test_parse_csv_handles_empty_smiles(self):
        """Test that empty SMILES values create error entries."""
        csv_content = b"""SMILES,Name
CCO,Ethanol
,Empty
"""
        molecules = parse_csv(csv_content)

        assert len(molecules) == 2
        assert molecules[0].smiles == "CCO"
        assert molecules[0].parse_error is None
        # Empty SMILES should have a parse error
        assert molecules[1].parse_error is not None
        assert (
            "Empty" in molecules[1].parse_error
            or "invalid" in molecules[1].parse_error.lower()
        )

    def test_parse_csv_auto_detect_name_column(self):
        """Test auto-detection of name column."""
        csv_content = b"""SMILES,ID
CCO,ETH001
"""
        molecules = parse_csv(csv_content)

        assert molecules[0].name == "ETH001"

    def test_parse_tsv_content(self):
        """Test parsing tab-separated content (TSV format)."""
        tsv_content = b"""SMILES\tName\tMolWeight
CCO\tEthanol\t46.07
C\tMethane\t16.04
CC(=O)O\tAceticAcid\t60.05
"""
        molecules = parse_csv(tsv_content)

        assert len(molecules) == 3
        assert molecules[0].smiles == "CCO"
        assert molecules[0].name == "Ethanol"
        assert molecules[1].smiles == "C"
        assert molecules[1].name == "Methane"

    def test_parse_mixed_delimiter_preference(self):
        """Test that pandas correctly handles tab-separated data."""
        # Tab-separated with no commas in data
        tsv_content = b"""SMILES\tID
CCO\tETH001
C\tMTH001
"""
        molecules = parse_csv(tsv_content)

        assert len(molecules) == 2
        assert molecules[0].smiles == "CCO"
        assert molecules[0].name == "ETH001"


class TestDetectCSVColumns:
    """Tests for CSV column detection."""

    def test_detect_columns_returns_all_columns(self):
        """Test that all columns are returned."""
        csv_content = b"""SMILES,Name,MW,LogP
CCO,Ethanol,46.07,-0.31
"""
        result = detect_csv_columns(csv_content)

        assert "columns" in result
        assert "SMILES" in result["columns"]
        assert "Name" in result["columns"]
        assert "MW" in result["columns"]

    def test_detect_columns_suggests_smiles(self):
        """Test that SMILES column is suggested."""
        csv_content = b"""compound_smiles,name
CCO,Ethanol
"""
        result = detect_csv_columns(csv_content)

        assert result["suggested_smiles"] == "compound_smiles"

    def test_detect_columns_row_count(self):
        """Test row count estimation."""
        csv_content = b"""SMILES,Name
CCO,Ethanol
C,Methane
CC,Ethane
"""
        result = detect_csv_columns(csv_content)

        assert result["row_count_estimate"] == 3
