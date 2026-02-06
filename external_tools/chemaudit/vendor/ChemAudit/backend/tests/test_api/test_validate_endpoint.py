"""
Tests for /api/v1/validate endpoint.

Tests single molecule validation API.
"""

import pytest

# Uses shared 'client' fixture from conftest.py


class TestValidateEndpoint:
    """Test POST /api/v1/validate endpoint"""

    @pytest.mark.asyncio
    async def test_validate_valid_smiles(self, client):
        """Should validate a valid SMILES string"""
        response = await client.post(
            "/api/v1/validate", json={"molecule": "CCO", "format": "smiles"}
        )
        assert response.status_code == 200
        data = response.json()

        # Check response structure
        assert "molecule_info" in data
        assert "overall_score" in data
        assert "issues" in data
        assert "all_checks" in data
        assert "execution_time_ms" in data

        # Check molecule info
        assert data["molecule_info"]["canonical_smiles"] == "CCO"
        assert data["molecule_info"]["input_smiles"] == "CCO"
        assert data["molecule_info"]["molecular_formula"] == "C2H6O"
        # num_atoms returns heavy atoms only (3 for C-C-O), not total atoms including H
        assert data["molecule_info"]["num_atoms"] == 3

        # Check score
        assert isinstance(data["overall_score"], int)
        assert 0 <= data["overall_score"] <= 100

        # Valid molecule should have high score
        assert data["overall_score"] >= 80

    @pytest.mark.asyncio
    async def test_validate_benzene(self, client):
        """Should validate benzene"""
        response = await client.post("/api/v1/validate", json={"molecule": "c1ccccc1"})
        assert response.status_code == 200
        data = response.json()
        # By default, API kekulizes SMILES (explicit double bonds)
        # Use preserve_aromatic=True for aromatic notation
        assert data["molecule_info"]["canonical_smiles"] == "C1=CC=CC=C1"
        assert data["overall_score"] >= 80

    @pytest.mark.asyncio
    async def test_validate_invalid_smiles(self, client):
        """Should return 400 for invalid SMILES"""
        response = await client.post(
            "/api/v1/validate", json={"molecule": "invalid_smiles_xyz"}
        )
        assert response.status_code == 400
        data = response.json()
        assert "detail" in data
        assert "error" in data["detail"]

    @pytest.mark.asyncio
    async def test_validate_empty_molecule(self, client):
        """Should return 422 for empty molecule string"""
        response = await client.post("/api/v1/validate", json={"molecule": ""})
        assert response.status_code == 422

    @pytest.mark.asyncio
    async def test_validate_with_specific_checks(self, client):
        """Should run only specified checks"""
        response = await client.post(
            "/api/v1/validate",
            json={"molecule": "CCO", "checks": ["parsability", "valence"]},
        )
        assert response.status_code == 200
        data = response.json()

        # Should have only 2 checks
        assert len(data["all_checks"]) == 2
        check_names = [c["check_name"] for c in data["all_checks"]]
        assert "parsability" in check_names
        assert "valence" in check_names

    @pytest.mark.asyncio
    async def test_validate_returns_all_checks(self, client):
        """Should return all check results"""
        response = await client.post("/api/v1/validate", json={"molecule": "CCO"})
        assert response.status_code == 200
        data = response.json()

        # Should have all 11 checks (basic + representation + stereochemistry)
        assert len(data["all_checks"]) == 11
        check_names = {c["check_name"] for c in data["all_checks"]}
        expected_checks = {
            # Basic checks
            "parsability",
            "sanitization",
            "valence",
            "aromaticity",
            "connectivity",
            # Representation checks
            "smiles_roundtrip",
            "inchi_generation",
            "inchi_roundtrip",
            # Stereochemistry checks
            "undefined_stereocenters",
            "undefined_doublebond_stereo",
            "conflicting_stereo",
        }
        assert check_names == expected_checks

    @pytest.mark.asyncio
    async def test_validate_separates_issues(self, client):
        """Should separate failed checks into issues"""
        response = await client.post("/api/v1/validate", json={"molecule": "CCO"})
        assert response.status_code == 200
        data = response.json()

        # Issues should only contain failed checks
        for issue in data["issues"]:
            assert issue["passed"] is False

    @pytest.mark.asyncio
    async def test_validate_inchi_format(self, client):
        """Should validate InChI format"""
        response = await client.post(
            "/api/v1/validate",
            json={"molecule": "InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3", "format": "inchi"},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["molecule_info"]["canonical_smiles"] == "CCO"

    @pytest.mark.asyncio
    async def test_validate_auto_format_detection(self, client):
        """Should auto-detect format"""
        response = await client.post(
            "/api/v1/validate", json={"molecule": "CCO", "format": "auto"}
        )
        assert response.status_code == 200

    @pytest.mark.asyncio
    async def test_validate_returns_execution_time(self, client):
        """Should return execution time"""
        response = await client.post("/api/v1/validate", json={"molecule": "CCO"})
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data["execution_time_ms"], int)
        assert data["execution_time_ms"] > 0


class TestListChecksEndpoint:
    """Test GET /api/v1/checks endpoint"""

    @pytest.mark.asyncio
    async def test_list_checks(self, client):
        """Should list available checks by category"""
        response = await client.get("/api/v1/checks")
        assert response.status_code == 200
        data = response.json()

        # Should return dictionary
        assert isinstance(data, dict)

        # Should have all categories
        assert "basic" in data
        assert "representation" in data
        assert "stereochemistry" in data

        # Basic category should have 5 checks
        assert len(data["basic"]) == 5
        expected_basic = {
            "parsability",
            "sanitization",
            "valence",
            "aromaticity",
            "connectivity",
        }
        assert set(data["basic"]) == expected_basic

        # Representation category should have 3 checks
        assert len(data["representation"]) == 3
        expected_repr = {"smiles_roundtrip", "inchi_generation", "inchi_roundtrip"}
        assert set(data["representation"]) == expected_repr

        # Stereochemistry category should have 3 checks
        assert len(data["stereochemistry"]) == 3
        expected_stereo = {
            "undefined_stereocenters",
            "undefined_doublebond_stereo",
            "conflicting_stereo",
        }
        assert set(data["stereochemistry"]) == expected_stereo
