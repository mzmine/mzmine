"""
Molecule Parser with Defensive Sanitization Pattern

CRITICAL: Always use sanitize=False then explicit SanitizeMol() with error catching.
This prevents silent failures and allows precise error detection.
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import Optional

from rdkit import Chem


class MoleculeFormat(str, Enum):
    """Supported molecule input formats"""

    SMILES = "smiles"
    INCHI = "inchi"
    MOL = "mol"
    UNKNOWN = "unknown"


@dataclass
class ParseResult:
    """Result of molecule parsing operation"""

    success: bool
    mol: Optional[Chem.Mol] = None
    canonical_smiles: Optional[str] = None
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    format_detected: MoleculeFormat = MoleculeFormat.UNKNOWN


def detect_format(input_string: str) -> MoleculeFormat:
    """
    Auto-detect molecule format from input string.

    Args:
        input_string: Raw molecule input

    Returns:
        Detected format enum
    """
    if not input_string or not isinstance(input_string, str):
        return MoleculeFormat.UNKNOWN

    input_clean = input_string.strip()

    # InChI format detection
    if input_clean.startswith("InChI="):
        return MoleculeFormat.INCHI

    # MOL format detection (has "M  END" marker)
    if "M  END" in input_clean or "\n" in input_clean[:50]:
        return MoleculeFormat.MOL

    # Default to SMILES
    return MoleculeFormat.SMILES


def parse_molecule(
    input_string: str, input_format: Optional[MoleculeFormat] = None
) -> ParseResult:
    """
    Parse molecule with defensive sanitization pattern.

    CRITICAL PATTERN:
    1. Parse WITHOUT sanitization (sanitize=False)
    2. Use DetectChemistryProblems() to find issues
    3. Attempt explicit SanitizeMol() with error catching
    4. AssignStereochemistry explicitly
    5. Generate canonical SMILES

    Args:
        input_string: Molecule input (SMILES, InChI, MOL block)
        input_format: Format hint (auto-detected if None)

    Returns:
        ParseResult with molecule and diagnostic information
    """
    result = ParseResult(success=False)

    if not input_string or not isinstance(input_string, str):
        result.errors.append("Empty or invalid input")
        return result

    input_clean = input_string.strip()

    # Auto-detect format if not provided
    if input_format is None:
        input_format = detect_format(input_clean)

    result.format_detected = input_format

    # Step 1: Parse WITHOUT sanitization
    mol = None
    try:
        if input_format == MoleculeFormat.SMILES:
            mol = Chem.MolFromSmiles(input_clean, sanitize=False)
        elif input_format == MoleculeFormat.INCHI:
            mol = Chem.MolFromInchi(input_clean, sanitize=False)
        elif input_format == MoleculeFormat.MOL:
            mol = Chem.MolFromMolBlock(input_clean, sanitize=False)
        else:
            result.errors.append(f"Unsupported format: {input_format}")
            return result
    except Exception as e:
        result.errors.append(f"Parse error: {str(e)}")
        return result

    if mol is None:
        result.errors.append(f"Failed to parse {input_format.value} input")
        return result

    # Step 2: Detect chemistry problems (before sanitization)
    problems = Chem.DetectChemistryProblems(mol)
    if problems:
        for problem in problems:
            result.warnings.append(f"{problem.GetType()}: {problem.Message()}")

    # Step 3: Attempt explicit sanitization with error catching
    sanitize_errors = []
    sanitize_flags = Chem.SanitizeFlags.SANITIZE_ALL

    try:
        Chem.SanitizeMol(mol, sanitizeOps=sanitize_flags, catchErrors=True)
    except Exception as e:
        sanitize_errors.append(str(e))

    # Check if sanitization introduced any issues
    san_fail = Chem.SanitizeMol(mol, catchErrors=True)
    if san_fail != Chem.SanitizeFlags.SANITIZE_NONE:
        # Extract which specific sanitization operations failed
        if san_fail & Chem.SanitizeFlags.SANITIZE_PROPERTIES:
            result.errors.append("Failed to sanitize properties")
        if san_fail & Chem.SanitizeFlags.SANITIZE_FINDRADICALS:
            result.errors.append("Failed to identify radicals")
        if san_fail & Chem.SanitizeFlags.SANITIZE_KEKULIZE:
            result.errors.append("Failed to kekulize aromatic rings")
        if san_fail & Chem.SanitizeFlags.SANITIZE_SETAROMATICITY:
            result.errors.append("Failed to set aromaticity")
        if san_fail & Chem.SanitizeFlags.SANITIZE_SETCONJUGATION:
            result.errors.append("Failed to set conjugation")
        if san_fail & Chem.SanitizeFlags.SANITIZE_SETHYBRIDIZATION:
            result.errors.append("Failed to set hybridization")
        if san_fail & Chem.SanitizeFlags.SANITIZE_SYMMRINGS:
            result.errors.append("Failed to symmetrize rings")
        if san_fail & Chem.SanitizeFlags.SANITIZE_CLEANUP:
            result.errors.append("Failed cleanup operations")

    # If sanitization completely failed, molecule is invalid
    if result.errors:
        result.success = False
        result.mol = mol  # Return unsanitized mol for inspection
        return result

    # Step 4: Assign stereochemistry explicitly
    try:
        Chem.AssignStereochemistry(mol, cleanIt=True, force=True)
    except Exception as e:
        result.warnings.append(f"Stereochemistry assignment issue: {str(e)}")

    # Step 5: Generate canonical SMILES
    try:
        canonical = Chem.MolToSmiles(mol, canonical=True)
        result.canonical_smiles = canonical
    except Exception as e:
        result.errors.append(f"Failed to generate canonical SMILES: {str(e)}")
        result.success = False
        return result

    # Success!
    result.success = True
    result.mol = mol

    return result
