"""
COCONUT (COlleCtion of Open Natural ProdUcTs) database integration client.

COCONUT is a comprehensive database of natural products with >400,000 entries.
https://coconut.naturalproducts.net/

This client uses COCONUT API v2 which requires authentication.
"""

from typing import Optional

import httpx
from rdkit import Chem

from app.core.config import settings
from app.schemas.integrations import COCONUTRequest, COCONUTResult


class COCONUTClient:
    """
    Client for COCONUT natural products database API v2.

    Requires API token for authentication.
    """

    def __init__(self):
        self.base_url = settings.COCONUT_API_URL
        self.token = settings.COCONUT_API_TOKEN
        self.timeout = settings.EXTERNAL_API_TIMEOUT

    def _get_headers(self) -> dict:
        """Get headers with authentication."""
        return {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        }

    async def search_by_inchikey(self, inchikey: str) -> Optional[dict]:
        """
        Search COCONUT by InChIKey using API v2.

        Args:
            inchikey: InChIKey to search

        Returns:
            Compound data dict if found, None otherwise
        """
        if not self.token:
            return None

        try:
            payload = {
                "search": {
                    "filters": [
                        {
                            "field": "standard_inchi_key",
                            "operator": "=",
                            "value": inchikey,
                        }
                    ],
                    "page": 1,
                    "limit": 10,
                }
            }

            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/molecules/search",
                    headers=self._get_headers(),
                    json=payload,
                )
                response.raise_for_status()

                data = response.json()
                if data and data.get("data") and len(data["data"]) > 0:
                    return data["data"][0]
                return None

        except (httpx.HTTPError, KeyError, ValueError, IndexError):
            return None

    async def search_by_smiles(self, smiles: str) -> Optional[dict]:
        """
        Search COCONUT by canonical SMILES using API v2.

        Args:
            smiles: SMILES string to search

        Returns:
            Compound data dict if found, None otherwise
        """
        if not self.token:
            return None

        try:
            payload = {
                "search": {
                    "filters": [
                        {"field": "canonical_smiles", "operator": "=", "value": smiles}
                    ],
                    "page": 1,
                    "limit": 10,
                }
            }

            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/molecules/search",
                    headers=self._get_headers(),
                    json=payload,
                )
                response.raise_for_status()

                data = response.json()
                if data and data.get("data") and len(data["data"]) > 0:
                    return data["data"][0]
                return None

        except (httpx.HTTPError, KeyError, ValueError, IndexError):
            return None


async def lookup_natural_product(request: COCONUTRequest) -> COCONUTResult:
    """
    Look up molecule in COCONUT natural products database.

    Searches by InChIKey first (more specific), falls back to SMILES.

    Args:
        request: COCONUT lookup request with SMILES or InChIKey

    Returns:
        Natural product information if found
    """
    client = COCONUTClient()

    # Check if API token is configured
    if not client.token:
        return COCONUTResult(found=False)

    # Try InChIKey first (most specific)
    if request.inchikey:
        data = await client.search_by_inchikey(request.inchikey)
        if data:
            return _parse_coconut_result(data, found=True)

    # Try SMILES - generate InChIKey for more reliable search
    if request.smiles:
        try:
            mol = Chem.MolFromSmiles(request.smiles)
            if mol:
                inchikey = Chem.MolToInchiKey(mol)
                data = await client.search_by_inchikey(inchikey)
                if data:
                    return _parse_coconut_result(data, found=True)

                # Fallback to canonical SMILES search
                canonical = Chem.MolToSmiles(mol, canonical=True)
                data = await client.search_by_smiles(canonical)
                if data:
                    return _parse_coconut_result(data, found=True)
        except Exception:
            pass

    # Not found
    return COCONUTResult(found=False)


def _parse_coconut_result(data: dict, found: bool) -> COCONUTResult:
    """Parse COCONUT API v2 response into result schema."""
    coconut_id = data.get("identifier")

    return COCONUTResult(
        found=found,
        coconut_id=coconut_id,
        name=data.get("name") or data.get("iupac_name"),
        smiles=data.get("canonical_smiles"),
        inchikey=data.get("standard_inchi_key"),
        molecular_formula=data.get("molecular_formula"),
        molecular_weight=data.get("molecular_weight"),
        organism=data.get("organism") or data.get("biological_source"),
        organism_type=data.get("organism_type"),
        nplikeness=data.get("np_likeness_score"),
        url=(
            f"https://coconut.naturalproducts.net/compounds/{coconut_id}"
            if coconut_id
            else None
        ),
    )
