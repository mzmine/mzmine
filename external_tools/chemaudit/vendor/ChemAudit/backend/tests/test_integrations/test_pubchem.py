"""
Tests for PubChem integration client.
"""

from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest

from app.schemas.integrations import PubChemRequest
from app.services.integrations.pubchem import PubChemClient, get_compound_info


class TestPubChemClient:
    """Tests for PubChemClient."""

    @pytest.mark.asyncio
    async def test_search_by_smiles_success(self):
        """Test successful search by SMILES."""
        client = PubChemClient()
        mock_response = MagicMock()
        mock_response.json.return_value = {"IdentifierList": {"CID": [702]}}
        mock_response.raise_for_status = MagicMock()

        with patch("httpx.AsyncClient") as mock_client:
            mock_client.return_value.__aenter__.return_value.post = AsyncMock(
                return_value=mock_response
            )

            result = await client.search_by_smiles("CCO")

            assert result == 702

    @pytest.mark.asyncio
    async def test_search_by_inchikey_success(self):
        """Test successful search by InChIKey."""
        client = PubChemClient()
        mock_response = MagicMock()
        mock_response.json.return_value = {"IdentifierList": {"CID": [702]}}
        mock_response.raise_for_status = MagicMock()

        with patch("httpx.AsyncClient") as mock_client:
            mock_client.return_value.__aenter__.return_value.get = AsyncMock(
                return_value=mock_response
            )

            result = await client.search_by_inchikey("LFQSCWFLJHTTHZ-UHFFFAOYSA-N")

            assert result == 702

    @pytest.mark.asyncio
    async def test_get_compound_properties_success(self):
        """Test successful retrieval of compound properties."""
        client = PubChemClient()
        mock_response = MagicMock()
        mock_response.json.return_value = {
            "PropertyTable": {
                "Properties": [
                    {
                        "CID": 702,
                        "MolecularFormula": "C2H6O",
                        "MolecularWeight": 46.07,
                        "IUPACName": "ethanol",
                    }
                ]
            }
        }
        mock_response.raise_for_status = MagicMock()

        with patch("httpx.AsyncClient") as mock_client:
            mock_client.return_value.__aenter__.return_value.get = AsyncMock(
                return_value=mock_response
            )

            result = await client.get_compound_properties(702)

            assert result is not None
            assert result["MolecularFormula"] == "C2H6O"

    @pytest.mark.asyncio
    async def test_get_synonyms_success(self):
        """Test successful retrieval of synonyms."""
        client = PubChemClient()
        mock_response = MagicMock()
        mock_response.json.return_value = {
            "InformationList": {
                "Information": [{"Synonym": ["ethanol", "ethyl alcohol", "alcohol"]}]
            }
        }
        mock_response.raise_for_status = MagicMock()

        with patch("httpx.AsyncClient") as mock_client:
            mock_client.return_value.__aenter__.return_value.get = AsyncMock(
                return_value=mock_response
            )

            result = await client.get_synonyms(702, max_synonyms=3)

            assert len(result) == 3
            assert "ethanol" in result

    @pytest.mark.asyncio
    async def test_api_failure_returns_none(self):
        """Test API failure returns None gracefully."""
        client = PubChemClient()
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = httpx.HTTPStatusError(
            "API error", request=MagicMock(), response=MagicMock()
        )

        with patch("httpx.AsyncClient") as mock_client:
            mock_client.return_value.__aenter__.return_value.get = AsyncMock(
                return_value=mock_response
            )

            result = await client.search_by_inchikey("INVALID")

            assert result is None


class TestGetCompoundInfo:
    """Tests for get_compound_info function."""

    @pytest.mark.asyncio
    async def test_get_compound_info_by_inchikey(self):
        """Test getting compound info by InChIKey."""
        mock_properties = {
            "CID": 702,
            "MolecularFormula": "C2H6O",
            "MolecularWeight": 46.07,
            "IUPACName": "ethanol",
        }

        with patch(
            "app.services.integrations.pubchem.PubChemClient"
        ) as mock_client_class:
            mock_client = AsyncMock()
            mock_client.search_by_inchikey = AsyncMock(return_value=702)
            mock_client.get_compound_properties = AsyncMock(
                return_value=mock_properties
            )
            mock_client.get_synonyms = AsyncMock(return_value=["ethanol"])
            mock_client_class.return_value = mock_client

            request = PubChemRequest(inchikey="LFQSCWFLJHTTHZ-UHFFFAOYSA-N")
            result = await get_compound_info(request)

            assert result.found is True
            assert result.cid == 702
            assert result.molecular_formula == "C2H6O"

    @pytest.mark.asyncio
    async def test_get_compound_info_not_found(self):
        """Test getting compound info when not found."""
        with patch(
            "app.services.integrations.pubchem.PubChemClient"
        ) as mock_client_class:
            mock_client = AsyncMock()
            mock_client.search_by_inchikey = AsyncMock(return_value=None)
            mock_client.search_by_smiles = AsyncMock(return_value=None)
            mock_client_class.return_value = mock_client

            request = PubChemRequest(smiles="INVALID")
            result = await get_compound_info(request)

            assert result.found is False
            assert result.cid is None
