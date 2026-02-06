"""
SDF Exporter

Exports batch results to SDF format using RDKit SDWriter.
"""

import logging
from io import BytesIO, StringIO
from typing import Any, Dict, List

from rdkit import Chem

from .base import BaseExporter, ExporterFactory, ExportFormat, extract_alert_names

logger = logging.getLogger(__name__)


class SDFExporter(BaseExporter):
    """Export batch results to SDF format with properties."""

    def export(self, results: List[Dict[str, Any]]) -> BytesIO:
        """
        Export results to SDF format.

        Args:
            results: List of batch result dictionaries

        Returns:
            BytesIO buffer containing SDF data
        """
        # Use StringIO for text-based SDF output
        string_buffer = StringIO()

        # Create SDWriter
        writer = Chem.SDWriter(string_buffer)

        skipped_count = 0

        for idx, result in enumerate(results):
            # Get SMILES - prefer standardized, fallback to canonical, then original
            validation = result.get("validation") or {}
            smiles = (
                result.get("standardized_smiles")
                or validation.get("canonical_smiles")
                or result.get("smiles")  # Fallback to original input SMILES
            )

            if not smiles:
                logger.warning(f"Skipping molecule at index {idx}: no valid SMILES")
                skipped_count += 1
                continue

            # Create mol object
            try:
                mol = Chem.MolFromSmiles(smiles)
                if mol is None:
                    logger.warning(
                        f"Skipping molecule at index {idx}: invalid SMILES '{smiles}'"
                    )
                    skipped_count += 1
                    continue
            except Exception as e:
                logger.warning(
                    f"Skipping molecule at index {idx}: error parsing SMILES: {e}"
                )
                skipped_count += 1
                continue

            # Set molecule properties
            mol.SetProp("_Name", result.get("name", f"mol_{idx + 1}"))

            # Add scores
            overall_score = validation.get("overall_score", 0)
            mol.SetProp("overall_score", str(overall_score))

            scoring = result.get("scoring") or {}
            if scoring:
                ml_score = scoring.get("ml_readiness_score", 0)
                np_score = scoring.get("np_likeness_score", 0)
                mol.SetProp("ml_readiness_score", str(ml_score))
                mol.SetProp("np_likeness_score", f"{np_score:.2f}")

            # Add InChIKey
            inchikey = validation.get("inchikey", "")
            if inchikey:
                mol.SetProp("inchikey", inchikey)

            # Add alerts (comma-separated)
            alert_names = extract_alert_names(result.get("alerts") or {})
            if alert_names:
                mol.SetProp("alerts", ", ".join(alert_names))

            # Write molecule
            try:
                writer.write(mol)
            except Exception as e:
                logger.warning(f"Failed to write molecule at index {idx}: {e}")
                skipped_count += 1

        # Close writer
        writer.close()

        if skipped_count > 0:
            logger.info(f"Skipped {skipped_count} molecules with invalid SMILES")

        # Convert to BytesIO
        bytes_buffer = BytesIO()
        bytes_buffer.write(string_buffer.getvalue().encode("utf-8"))
        bytes_buffer.seek(0)

        return bytes_buffer

    @property
    def media_type(self) -> str:
        """MIME type for SDF."""
        return "chemical/x-mdl-sdfile"

    @property
    def file_extension(self) -> str:
        """File extension for SDF."""
        return "sdf"


# Register with factory
ExporterFactory.register(ExportFormat.SDF, SDFExporter)
