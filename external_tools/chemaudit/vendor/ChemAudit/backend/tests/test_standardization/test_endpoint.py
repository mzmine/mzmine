"""
Tests for standardization API endpoint.

Tests the POST /api/v1/standardize endpoint.
"""

import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app


@pytest.fixture
async def client():
    """Create async test client."""
    async with AsyncClient(
        transport=ASGITransport(app=app), base_url="http://test/api/v1"
    ) as ac:
        yield ac


class TestStandardizeEndpoint:
    """Test POST /api/v1/standardize endpoint."""

    @pytest.mark.asyncio
    async def test_standardize_simple_molecule(self, client):
        """Simple molecule should be standardized successfully."""
        response = await client.post("/standardize", json={"molecule": "CCO"})

        assert response.status_code == 200
        data = response.json()

        assert "molecule_info" in data
        assert "result" in data
        assert "execution_time_ms" in data

        result = data["result"]
        assert result["success"] is True
        assert result["standardized_smiles"] == "CCO"

    @pytest.mark.asyncio
    async def test_standardize_salt_form(self, client):
        """Salt form should have counterion stripped."""
        response = await client.post("/standardize", json={"molecule": "CCN.Cl"})

        assert response.status_code == 200
        data = response.json()

        result = data["result"]
        assert result["success"] is True
        assert "Cl" not in result["standardized_smiles"]

    @pytest.mark.asyncio
    async def test_standardize_returns_steps(self, client):
        """Response should include pipeline steps."""
        response = await client.post("/standardize", json={"molecule": "CCO"})

        assert response.status_code == 200
        data = response.json()

        result = data["result"]
        assert "steps_applied" in result
        assert (
            len(result["steps_applied"]) >= 4
        )  # checker, standardizer, get_parent, tautomer

        step_names = [s["step_name"] for s in result["steps_applied"]]
        assert "checker" in step_names
        assert "standardizer" in step_names
        assert "get_parent" in step_names

    @pytest.mark.asyncio
    async def test_standardize_returns_stereo_comparison(self, client):
        """Response should include stereochemistry comparison."""
        response = await client.post("/standardize", json={"molecule": "C[C@H](O)CC"})

        assert response.status_code == 200
        data = response.json()

        result = data["result"]
        assert "stereo_comparison" in result
        assert result["stereo_comparison"] is not None

    @pytest.mark.asyncio
    async def test_standardize_with_tautomer_option(self, client):
        """Tautomer option should be respected."""
        # Without tautomer (default)
        response_no_taut = await client.post(
            "/standardize",
            json={"molecule": "CCO", "options": {"include_tautomer": False}},
        )

        # With tautomer
        response_with_taut = await client.post(
            "/standardize",
            json={"molecule": "CCO", "options": {"include_tautomer": True}},
        )

        assert response_no_taut.status_code == 200
        assert response_with_taut.status_code == 200

        # Check tautomer step
        data_no = response_no_taut.json()
        data_yes = response_with_taut.json()

        taut_step_no = next(
            s
            for s in data_no["result"]["steps_applied"]
            if s["step_name"] == "tautomer_canonicalization"
        )
        taut_step_yes = next(
            s
            for s in data_yes["result"]["steps_applied"]
            if s["step_name"] == "tautomer_canonicalization"
        )

        assert taut_step_no["applied"] is False
        assert taut_step_yes["applied"] is True

    @pytest.mark.asyncio
    async def test_standardize_invalid_molecule(self, client):
        """Invalid molecule should return 400 error."""
        response = await client.post(
            "/standardize", json={"molecule": "this-is-not-a-valid-smiles"}
        )

        assert response.status_code == 400
        data = response.json()
        assert "error" in data["detail"]

    @pytest.mark.asyncio
    async def test_standardize_excluded_fragments(self, client):
        """Excluded fragments should be listed."""
        response = await client.post("/standardize", json={"molecule": "CCN.Cl"})

        assert response.status_code == 200
        data = response.json()

        result = data["result"]
        # Should have excluded fragments or no chloride in result
        assert (
            len(result["excluded_fragments"]) > 0
            or "Cl" not in result["standardized_smiles"]
        )

    @pytest.mark.asyncio
    async def test_standardize_structure_comparison(self, client):
        """Response should include structure comparison."""
        response = await client.post("/standardize", json={"molecule": "CC(=O)O.[Na]"})

        assert response.status_code == 200
        data = response.json()

        result = data["result"]
        assert "structure_comparison" in result
        assert result["structure_comparison"] is not None
        assert "original_atom_count" in result["structure_comparison"]
        assert "standardized_atom_count" in result["structure_comparison"]


class TestStandardizeOptionsEndpoint:
    """Test GET /api/v1/standardize/options endpoint."""

    @pytest.mark.asyncio
    async def test_get_options(self, client):
        """Options endpoint should return available options."""
        response = await client.get("/standardize/options")

        assert response.status_code == 200
        data = response.json()

        assert "options" in data
        assert "include_tautomer" in data["options"]
        assert "preserve_stereo" in data["options"]

        # Check tautomer option has warning
        assert "warning" in data["options"]["include_tautomer"]

    @pytest.mark.asyncio
    async def test_get_pipeline_steps(self, client):
        """Options endpoint should list pipeline steps."""
        response = await client.get("/standardize/options")

        assert response.status_code == 200
        data = response.json()

        assert "pipeline_steps" in data
        step_names = [s["name"] for s in data["pipeline_steps"]]
        assert "checker" in step_names
        assert "standardizer" in step_names
        assert "get_parent" in step_names


class TestMoleculeFormats:
    """Test different molecule input formats."""

    @pytest.mark.asyncio
    async def test_inchi_input(self, client):
        """InChI input should be supported."""
        response = await client.post(
            "/standardize",
            json={"molecule": "InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3", "format": "inchi"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["result"]["success"] is True

    @pytest.mark.asyncio
    async def test_auto_format_detection(self, client):
        """Auto format detection should work."""
        # SMILES
        response_smiles = await client.post(
            "/standardize", json={"molecule": "CCO", "format": "auto"}
        )
        assert response_smiles.status_code == 200

        # InChI
        response_inchi = await client.post(
            "/standardize",
            json={"molecule": "InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3", "format": "auto"},
        )
        assert response_inchi.status_code == 200
