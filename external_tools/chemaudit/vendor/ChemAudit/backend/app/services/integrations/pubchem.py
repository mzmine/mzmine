"""
PubChem integration client.

PubChem is a public database of chemical compounds and their properties,
maintained by the NIH National Library of Medicine.
https://pubchem.ncbi.nlm.nih.gov/

This client provides compound lookup and cross-reference functionality.
"""

from typing import List, Optional

import httpx
from rdkit import Chem

from app.core.config import settings
from app.schemas.integrations import PubChemRequest, PubChemResult


class PubChemClient:
    """
    Client for PubChem PUG REST API.

    Provides search and compound information retrieval.
    """

    def __init__(self):
        self.base_url = settings.PUBCHEM_API_URL
        self.timeout = settings.EXTERNAL_API_TIMEOUT

    async def search_by_smiles(self, smiles: str) -> Optional[int]:
        """
        Search PubChem by SMILES string.

        Args:
            smiles: SMILES string to search

        Returns:
            PubChem CID (Compound ID) if found, None otherwise
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/compound/smiles/cids/JSON",
                    data={"smiles": smiles},
                )
                response.raise_for_status()

                data = response.json()
                cids = data.get("IdentifierList", {}).get("CID", [])
                if cids and len(cids) > 0:
                    return cids[0]  # Return first match
                return None

        except (httpx.HTTPError, KeyError, ValueError, IndexError):
            # External API failure - return None gracefully
            return None

    async def search_by_inchikey(self, inchikey: str) -> Optional[int]:
        """
        Search PubChem by InChIKey.

        Args:
            inchikey: InChIKey to search

        Returns:
            PubChem CID if found, None otherwise
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/compound/inchikey/{inchikey}/cids/JSON",
                )
                response.raise_for_status()

                data = response.json()
                cids = data.get("IdentifierList", {}).get("CID", [])
                if cids and len(cids) > 0:
                    return cids[0]  # Return first match
                return None

        except (httpx.HTTPError, KeyError, ValueError, IndexError):
            # External API failure - return None gracefully
            return None

    async def get_compound_properties(self, cid: int) -> Optional[dict]:
        """
        Get compound properties by CID.

        Args:
            cid: PubChem Compound ID

        Returns:
            Compound properties dict if found, None otherwise
        """
        try:
            properties = "MolecularFormula,MolecularWeight,CanonicalSMILES,InChI,InChIKey,IUPACName"

            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/compound/cid/{cid}/property/{properties}/JSON",
                )
                response.raise_for_status()

                data = response.json()
                properties_list = data.get("PropertyTable", {}).get("Properties", [])
                if properties_list and len(properties_list) > 0:
                    return properties_list[0]
                return None

        except (httpx.HTTPError, KeyError, ValueError, IndexError):
            # External API failure - return None gracefully
            return None

    async def get_synonyms(self, cid: int, max_synonyms: int = 10) -> List[str]:
        """
        Get compound synonyms by CID.

        Args:
            cid: PubChem Compound ID
            max_synonyms: Maximum number of synonyms to return

        Returns:
            List of synonyms (up to max_synonyms)
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/compound/cid/{cid}/synonyms/JSON",
                )
                response.raise_for_status()

                data = response.json()
                synonyms = (
                    data.get("InformationList", {})
                    .get("Information", [{}])[0]
                    .get("Synonym", [])
                )
                return synonyms[:max_synonyms]

        except (httpx.HTTPError, KeyError, ValueError, IndexError):
            # External API failure - return empty list gracefully
            return []


async def get_compound_info(request: PubChemRequest) -> PubChemResult:
    """
    Get compound information from PubChem.

    Searches by InChIKey first (more specific), falls back to SMILES.

    Args:
        request: PubChem lookup request with SMILES or InChIKey

    Returns:
        Compound information if found
    """
    client = PubChemClient()
    cid = None

    # Try InChIKey first (most specific)
    if request.inchikey:
        cid = await client.search_by_inchikey(request.inchikey)

    # Try SMILES if InChIKey failed
    if cid is None and request.smiles:
        # Generate InChIKey from SMILES for more reliable search
        try:
            mol = Chem.MolFromSmiles(request.smiles)
            if mol:
                inchikey = Chem.MolToInchiKey(mol)
                cid = await client.search_by_inchikey(inchikey)

                # Fallback to SMILES search
                if cid is None:
                    cid = await client.search_by_smiles(request.smiles)
        except Exception:
            # RDKit error - try direct SMILES search anyway
            cid = await client.search_by_smiles(request.smiles)

    # Not found
    if cid is None:
        return PubChemResult(found=False)

    # Get compound properties
    properties = await client.get_compound_properties(cid)
    if properties is None:
        return PubChemResult(found=False)

    # Get synonyms
    synonyms = await client.get_synonyms(cid, max_synonyms=10)

    return PubChemResult(
        found=True,
        cid=cid,
        iupac_name=properties.get("IUPACName"),
        molecular_formula=properties.get("MolecularFormula"),
        molecular_weight=properties.get("MolecularWeight"),
        canonical_smiles=properties.get("CanonicalSMILES"),
        inchi=properties.get("InChI"),
        inchikey=properties.get("InChIKey"),
        synonyms=synonyms if synonyms else None,
        url=f"https://pubchem.ncbi.nlm.nih.gov/compound/{cid}",
    )
