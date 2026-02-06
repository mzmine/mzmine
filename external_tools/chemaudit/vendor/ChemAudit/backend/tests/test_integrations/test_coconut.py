"""
Tests for COCONUT integration client.
"""

from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest

from app.schemas.integrations import COCONUTRequest
from app.services.integrations.coconut import COCONUTClient, lookup_natural_product


class TestCOCONUTClient:
    """Tests for COCONUTClient."""

    @pytest.mark.asyncio
    async def test_search_by_smiles_success(self):
        """Test successful search by SMILES."""
        mock_response = MagicMock()
        # API v2 returns {"data": [...]} structure
        mock_response.json.return_value = {
            "data": [
                {
                    "identifier": "CNP0123456",
                    "name": "Caffeine",
                    "canonical_smiles": "CN1C=NC2=C1C(=O)N(C(=O)N2C)C",
                }
            ]
        }
        mock_response.raise_for_status = MagicMock()

        with patch("app.services.integrations.coconut.settings") as mock_settings:
            mock_settings.COCONUT_API_URL = "https://api.coconut.test"
            mock_settings.COCONUT_API_TOKEN = "test_token"
            mock_settings.EXTERNAL_API_TIMEOUT = 30.0

            with patch("httpx.AsyncClient") as mock_client:
                # COCONUT uses POST for search
                mock_client.return_value.__aenter__.return_value.post = AsyncMock(
                    return_value=mock_response
                )

                client = COCONUTClient()
                result = await client.search_by_smiles("CN1C=NC2=C1C(=O)N(C(=O)N2C)C")

                assert result is not None
                assert result["identifier"] == "CNP0123456"

    @pytest.mark.asyncio
    async def test_search_by_smiles_not_found(self):
        """Test search by SMILES not found."""
        mock_response = MagicMock()
        mock_response.json.return_value = {"data": []}
        mock_response.raise_for_status = MagicMock()

        with patch("app.services.integrations.coconut.settings") as mock_settings:
            mock_settings.COCONUT_API_URL = "https://api.coconut.test"
            mock_settings.COCONUT_API_TOKEN = "test_token"
            mock_settings.EXTERNAL_API_TIMEOUT = 30.0

            with patch("httpx.AsyncClient") as mock_client:
                mock_client.return_value.__aenter__.return_value.post = AsyncMock(
                    return_value=mock_response
                )

                client = COCONUTClient()
                result = await client.search_by_smiles("CCO")

                assert result is None

    @pytest.mark.asyncio
    async def test_search_by_inchikey_success(self):
        """Test successful search by InChIKey."""
        mock_response = MagicMock()
        # API v2 returns {"data": [...]} structure
        mock_response.json.return_value = {
            "data": [
                {
                    "identifier": "CNP0123456",
                    "standard_inchi_key": "RYYVLZVUVIJVGH-UHFFFAOYSA-N",
                }
            ]
        }
        mock_response.raise_for_status = MagicMock()

        with patch("app.services.integrations.coconut.settings") as mock_settings:
            mock_settings.COCONUT_API_URL = "https://api.coconut.test"
            mock_settings.COCONUT_API_TOKEN = "test_token"
            mock_settings.EXTERNAL_API_TIMEOUT = 30.0

            with patch("httpx.AsyncClient") as mock_client:
                mock_client.return_value.__aenter__.return_value.post = AsyncMock(
                    return_value=mock_response
                )

                client = COCONUTClient()
                result = await client.search_by_inchikey("RYYVLZVUVIJVGH-UHFFFAOYSA-N")

                assert result is not None
                assert result["identifier"] == "CNP0123456"

    @pytest.mark.asyncio
    async def test_search_api_failure(self):
        """Test API failure returns None gracefully."""
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = httpx.HTTPStatusError(
            "API error", request=MagicMock(), response=MagicMock()
        )

        with patch("app.services.integrations.coconut.settings") as mock_settings:
            mock_settings.COCONUT_API_URL = "https://api.coconut.test"
            mock_settings.COCONUT_API_TOKEN = "test_token"
            mock_settings.EXTERNAL_API_TIMEOUT = 30.0

            with patch("httpx.AsyncClient") as mock_client:
                mock_client.return_value.__aenter__.return_value.post = AsyncMock(
                    return_value=mock_response
                )

                client = COCONUTClient()
                result = await client.search_by_smiles("CCO")

                assert result is None


class TestLookupNaturalProduct:
    """Tests for lookup_natural_product function."""

    @pytest.mark.asyncio
    async def test_lookup_by_inchikey(self):
        """Test lookup by InChIKey."""
        # Mock data uses actual API v2 field names
        mock_data = {
            "identifier": "CNP0123456",
            "name": "Caffeine",
            "canonical_smiles": "CN1C=NC2=C1C(=O)N(C(=O)N2C)C",
            "standard_inchi_key": "RYYVLZVUVIJVGH-UHFFFAOYSA-N",
            "molecular_weight": 194.19,
        }

        with patch(
            "app.services.integrations.coconut.COCONUTClient"
        ) as mock_client_class:
            mock_client = AsyncMock()
            mock_client.token = "test_token"  # Ensure token check passes
            mock_client.search_by_inchikey = AsyncMock(return_value=mock_data)
            mock_client_class.return_value = mock_client

            request = COCONUTRequest(inchikey="RYYVLZVUVIJVGH-UHFFFAOYSA-N")
            result = await lookup_natural_product(request)

            assert result.found is True
            assert result.coconut_id == "CNP0123456"
            assert result.name == "Caffeine"
            assert result.url is not None

    @pytest.mark.asyncio
    async def test_lookup_by_smiles(self):
        """Test lookup by SMILES."""
        mock_data = {
            "identifier": "CNP0123456",
            "name": "Ethanol",
            "canonical_smiles": "CCO",
        }

        with patch(
            "app.services.integrations.coconut.COCONUTClient"
        ) as mock_client_class:
            mock_client = AsyncMock()
            mock_client.token = "test_token"
            mock_client.search_by_inchikey = AsyncMock(return_value=mock_data)
            mock_client_class.return_value = mock_client

            request = COCONUTRequest(smiles="CCO")
            result = await lookup_natural_product(request)

            assert result.found is True

    @pytest.mark.asyncio
    async def test_lookup_not_found(self):
        """Test lookup not found."""
        with patch(
            "app.services.integrations.coconut.COCONUTClient"
        ) as mock_client_class:
            mock_client = AsyncMock()
            mock_client.token = "test_token"
            mock_client.search_by_inchikey = AsyncMock(return_value=None)
            mock_client.search_by_smiles = AsyncMock(return_value=None)
            mock_client_class.return_value = mock_client

            request = COCONUTRequest(smiles="CCO")
            result = await lookup_natural_product(request)

            assert result.found is False
            assert result.coconut_id is None
