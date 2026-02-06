"""
Tests for structural alert screening API endpoints.
"""

import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app


@pytest.fixture
async def client():
    """Create async test client."""
    async with AsyncClient(
        transport=ASGITransport(app=app), base_url="http://test"
    ) as ac:
        yield ac


class TestAlertEndpoint:
    """Test POST /api/v1/alerts endpoint."""

    @pytest.mark.asyncio
    async def test_screen_rhodanine_pains(self, client: AsyncClient):
        """Test screening rhodanine for PAINS alerts."""
        response = await client.post(
            "/api/v1/alerts",
            json={
                "molecule": "O=C1NC(=S)SC1",  # Rhodanine (rhod_sat_A pattern)
                "catalogs": ["PAINS"],
            },
        )

        assert response.status_code == 200
        data = response.json()

        assert data["status"] == "completed"
        assert data["total_alerts"] > 0
        assert "PAINS" in data["screened_catalogs"]
        assert len(data["alerts"]) > 0

        # Check alert structure
        alert = data["alerts"][0]
        assert "pattern_name" in alert
        assert "severity" in alert
        assert "matched_atoms" in alert
        assert "catalog_source" in alert

    @pytest.mark.asyncio
    async def test_screen_clean_molecule(self, client: AsyncClient):
        """Test screening clean molecule (no alerts)."""
        response = await client.post(
            "/api/v1/alerts", json={"molecule": "CCO", "catalogs": ["PAINS"]}  # Ethanol
        )

        assert response.status_code == 200
        data = response.json()

        assert data["status"] == "completed"
        assert data["total_alerts"] == 0
        assert len(data["alerts"]) == 0

    @pytest.mark.asyncio
    async def test_screen_multiple_catalogs(self, client: AsyncClient):
        """Test screening with multiple catalogs."""
        response = await client.post(
            "/api/v1/alerts",
            json={
                "molecule": "CCBr",  # Bromoethane (BRENK alert)
                "catalogs": ["PAINS", "BRENK"],
            },
        )

        assert response.status_code == 200
        data = response.json()

        assert "PAINS" in data["screened_catalogs"]
        assert "BRENK" in data["screened_catalogs"]
        # Should have BRENK alert for alkyl halide
        assert data["total_alerts"] > 0

    @pytest.mark.asyncio
    async def test_screen_invalid_molecule(self, client: AsyncClient):
        """Test screening with invalid molecule."""
        response = await client.post(
            "/api/v1/alerts",
            json={"molecule": "not_a_valid_smiles", "catalogs": ["PAINS"]},
        )

        assert response.status_code == 400
        data = response.json()
        assert "error" in str(data).lower() or "detail" in data

    @pytest.mark.asyncio
    async def test_screen_default_catalogs(self, client: AsyncClient):
        """Test screening with default catalogs (PAINS)."""
        response = await client.post(
            "/api/v1/alerts",
            json={
                "molecule": "O=C1NC(=S)SC1"  # Rhodanine (rhod_sat_A pattern)
                # No catalogs specified - should default to PAINS
            },
        )

        assert response.status_code == 200
        data = response.json()

        assert "PAINS" in data["screened_catalogs"]
        assert data["total_alerts"] > 0

    @pytest.mark.asyncio
    async def test_response_includes_molecule_info(self, client: AsyncClient):
        """Test that response includes molecule information."""
        response = await client.post(
            "/api/v1/alerts",
            json={"molecule": "c1ccccc1", "catalogs": ["PAINS"]},  # Benzene
        )

        assert response.status_code == 200
        data = response.json()

        mol_info = data["molecule_info"]
        assert "input_string" in mol_info
        assert "canonical_smiles" in mol_info
        assert mol_info["canonical_smiles"] is not None

    @pytest.mark.asyncio
    async def test_response_includes_educational_note(self, client: AsyncClient):
        """Test that response includes educational context."""
        response = await client.post(
            "/api/v1/alerts", json={"molecule": "CCO", "catalogs": ["PAINS"]}
        )

        assert response.status_code == 200
        data = response.json()

        assert "educational_note" in data
        assert "warning" in data["educational_note"].lower()

    @pytest.mark.asyncio
    async def test_alert_severity_is_warning(self, client: AsyncClient):
        """Test that PAINS alerts have WARNING severity (not ERROR)."""
        response = await client.post(
            "/api/v1/alerts",
            json={
                "molecule": "O=C1NC(=S)SC1",  # Rhodanine (rhod_sat_A pattern)
                "catalogs": ["PAINS"],
            },
        )

        assert response.status_code == 200
        data = response.json()

        # PAINS should be warnings, not errors
        for alert in data["alerts"]:
            assert alert["severity"] in ["warning", "critical", "info"]


class TestCatalogsEndpoint:
    """Test GET /api/v1/alerts/catalogs endpoint."""

    @pytest.mark.asyncio
    async def test_list_catalogs(self, client: AsyncClient):
        """Test listing available catalogs."""
        response = await client.get("/api/v1/alerts/catalogs")

        assert response.status_code == 200
        data = response.json()

        assert "catalogs" in data
        assert "PAINS" in data["catalogs"]
        assert "BRENK" in data["catalogs"]
        assert "default_catalogs" in data

    @pytest.mark.asyncio
    async def test_catalog_info_structure(self, client: AsyncClient):
        """Test catalog information structure."""
        response = await client.get("/api/v1/alerts/catalogs")

        assert response.status_code == 200
        data = response.json()

        pains_info = data["catalogs"]["PAINS"]
        assert "name" in pains_info
        assert "description" in pains_info
        assert "pattern_count" in pains_info
        assert "severity" in pains_info


class TestQuickCheckEndpoint:
    """Test POST /api/v1/alerts/quick-check endpoint."""

    @pytest.mark.asyncio
    async def test_quick_check_with_alerts(self, client: AsyncClient):
        """Test quick check on molecule with alerts."""
        response = await client.post(
            "/api/v1/alerts/quick-check",
            json={
                "molecule": "O=C1NC(=S)SC1",  # Rhodanine (rhod_sat_A pattern)
                "catalogs": ["PAINS"],
            },
        )

        assert response.status_code == 200
        data = response.json()

        assert data["has_alerts"] is True
        assert "PAINS" in data["checked_catalogs"]

    @pytest.mark.asyncio
    async def test_quick_check_no_alerts(self, client: AsyncClient):
        """Test quick check on clean molecule."""
        response = await client.post(
            "/api/v1/alerts/quick-check",
            json={"molecule": "CCO", "catalogs": ["PAINS"]},  # Ethanol
        )

        assert response.status_code == 200
        data = response.json()

        assert data["has_alerts"] is False
