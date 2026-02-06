"""
ChEMBL integration client.

ChEMBL is a manually curated database of bioactive molecules with
drug-like properties, maintained by EMBL-EBI.
https://www.ebi.ac.uk/chembl/

This client provides bioactivity data lookup functionality.
"""

from typing import List, Optional

import httpx
from rdkit import Chem

from app.core.config import settings
from app.schemas.integrations import BioactivityData, ChEMBLRequest, ChEMBLResult


class ChEMBLClient:
    """
    Client for ChEMBL REST API.

    Provides molecule search and bioactivity data retrieval.
    """

    def __init__(self):
        self.base_url = settings.CHEMBL_API_URL
        self.timeout = settings.EXTERNAL_API_TIMEOUT

    async def search_by_smiles(self, smiles: str) -> Optional[str]:
        """
        Search ChEMBL by SMILES string (structure similarity search).

        Args:
            smiles: SMILES string to search

        Returns:
            ChEMBL molecule ID if found, None otherwise
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/molecule.json",
                    params={"molecule_structures__canonical_smiles__flexmatch": smiles},
                )
                response.raise_for_status()

                data = response.json()
                molecules = data.get("molecules", [])
                if molecules and len(molecules) > 0:
                    return molecules[0].get("molecule_chembl_id")
                return None

        except (httpx.HTTPError, KeyError, ValueError, IndexError):
            # External API failure - return None gracefully
            return None

    async def get_molecule_by_inchikey(self, inchikey: str) -> Optional[dict]:
        """
        Get molecule by InChIKey.

        Args:
            inchikey: InChIKey to search

        Returns:
            Molecule data dict if found, None otherwise
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/molecule.json",
                    params={"molecule_structures__standard_inchi_key": inchikey},
                )
                response.raise_for_status()

                data = response.json()
                molecules = data.get("molecules", [])
                if molecules and len(molecules) > 0:
                    return molecules[0]
                return None

        except (httpx.HTTPError, KeyError, ValueError, IndexError):
            # External API failure - return None gracefully
            return None

    async def get_molecule(self, chembl_id: str) -> Optional[dict]:
        """
        Get molecule by ChEMBL ID.

        Args:
            chembl_id: ChEMBL molecule ID

        Returns:
            Molecule data dict if found, None otherwise
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/molecule/{chembl_id}.json",
                )
                response.raise_for_status()

                return response.json()

        except (httpx.HTTPError, KeyError, ValueError):
            # External API failure - return None gracefully
            return None

    async def get_bioactivities(self, chembl_id: str, limit: int = 50) -> List[dict]:
        """
        Get bioactivity data for a molecule.

        Args:
            chembl_id: ChEMBL molecule ID
            limit: Maximum number of bioactivities to return

        Returns:
            List of bioactivity records
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/activity.json",
                    params={
                        "molecule_chembl_id": chembl_id,
                        "limit": limit,
                    },
                )
                response.raise_for_status()

                data = response.json()
                return data.get("activities", [])

        except (httpx.HTTPError, KeyError, ValueError):
            # External API failure - return empty list gracefully
            return []


async def get_bioactivity(request: ChEMBLRequest) -> ChEMBLResult:
    """
    Get molecule and bioactivity information from ChEMBL.

    Searches by InChIKey first (more specific), falls back to SMILES.

    Args:
        request: ChEMBL lookup request with SMILES or InChIKey

    Returns:
        Molecule and bioactivity information if found
    """
    client = ChEMBLClient()
    molecule_data = None

    # Try InChIKey first (most specific)
    if request.inchikey:
        molecule_data = await client.get_molecule_by_inchikey(request.inchikey)

    # Try SMILES if InChIKey failed
    if molecule_data is None and request.smiles:
        # Generate InChIKey from SMILES for more reliable search
        try:
            mol = Chem.MolFromSmiles(request.smiles)
            if mol:
                inchikey = Chem.MolToInchiKey(mol)
                molecule_data = await client.get_molecule_by_inchikey(inchikey)

                # Fallback to SMILES search
                if molecule_data is None:
                    chembl_id = await client.search_by_smiles(request.smiles)
                    if chembl_id:
                        molecule_data = await client.get_molecule(chembl_id)
        except Exception:
            # RDKit error - try direct SMILES search anyway
            chembl_id = await client.search_by_smiles(request.smiles)
            if chembl_id:
                molecule_data = await client.get_molecule(chembl_id)

    # Not found
    if molecule_data is None:
        return ChEMBLResult(found=False)

    chembl_id = molecule_data.get("molecule_chembl_id")

    # Get bioactivity data
    bioactivities_raw = await client.get_bioactivities(chembl_id, limit=50)

    # Parse bioactivities
    bioactivities = []
    for activity in bioactivities_raw:
        # Skip activities without target or activity type
        if not activity.get("target_chembl_id") or not activity.get("standard_type"):
            continue

        bioactivities.append(
            BioactivityData(
                target_chembl_id=activity.get("target_chembl_id"),
                target_name=activity.get("target_pref_name"),
                target_type=activity.get("target_organism"),
                activity_type=activity.get("standard_type"),
                activity_value=activity.get("standard_value"),
                activity_unit=activity.get("standard_units"),
                assay_chembl_id=activity.get("assay_chembl_id"),
                document_chembl_id=activity.get("document_chembl_id"),
            )
        )

    return ChEMBLResult(
        found=True,
        chembl_id=chembl_id,
        pref_name=molecule_data.get("pref_name"),
        molecule_type=molecule_data.get("molecule_type"),
        max_phase=molecule_data.get("max_phase"),
        molecular_formula=molecule_data.get("molecule_properties", {}).get(
            "full_molecular_formula"
        ),
        molecular_weight=molecule_data.get("molecule_properties", {}).get(
            "molecular_weight"
        ),
        bioactivities=bioactivities,
        bioactivity_count=len(bioactivities),
        url=(
            f"https://www.ebi.ac.uk/chembl/compound_report_card/{chembl_id}"
            if chembl_id
            else None
        ),
    )
