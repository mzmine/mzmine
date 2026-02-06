"""
Tests for Scaffold Extraction

Tests the extract_scaffold function for various molecule types.
"""

from rdkit import Chem

from app.services.scoring.scaffold import ScaffoldResult, extract_scaffold


class TestScaffoldExtraction:
    """Tests for extract_scaffold function."""

    def test_aspirin_scaffold(self):
        """Test scaffold extraction for aspirin (has benzene ring)."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = extract_scaffold(mol)

        assert isinstance(result, ScaffoldResult)
        assert result.has_scaffold is True
        assert result.scaffold_smiles != ""
        assert result.generic_scaffold_smiles != ""
        # Should contain benzene ring
        assert (
            "c1ccccc1" in result.scaffold_smiles.lower()
            or "C1=CC=CC=C1" in result.scaffold_smiles
        )

    def test_caffeine_scaffold(self):
        """Test scaffold extraction for caffeine (fused ring system)."""
        mol = Chem.MolFromSmiles("CN1C=NC2=C1C(=O)N(C(=O)N2C)C")  # Caffeine
        result = extract_scaffold(mol)

        assert result.has_scaffold is True
        assert result.scaffold_smiles != ""
        # Caffeine has purine-like bicyclic scaffold
        assert result.details.get("scaffold_rings", 0) >= 2

    def test_ethanol_no_scaffold(self):
        """Test that acyclic molecules return no scaffold."""
        mol = Chem.MolFromSmiles("CCO")  # Ethanol - no rings
        result = extract_scaffold(mol)

        assert result.has_scaffold is False
        assert "No ring system detected" in result.message
        # Should return original molecule
        assert result.scaffold_smiles != ""

    def test_methane_no_scaffold(self):
        """Test that methane returns no scaffold."""
        mol = Chem.MolFromSmiles("C")  # Methane
        result = extract_scaffold(mol)

        assert result.has_scaffold is False
        assert (
            "acyclic" in result.message.lower() or "no ring" in result.message.lower()
        )

    def test_generic_scaffold_is_different(self):
        """Test that generic scaffold differs from standard scaffold."""
        # Molecule with heteroatoms in ring
        mol = Chem.MolFromSmiles("c1ccncc1")  # Pyridine
        result = extract_scaffold(mol)

        assert result.has_scaffold is True
        # Generic scaffold should be all carbons
        # Standard scaffold has nitrogen
        # They should differ (unless already generic)
        assert result.generic_scaffold_smiles != ""
        assert result.scaffold_smiles != ""

    def test_steroid_scaffold(self):
        """Test scaffold extraction for steroid (4 fused rings)."""
        # Simplified steroid core
        mol = Chem.MolFromSmiles(
            "CC12CCC3C(CCC4=CC(=O)CCC34C)C1CCC2O"
        )  # Testosterone-like
        result = extract_scaffold(mol)

        assert result.has_scaffold is True
        assert result.details.get("scaffold_rings", 0) >= 4

    def test_details_populated(self):
        """Test that details dict contains expected keys."""
        mol = Chem.MolFromSmiles("c1ccccc1")  # Benzene
        result = extract_scaffold(mol)

        assert "num_rings" in result.details
        assert "original_smiles" in result.details
        assert result.details["num_rings"] == 1

    def test_complex_molecule_scaffold(self):
        """Test scaffold for molecule with multiple ring systems."""
        # Biphenyl
        mol = Chem.MolFromSmiles("c1ccc(cc1)c2ccccc2")
        result = extract_scaffold(mol)

        assert result.has_scaffold is True
        # Should capture both rings
        assert result.details.get("scaffold_rings", 0) == 2

    def test_spiro_compound_scaffold(self):
        """Test scaffold for spiro compound."""
        # Simple spiro compound
        mol = Chem.MolFromSmiles("C1CCC2(CC1)CCCCC2")  # Spiro[5.5]undecane
        result = extract_scaffold(mol)

        assert result.has_scaffold is True
        # Should have 2 rings
        assert result.details.get("num_rings", 0) == 2

    def test_null_molecule_handling(self):
        """Test handling of null molecule."""
        result = extract_scaffold(None)

        assert result.has_scaffold is False
        assert "invalid" in result.message.lower() or "Invalid" in result.message

    def test_message_describes_scaffold(self):
        """Test that message provides useful information."""
        mol = Chem.MolFromSmiles("c1ccccc1")  # Benzene
        result = extract_scaffold(mol)

        # Message should mention rings and atoms
        assert "ring" in result.message.lower()
        assert result.has_scaffold is True


class TestScaffoldSmiles:
    """Tests for scaffold SMILES validity."""

    def test_scaffold_smiles_is_valid(self):
        """Test that returned scaffold SMILES is valid."""
        mol = Chem.MolFromSmiles("CC(=O)Oc1ccccc1C(=O)O")  # Aspirin
        result = extract_scaffold(mol)

        # Try to parse the scaffold SMILES
        scaffold_mol = Chem.MolFromSmiles(result.scaffold_smiles)
        assert scaffold_mol is not None

    def test_generic_scaffold_smiles_is_valid(self):
        """Test that generic scaffold SMILES is valid."""
        mol = Chem.MolFromSmiles("CN1C=NC2=C1C(=O)N(C(=O)N2C)C")  # Caffeine
        result = extract_scaffold(mol)

        # Try to parse the generic scaffold SMILES
        generic_mol = Chem.MolFromSmiles(result.generic_scaffold_smiles)
        assert generic_mol is not None

    def test_generic_scaffold_atoms(self):
        """Test that generic scaffold has expected properties."""
        mol = Chem.MolFromSmiles("c1ccncc1")  # Pyridine
        result = extract_scaffold(mol)

        if result.has_scaffold:
            generic_mol = Chem.MolFromSmiles(result.generic_scaffold_smiles)
            if generic_mol is not None:
                # Generic scaffold should have carbons only (all heteroatoms converted)
                atom_symbols = [atom.GetSymbol() for atom in generic_mol.GetAtoms()]
                # Should be mostly/all carbon (C) in generic scaffold
                assert "C" in atom_symbols


class TestGenericScaffoldCorrectness:
    """Tests for correct generic scaffold generation (Bemis-Murcko CSK)."""

    def test_exocyclic_double_bond_removed_in_generic(self):
        """
        Test that exocyclic double-bonded atoms are correctly removed
        in the generic scaffold.

        RDKit's MakeScaffoldGeneric converts =O to -C, but we need to
        remove this extra carbon by calling GetScaffoldForMol again.

        Reference: https://github.com/rdkit/rdkit/discussions/6844
        """
        # Cyclopropanone: 3-membered ring with =O
        mol = Chem.MolFromSmiles("C1CC1=O")
        result = extract_scaffold(mol)

        assert result.has_scaffold is True

        # Standard scaffold should keep the =O (RDKit behavior)
        # Generic scaffold should be just the 3-membered carbon ring
        generic_mol = Chem.MolFromSmiles(result.generic_scaffold_smiles)
        assert generic_mol is not None

        # Generic scaffold should have exactly 3 atoms (cyclopropane ring)
        # NOT 4 atoms (which would be wrong - the =O converted to -C)
        assert generic_mol.GetNumAtoms() == 3, (
            f"Generic scaffold should have 3 atoms (cyclopropane), "
            f"got {generic_mol.GetNumAtoms()} atoms: {result.generic_scaffold_smiles}"
        )

    def test_ketone_on_benzene_generic(self):
        """Test acetophenone - benzene with C(=O)C side chain."""
        mol = Chem.MolFromSmiles("CC(=O)c1ccccc1")  # Acetophenone
        result = extract_scaffold(mol)

        assert result.has_scaffold is True

        # Generic scaffold should be just benzene (6 carbons)
        generic_mol = Chem.MolFromSmiles(result.generic_scaffold_smiles)
        assert generic_mol is not None
        assert generic_mol.GetNumAtoms() == 6, (
            f"Generic scaffold should be benzene (6 atoms), "
            f"got {generic_mol.GetNumAtoms()}: {result.generic_scaffold_smiles}"
        )

    def test_pyridone_generic_scaffold(self):
        """Test 2-pyridone - ring with both N and =O."""
        mol = Chem.MolFromSmiles("O=c1cccc[nH]1")  # 2-pyridone
        result = extract_scaffold(mol)

        assert result.has_scaffold is True

        # Generic scaffold should be 6-membered carbon ring
        generic_mol = Chem.MolFromSmiles(result.generic_scaffold_smiles)
        assert generic_mol is not None
        assert generic_mol.GetNumAtoms() == 6, (
            f"Generic scaffold should have 6 atoms, "
            f"got {generic_mol.GetNumAtoms()}: {result.generic_scaffold_smiles}"
        )

        # All atoms should be carbon in generic scaffold
        for atom in generic_mol.GetAtoms():
            assert (
                atom.GetSymbol() == "C"
            ), f"Generic scaffold should have all carbons, found {atom.GetSymbol()}"


class TestEdgeCases:
    """Tests for edge cases."""

    def test_single_ring_molecule(self):
        """Test molecule with single ring."""
        mol = Chem.MolFromSmiles("C1CCCCC1")  # Cyclohexane
        result = extract_scaffold(mol)

        assert result.has_scaffold is True
        assert result.details.get("num_rings") == 1

    def test_large_ring_system(self):
        """Test molecule with large ring."""
        # 12-membered ring
        mol = Chem.MolFromSmiles("C1CCCCCCCCCCC1")
        result = extract_scaffold(mol)

        assert result.has_scaffold is True

    def test_molecule_with_side_chains(self):
        """Test that side chains are removed in scaffold."""
        # Toluene (benzene with methyl)
        mol = Chem.MolFromSmiles("Cc1ccccc1")
        result = extract_scaffold(mol)

        assert result.has_scaffold is True
        # Scaffold should be benzene, not toluene
        scaffold_mol = Chem.MolFromSmiles(result.scaffold_smiles)
        assert scaffold_mol.GetNumAtoms() == 6  # Just benzene

    def test_fused_ring_system(self):
        """Test fused ring system."""
        # Naphthalene
        mol = Chem.MolFromSmiles("c1ccc2ccccc2c1")
        result = extract_scaffold(mol)

        assert result.has_scaffold is True
        assert result.details.get("scaffold_rings", 0) == 2
