"""
Pytest fixtures for performance tests.
"""

from typing import List, Tuple

import pytest
from rdkit import Chem

# Test molecule set covering diverse complexities
TEST_MOLECULES: List[Tuple[str, str]] = [
    # Simple molecules (fast)
    ("ethanol", "CCO"),
    ("benzene", "c1ccccc1"),
    ("acetic_acid", "CC(=O)O"),
    ("pyridine", "c1ccncc1"),
    # Drug-like molecules (medium)
    ("aspirin", "CC(=O)OC1=CC=CC=C1C(=O)O"),
    ("caffeine", "CN1C=NC2=C1C(=O)N(C)C(=O)N2C"),
    ("ibuprofen", "CC(C)CC1=CC=C(C=C1)C(C)C(=O)O"),
    ("metformin", "CN(C)C(=N)NC(=N)N"),
    # Complex molecules (slow)
    (
        "atorvastatin",
        "CC(C)C1=C(C(=C(N1CCC(CC(CC(=O)O)O)O)C2=CC=C(C=C2)F)C3=CC=CC=C3)C(=O)NC4=CC=CC=C4",
    ),
    ("morphine", "CN1CC[C@]23C4=C5C=CC(=C4O[C@H]2[C@@H](C=C[C@H]3[C@H]1C5)O)O"),
    (
        "cholesterol",
        "CC(C)CCC[C@@H](C)[C@H]1CC[C@@H]2[C@@]1(CC[C@H]3[C@H]2CC=C4[C@@]3(CC[C@@H](C4)O)C)C",
    ),
    ("quercetin", "OC1=CC(=C2C(=O)C(=C(OC2=C1)C3=CC=C(O)C(=C3)O)O)O"),
    ("sildenafil", "CCCC1=NN(C2=C1NC(=NC2=O)C3=C(C=CC(=C3)S(=O)(=O)N4CCN(CC4)C)OCC)C"),
]


@pytest.fixture(scope="session")
def test_molecules() -> List[Tuple[str, Chem.Mol]]:
    """
    Pre-parsed test molecules for benchmarking.

    Returns:
        List of (name, mol) tuples
    """
    molecules = []
    for name, smiles in TEST_MOLECULES:
        mol = Chem.MolFromSmiles(smiles)
        if mol:
            molecules.append((name, mol))
    return molecules


@pytest.fixture(scope="session")
def simple_molecules() -> List[Tuple[str, Chem.Mol]]:
    """Simple molecules for fast benchmarks."""
    molecules = []
    for name, smiles in TEST_MOLECULES[:4]:
        mol = Chem.MolFromSmiles(smiles)
        if mol:
            molecules.append((name, mol))
    return molecules


@pytest.fixture(scope="session")
def complex_molecules() -> List[Tuple[str, Chem.Mol]]:
    """Complex molecules for stress testing."""
    molecules = []
    for name, smiles in TEST_MOLECULES[8:]:
        mol = Chem.MolFromSmiles(smiles)
        if mol:
            molecules.append((name, mol))
    return molecules


@pytest.fixture(scope="session")
def warmed_filter_catalogs():
    """Pre-warm filter catalogs to avoid cold start penalty."""
    from app.services.alerts.filter_catalog import get_filter_catalog

    # Load all catalogs
    for catalog in ["PAINS", "BRENK", "NIH", "ZINC"]:
        try:
            get_filter_catalog(catalog)
        except Exception:
            pass  # Some catalogs may not be available

    return True
