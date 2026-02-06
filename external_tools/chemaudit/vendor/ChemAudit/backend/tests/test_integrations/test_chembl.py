"""
Tests for ChEMBL integration client.
"""

from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest

from app.schemas.integrations import ChEMBLRequest
from app.services.integrations.chembl import ChEMBLClient, get_bioactivity


class TestChEMBLClient:
    """Tests for ChEMBLClient."""

    @pytest.mark.asyncio
    async def test_search_by_smiles_success(self):
        """Test successful search by SMILES."""
        client = ChEMBLClient()
        mock_response = MagicMock()
        mock_response.json.return_value = {
            "molecules": [{"molecule_chembl_id": "CHEMBL25"}]
        }
        mock_response.raise_for_status = MagicMock()

        with patch("httpx.AsyncClient") as mock_client:
            mock_client.return_value.__aenter__.return_value.get = AsyncMock(
                return_value=mock_response
            )

            result = await client.search_by_smiles("CC(=O)Oc1ccccc1C(=O)O")

            assert result == "CHEMBL25"

    @pytest.mark.asyncio
    async def test_get_molecule_by_inchikey_success(self):
        """Test successful retrieval by InChIKey."""
        client = ChEMBLClient()
        mock_response = MagicMock()
        mock_response.json.return_value = {
            "molecules": [{"molecule_chembl_id": "CHEMBL25", "pref_name": "ASPIRIN"}]
        }
        mock_response.raise_for_status = MagicMock()

        with patch("httpx.AsyncClient") as mock_client:
            mock_client.return_value.__aenter__.return_value.get = AsyncMock(
                return_value=mock_response
            )

            result = await client.get_molecule_by_inchikey(
                "BSYNRYMUTXBXSQ-UHFFFAOYSA-N"
            )

            assert result is not None
            assert result["molecule_chembl_id"] == "CHEMBL25"

    @pytest.mark.asyncio
    async def test_get_molecule_success(self):
        """Test successful retrieval of molecule by ChEMBL ID."""
        client = ChEMBLClient()
        mock_response = MagicMock()
        mock_response.json.return_value = {
            "molecule_chembl_id": "CHEMBL25",
            "pref_name": "ASPIRIN",
            "max_phase": 4,
        }
        mock_response.raise_for_status = MagicMock()

        with patch("httpx.AsyncClient") as mock_client:
            mock_client.return_value.__aenter__.return_value.get = AsyncMock(
                return_value=mock_response
            )

            result = await client.get_molecule("CHEMBL25")

            assert result is not None
            assert result["pref_name"] == "ASPIRIN"

    @pytest.mark.asyncio
    async def test_get_bioactivities_success(self):
        """Test successful retrieval of bioactivities."""
        client = ChEMBLClient()
        mock_response = MagicMock()
        mock_response.json.return_value = {
            "activities": [
                {
                    "target_chembl_id": "CHEMBL240",
                    "target_pref_name": "Cyclooxygenase-1",
                    "standard_type": "IC50",
                    "standard_value": 100.0,
                    "standard_units": "nM",
                    "assay_chembl_id": "CHEMBL12345",
                }
            ]
        }
        mock_response.raise_for_status = MagicMock()

        with patch("httpx.AsyncClient") as mock_client:
            mock_client.return_value.__aenter__.return_value.get = AsyncMock(
                return_value=mock_response
            )

            result = await client.get_bioactivities("CHEMBL25")

            assert len(result) == 1
            assert result[0]["target_chembl_id"] == "CHEMBL240"

    @pytest.mark.asyncio
    async def test_api_failure_returns_none(self):
        """Test API failure returns None gracefully."""
        client = ChEMBLClient()
        mock_response = MagicMock()
        # Use httpx.HTTPStatusError which is what raise_for_status() raises
        mock_response.raise_for_status.side_effect = httpx.HTTPStatusError(
            "API error", request=MagicMock(), response=MagicMock()
        )

        with patch("httpx.AsyncClient") as mock_client:
            mock_client.return_value.__aenter__.return_value.get = AsyncMock(
                return_value=mock_response
            )

            result = await client.get_molecule("INVALID")

            assert result is None


class TestGetBioactivity:
    """Tests for get_bioactivity function."""

    @pytest.mark.asyncio
    async def test_get_bioactivity_by_inchikey(self):
        """Test getting bioactivity by InChIKey."""
        mock_molecule = {
            "molecule_chembl_id": "CHEMBL25",
            "pref_name": "ASPIRIN",
            "max_phase": 4,
            "molecule_properties": {
                "full_molecular_formula": "C9H8O4",
                "molecular_weight": 180.16,
            },
        }

        mock_bioactivities = [
            {
                "target_chembl_id": "CHEMBL240",
                "target_pref_name": "Cyclooxygenase-1",
                "standard_type": "IC50",
                "standard_value": 100.0,
                "standard_units": "nM",
                "assay_chembl_id": "CHEMBL12345",
            }
        ]

        with patch(
            "app.services.integrations.chembl.ChEMBLClient"
        ) as mock_client_class:
            mock_client = AsyncMock()
            mock_client.get_molecule_by_inchikey = AsyncMock(return_value=mock_molecule)
            mock_client.get_bioactivities = AsyncMock(return_value=mock_bioactivities)
            mock_client_class.return_value = mock_client

            request = ChEMBLRequest(inchikey="BSYNRYMUTXBXSQ-UHFFFAOYSA-N")
            result = await get_bioactivity(request)

            assert result.found is True
            assert result.chembl_id == "CHEMBL25"
            assert result.bioactivity_count == 1
            assert len(result.bioactivities) == 1

    @pytest.mark.asyncio
    async def test_get_bioactivity_not_found(self):
        """Test getting bioactivity when not found."""
        with patch(
            "app.services.integrations.chembl.ChEMBLClient"
        ) as mock_client_class:
            mock_client = AsyncMock()
            mock_client.get_molecule_by_inchikey = AsyncMock(return_value=None)
            mock_client.search_by_smiles = AsyncMock(return_value=None)
            mock_client_class.return_value = mock_client

            request = ChEMBLRequest(smiles="INVALID")
            result = await get_bioactivity(request)

            assert result.found is False
            assert result.chembl_id is None
            assert result.bioactivity_count == 0

    @pytest.mark.asyncio
    async def test_get_bioactivity_no_activities(self):
        """Test getting bioactivity when molecule found but no activities."""
        mock_molecule = {
            "molecule_chembl_id": "CHEMBL99999",
            "pref_name": "Test Compound",
        }

        with patch(
            "app.services.integrations.chembl.ChEMBLClient"
        ) as mock_client_class:
            mock_client = AsyncMock()
            mock_client.get_molecule_by_inchikey = AsyncMock(return_value=mock_molecule)
            mock_client.get_bioactivities = AsyncMock(return_value=[])
            mock_client_class.return_value = mock_client

            request = ChEMBLRequest(inchikey="TEST-INCHIKEY")
            result = await get_bioactivity(request)

            assert result.found is True
            assert result.chembl_id == "CHEMBL99999"
            assert result.bioactivity_count == 0
