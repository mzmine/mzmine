"""
File Parser Module

Parses SDF and CSV files into molecule data for batch processing.
Handles errors per-molecule without crashing the entire batch.
Includes security validations for production use.
"""

import io
import logging
import re
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Tuple

import pandas as pd
from rdkit import Chem

logger = logging.getLogger(__name__)

# =============================================================================
# File Content Type Validation
# =============================================================================

# Suspicious patterns that should never appear in chemical data files
SUSPICIOUS_PATTERNS = [
    rb"<script",  # JavaScript injection
    rb"javascript:",  # JavaScript protocol
    rb"<iframe",  # Iframe injection
    rb"onerror\s*=",  # Event handlers
    rb"onclick\s*=",  # Event handlers
    rb"onload\s*=",  # Event handlers
    rb"<object",  # Object tags
    rb"<embed",  # Embed tags
    rb"data:text/html",  # Data URI HTML
    rb"\x00",  # Null bytes (binary injection)
]

# SDF file markers
SDF_MARKERS = [
    b"M  END",  # End of molecule connection table
    b"$$$$",  # End of molecule record
    b"V2000",  # V2000 molfile format
    b"V3000",  # V3000 molfile format
]

# CSV-like content patterns
CSV_PATTERNS = [
    rb"^[^,\t]+[,\t][^,\t]+",  # Has delimiters
]


def validate_file_content_type(
    content: bytes,
    expected_type: str,
    filename: str = "",
) -> Tuple[bool, Optional[str]]:
    """
    Validate that file content matches the expected type.

    Performs magic byte / content analysis to detect:
    - Mismatched file types (e.g., executable disguised as CSV)
    - Malicious content injection attempts
    - Binary files masquerading as text

    Args:
        content: Raw file bytes
        expected_type: Expected file type ('sdf' or 'csv')
        filename: Original filename for logging

    Returns:
        Tuple of (is_valid, error_message)
        - (True, None) if content matches expected type
        - (False, error_message) if validation fails
    """
    if not content:
        return False, "Empty file"

    # Check for suspicious patterns in first 100KB
    sample = content[:102400]

    for pattern in SUSPICIOUS_PATTERNS:
        if re.search(pattern, sample, re.IGNORECASE):
            logger.warning(
                f"Suspicious pattern detected in file '{filename}': {pattern}"
            )
            return False, "File contains potentially malicious content"

    # Check file magic bytes / signatures
    # Common executable/binary signatures that should never be in chemical files
    dangerous_signatures = [
        (b"MZ", "Windows executable"),  # PE/EXE
        (b"\x7fELF", "Linux executable"),  # ELF
        (b"PK\x03\x04", "ZIP archive"),  # ZIP (could be docx, xlsx, etc)
        (b"%PDF", "PDF document"),  # PDF
        (b"\xd0\xcf\x11\xe0", "OLE document"),  # DOC, XLS (old Office)
        (b"Rar!", "RAR archive"),  # RAR
        (b"\x1f\x8b", "GZIP compressed"),  # GZIP
        (b"BZh", "BZIP2 compressed"),  # BZIP2
    ]

    for sig, desc in dangerous_signatures:
        if content.startswith(sig):
            logger.warning(f"Invalid file type for '{filename}': detected {desc}")
            return False, f"Invalid file type: {desc} files are not allowed"

    # Validate based on expected type
    if expected_type.lower() == "sdf":
        return _validate_sdf_content(content, filename)
    elif expected_type.lower() == "csv":
        return _validate_csv_content(content, filename)
    else:
        return False, f"Unknown file type: {expected_type}"


def _validate_sdf_content(content: bytes, filename: str) -> Tuple[bool, Optional[str]]:
    """
    Validate that content looks like a valid SDF file.

    Args:
        content: Raw file bytes
        filename: Original filename

    Returns:
        Tuple of (is_valid, error_message)
    """
    # Check for SDF markers in the file
    found_markers = []
    for marker in SDF_MARKERS:
        if marker in content:
            found_markers.append(marker)

    # A valid SDF should have at least M END or $$$$
    if not any(marker in found_markers for marker in [b"M  END", b"$$$$"]):
        logger.warning(f"File '{filename}' does not contain SDF markers")
        return (
            False,
            "File does not appear to be a valid SDF file (missing molecule delimiters)",
        )

    # Check that content is primarily text-based
    # SDF files should be mostly printable ASCII
    sample = content[:10000]
    non_printable = sum(
        1
        for b in sample
        if b < 32 and b not in (9, 10, 13)  # Allow tab, newline, carriage return
    )
    if non_printable > len(sample) * 0.05:  # More than 5% non-printable
        logger.warning(f"File '{filename}' contains excessive non-printable characters")
        return False, "File contains too many non-printable characters for an SDF file"

    return True, None


def _validate_csv_content(content: bytes, filename: str) -> Tuple[bool, Optional[str]]:
    """
    Validate that content looks like a valid CSV file.

    Args:
        content: Raw file bytes
        filename: Original filename

    Returns:
        Tuple of (is_valid, error_message)
    """
    # Check that content is primarily text-based
    sample = content[:10000]

    # Count non-printable characters (excluding common whitespace)
    non_printable = sum(
        1
        for b in sample
        if b < 32 and b not in (9, 10, 13)  # Allow tab, newline, carriage return
    )
    if non_printable > len(sample) * 0.01:  # More than 1% non-printable
        logger.warning(f"File '{filename}' contains non-printable characters")
        return False, "File contains too many non-printable characters for a CSV file"

    # Try to decode as UTF-8 or Latin-1
    try:
        text = sample.decode("utf-8")
    except UnicodeDecodeError:
        try:
            text = sample.decode("latin-1")
        except UnicodeDecodeError:
            return (
                False,
                "File is not valid text (could not decode as UTF-8 or Latin-1)",
            )

    # Check for common CSV structure (has lines and delimiters)
    lines = text.split("\n")
    if len(lines) < 2:
        return False, "CSV file must have at least a header row and one data row"

    # Check that first line (header) has comma or tab delimiters
    header = lines[0]
    if "," not in header and "\t" not in header:
        return False, "CSV file must use comma or tab delimiters"

    return True, None


def detect_suspicious_content(content: bytes) -> List[str]:
    """
    Scan content for suspicious patterns and return list of findings.

    This is useful for logging/auditing rather than blocking.

    Args:
        content: Raw file bytes

    Returns:
        List of suspicious pattern descriptions found
    """
    findings = []
    sample = content[:102400]

    patterns_with_names = [
        (rb"<script", "JavaScript tag"),
        (rb"javascript:", "JavaScript protocol"),
        (rb"<iframe", "Iframe tag"),
        (rb"onerror\s*=", "onerror event handler"),
        (rb"onclick\s*=", "onclick event handler"),
        (rb"onload\s*=", "onload event handler"),
        (rb"<object", "Object tag"),
        (rb"<embed", "Embed tag"),
        (rb"data:text/html", "Data URI with HTML"),
        (rb"\x00", "Null byte"),
    ]

    for pattern, name in patterns_with_names:
        if re.search(pattern, sample, re.IGNORECASE):
            findings.append(name)

    return findings


# Security limits
MAX_COLUMN_NAME_LENGTH = 256
MAX_PROPERTY_VALUE_LENGTH = 10000
MAX_SMILES_LENGTH = 5000
FORBIDDEN_COLUMN_PATTERNS = [
    r'[<>"\'\\/;`]',  # Characters that could be used in injection attacks
]


@dataclass
class MoleculeData:
    """Data extracted from a molecule in a batch file."""

    smiles: str
    name: Optional[str] = None
    index: int = 0
    properties: Dict[str, Any] = field(default_factory=dict)
    parse_error: Optional[str] = None


def _sanitize_string(value: str, max_length: int = MAX_PROPERTY_VALUE_LENGTH) -> str:
    """Sanitize a string value by truncating and removing control characters."""
    if not isinstance(value, str):
        value = str(value)
    # Remove control characters except newlines and tabs
    value = re.sub(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]", "", value)
    # Truncate to max length
    return value[:max_length]


def _validate_column_name(name: str) -> bool:
    """Validate that a column name is safe to use."""
    if not name or len(name) > MAX_COLUMN_NAME_LENGTH:
        return False
    for pattern in FORBIDDEN_COLUMN_PATTERNS:
        if re.search(pattern, name):
            return False
    return True


def _detect_delimiter(content: bytes) -> str:
    """
    Detect the delimiter used in a delimited text file.

    Examines the first line (header) to determine if the file uses
    tabs or commas as delimiters.

    Args:
        content: Raw file bytes

    Returns:
        Delimiter character ('\\t' for tab, ',' for comma)
    """
    # Get the first line
    try:
        # Try UTF-8 first
        text = content.decode("utf-8")
    except UnicodeDecodeError:
        try:
            text = content.decode("latin-1")
        except UnicodeDecodeError:
            return ","  # Default to comma

    first_line = text.split("\n")[0] if "\n" in text else text

    # Count delimiters in the first line
    tab_count = first_line.count("\t")
    comma_count = first_line.count(",")

    # Use the more common delimiter, preferring tab if equal
    # (since tabs are less likely to appear in data)
    if tab_count > 0 and tab_count >= comma_count:
        return "\t"
    return ","


def _validate_smiles_format(smiles: str) -> Tuple[bool, Optional[str]]:
    """
    Basic validation of SMILES string format before RDKit parsing.

    Returns:
        Tuple of (is_valid, error_message)
    """
    if not smiles or not isinstance(smiles, str):
        return False, "Empty or invalid SMILES"

    smiles = smiles.strip()

    if len(smiles) > MAX_SMILES_LENGTH:
        return False, f"SMILES too long (>{MAX_SMILES_LENGTH} chars)"

    # Check for obviously invalid characters (basic sanity check)
    # SMILES can contain: letters, numbers, @, #, =, -, +, [, ], (, ), /, \, ., %
    if re.search(r'[<>"\';`]', smiles):
        return False, "SMILES contains invalid characters"

    return True, None


def parse_sdf(file_content: bytes, max_file_size_mb: int = 500) -> List[MoleculeData]:
    """
    Parse an SDF file into a list of molecule data.

    Args:
        file_content: Raw bytes of the SDF file
        max_file_size_mb: Maximum file size in MB

    Returns:
        List of MoleculeData objects (includes entries with parse_error for invalid molecules)

    Raises:
        ValueError: If file exceeds size limit

    Note:
        Individual parse errors are captured per-molecule rather than failing the entire batch.
    """
    # Security: Check file size
    file_size_mb = len(file_content) / (1024 * 1024)
    if file_size_mb > max_file_size_mb:
        raise ValueError(
            f"File too large: {file_size_mb:.1f}MB exceeds limit of {max_file_size_mb}MB"
        )

    molecules = []
    supplier = Chem.ForwardSDMolSupplier(io.BytesIO(file_content))

    for idx, mol in enumerate(supplier):
        if mol is None:
            # Failed to parse this molecule
            molecules.append(
                MoleculeData(
                    smiles="",
                    name=f"mol_{idx}",
                    index=idx,
                    parse_error=f"Failed to parse molecule at index {idx}",
                )
            )
            continue

        try:
            # Get SMILES
            smiles = Chem.MolToSmiles(mol)

            # Extract name from _Name property or generate index-based
            name = mol.GetProp("_Name") if mol.HasProp("_Name") else f"mol_{idx}"
            if not name.strip():
                name = f"mol_{idx}"
            name = _sanitize_string(name, max_length=256)

            # Extract all other properties with sanitization
            properties = {}
            for prop_name in mol.GetPropsAsDict():
                if prop_name != "_Name":
                    try:
                        prop_value = mol.GetProp(prop_name)
                        # Sanitize property name and value
                        safe_name = _sanitize_string(
                            prop_name, max_length=MAX_COLUMN_NAME_LENGTH
                        )
                        if _validate_column_name(safe_name):
                            properties[safe_name] = _sanitize_string(prop_value)
                    except Exception:
                        pass

            molecules.append(
                MoleculeData(
                    smiles=smiles,
                    name=name,
                    index=idx,
                    properties=properties,
                )
            )
        except Exception as e:
            molecules.append(
                MoleculeData(
                    smiles="",
                    name=f"mol_{idx}",
                    index=idx,
                    parse_error=f"Error extracting data: {str(e)[:200]}",
                )
            )

    return molecules


def parse_csv(
    file_content: bytes,
    smiles_column: str = "SMILES",
    name_column: Optional[str] = None,
    max_file_size_mb: int = 500,
) -> List[MoleculeData]:
    """
    Parse a CSV/TSV/TXT file into a list of molecule data.

    Automatically detects whether the file uses comma or tab delimiters.

    Args:
        file_content: Raw bytes of the delimited text file
        smiles_column: Name of the column containing SMILES strings
        name_column: Optional name of the column containing molecule names
        max_file_size_mb: Maximum file size in MB

    Returns:
        List of MoleculeData objects

    Raises:
        ValueError: If the SMILES column is not found or file exceeds size limit

    Note:
        Individual parse errors are captured per-molecule.
    """
    # Security: Check file size
    file_size_mb = len(file_content) / (1024 * 1024)
    if file_size_mb > max_file_size_mb:
        raise ValueError(
            f"File too large: {file_size_mb:.1f}MB exceeds limit of {max_file_size_mb}MB"
        )

    # Detect delimiter (comma or tab)
    delimiter = _detect_delimiter(file_content)

    # First, read just the header to find column names
    try:
        df_header = pd.read_csv(io.BytesIO(file_content), nrows=0, sep=delimiter)
        all_columns = df_header.columns.tolist()
    except Exception as e:
        raise ValueError(f"Failed to parse CSV file: {str(e)[:200]}")

    # Security: Validate column count
    if len(all_columns) > 1000:
        raise ValueError("Too many columns in CSV (maximum 1000)")

    # Check for SMILES column (case-insensitive search)
    smiles_col_actual = None
    for col in all_columns:
        if col.lower() == smiles_column.lower():
            smiles_col_actual = col
            break

    if smiles_col_actual is None:
        raise ValueError(f"SMILES column '{smiles_column}' not found in CSV")

    # Determine name column
    name_col_actual = None
    if name_column:
        for col in all_columns:
            if col.lower() == name_column.lower():
                name_col_actual = col
                break
    else:
        # Try to auto-detect name column
        for candidate in [
            "Name",
            "name",
            "NAME",
            "ID",
            "id",
            "Compound",
            "compound",
            "Molecule",
            "molecule",
            "Title",
            "title",
        ]:
            if candidate in all_columns:
                name_col_actual = candidate
                break

    # Only read the columns we need (much faster for files with many columns)
    cols_to_read = [smiles_col_actual]
    if name_col_actual:
        cols_to_read.append(name_col_actual)

    try:
        df = pd.read_csv(
            io.BytesIO(file_content),
            usecols=cols_to_read,
            dtype=str,
            na_filter=False,
            sep=delimiter,
        )
    except Exception as e:
        raise ValueError(f"Failed to parse CSV file: {str(e)[:200]}")

    molecules = []
    for idx, row in df.iterrows():
        smiles = str(row[smiles_col_actual]).strip()

        # Validate SMILES format
        is_valid, error_msg = _validate_smiles_format(smiles)
        if not is_valid:
            molecules.append(
                MoleculeData(
                    smiles="",
                    name=f"row_{idx}",
                    index=idx,
                    parse_error=error_msg or "Invalid SMILES",
                )
            )
            continue

        # Get name with sanitization
        if name_col_actual and row[name_col_actual]:
            name = _sanitize_string(str(row[name_col_actual]).strip(), max_length=256)
        else:
            name = f"row_{idx}"

        molecules.append(
            MoleculeData(
                smiles=smiles,
                name=name,
                index=idx,
                properties={},  # No extra properties needed for batch processing
            )
        )

    return molecules


def detect_csv_columns(
    file_content: bytes,
    max_file_size_mb: int = 500,
) -> Dict[str, Any]:
    """
    Detect column names in a delimited text file for user selection.

    Automatically detects whether the file uses comma or tab delimiters.

    Args:
        file_content: Raw bytes of the delimited text file
        max_file_size_mb: Maximum file size in MB

    Returns:
        Dictionary with column info and suggestions

    Raises:
        ValueError: If file cannot be read or exceeds size limit
    """
    # Security: Check file size
    file_size_mb = len(file_content) / (1024 * 1024)
    if file_size_mb > max_file_size_mb:
        raise ValueError(
            f"File too large: {file_size_mb:.1f}MB exceeds limit of {max_file_size_mb}MB"
        )

    # Detect delimiter (comma or tab)
    delimiter = _detect_delimiter(file_content)

    try:
        # Read only first few rows for column detection
        df_preview = pd.read_csv(
            io.BytesIO(file_content), nrows=5, dtype=str, sep=delimiter
        )
        columns = [
            col for col in df_preview.columns.tolist() if _validate_column_name(col)
        ]

        if not columns:
            raise ValueError("No valid columns found in CSV")

        # Try to detect SMILES column
        suggested_smiles = None
        smiles_keywords = [
            "smiles",
            "smi",
            "canonical_smiles",
            "isomeric_smiles",
            "structure",
        ]
        for col in columns:
            col_lower = col.lower()
            for keyword in smiles_keywords:
                if keyword in col_lower:
                    suggested_smiles = col
                    break
            if suggested_smiles:
                break

        # Try to detect Name/ID column
        suggested_name = None
        name_keywords = ["name", "id", "compound", "molecule", "title", "identifier"]
        for col in columns:
            col_lower = col.lower()
            # Skip if it's the SMILES column
            if col == suggested_smiles:
                continue
            for keyword in name_keywords:
                if keyword in col_lower:
                    suggested_name = col
                    break
            if suggested_name:
                break

        # Get sample values for each column (first non-empty value)
        column_samples = {}
        for col in columns[:20]:  # Limit to first 20 columns for preview
            for val in df_preview[col]:
                if val and str(val).strip():
                    column_samples[col] = _sanitize_string(str(val), max_length=100)
                    break

        # Estimate row count from file size (fast, no full file read)
        # For column detection, an estimate is sufficient
        # Average row size varies, but ~200 bytes per row is reasonable for CSV with SMILES
        if file_size_mb < 1:
            # For small files, do a quick count
            try:
                line_count = file_content.count(b"\n")
                row_count = max(1, line_count - 1)  # Subtract header
            except Exception:
                row_count = max(1, int(file_size_mb * 5000))
        else:
            # For larger files, estimate based on sample
            try:
                # Sample first 100KB to estimate average line length
                sample_size = min(102400, len(file_content))
                sample = file_content[:sample_size]
                lines_in_sample = sample.count(b"\n")
                if lines_in_sample > 1:
                    avg_line_length = sample_size / lines_in_sample
                    row_count = max(1, int(len(file_content) / avg_line_length) - 1)
                else:
                    row_count = max(1, int(file_size_mb * 5000))
            except Exception:
                row_count = max(1, int(file_size_mb * 5000))

        return {
            "columns": columns,
            "suggested_smiles": suggested_smiles,
            "suggested_name": suggested_name,
            "column_samples": column_samples,
            "row_count_estimate": row_count,
            "file_size_mb": round(file_size_mb, 2),
        }
    except ValueError:
        raise
    except Exception as e:
        logger.error(f"Error detecting CSV columns: {e}")
        raise ValueError(f"Failed to read CSV file: {str(e)[:100]}")
