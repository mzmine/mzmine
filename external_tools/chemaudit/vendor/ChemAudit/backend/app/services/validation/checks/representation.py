"""
Representation Consistency Checks

Checks for issues with molecular representation conversions (SMILES/InChI roundtrips).
"""

from rdkit import Chem

from app.schemas.common import Severity

from ..registry import CheckRegistry
from .base import BaseCheck, CheckResult


@CheckRegistry.register("smiles_roundtrip")
class SmilesRoundtripCheck(BaseCheck):
    """
    Check SMILES roundtrip consistency.

    Converts mol -> SMILES -> mol -> SMILES and compares InChIKeys.
    Differences indicate information loss or ambiguity.
    """

    name = "smiles_roundtrip"
    description = "Verify SMILES roundtrip produces consistent structure"
    category = "representation"

    def run(self, mol: Chem.Mol) -> CheckResult:
        """
        Perform SMILES roundtrip test.

        Args:
            mol: RDKit molecule object

        Returns:
            CheckResult with WARNING if roundtrip produces different structure
        """
        if mol is None:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message="Cannot perform roundtrip on None molecule",
            )

        try:
            # Get original InChIKey for comparison
            original_inchikey = Chem.MolToInchiKey(mol)
            if not original_inchikey:
                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.WARNING,
                    message="Cannot generate InChIKey for comparison",
                )

            # Convert to SMILES
            smiles = Chem.MolToSmiles(mol)
            if not smiles:
                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.WARNING,
                    message="Cannot generate SMILES for roundtrip",
                )

            # Convert back to mol
            roundtrip_mol = Chem.MolFromSmiles(smiles)
            if roundtrip_mol is None:
                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.WARNING,
                    message="SMILES roundtrip failed to parse",
                    details={"smiles": smiles},
                )

            # Get roundtrip InChIKey
            roundtrip_inchikey = Chem.MolToInchiKey(roundtrip_mol)
            if not roundtrip_inchikey:
                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.WARNING,
                    message="Cannot generate InChIKey from roundtrip molecule",
                )

            # Compare InChIKeys
            if original_inchikey == roundtrip_inchikey:
                return CheckResult(
                    check_name=self.name,
                    passed=True,
                    severity=Severity.INFO,
                    message="SMILES roundtrip successful",
                    details={
                        "smiles": smiles,
                        "inchikey": original_inchikey,
                    },
                )
            else:
                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.WARNING,
                    message="SMILES roundtrip produced different structure",
                    details={
                        "original_inchikey": original_inchikey,
                        "roundtrip_inchikey": roundtrip_inchikey,
                        "smiles": smiles,
                    },
                )

        except Exception as e:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message=f"Error during SMILES roundtrip: {str(e)}",
            )


@CheckRegistry.register("inchi_generation")
class InchiGenerationCheck(BaseCheck):
    """
    Check if valid InChI can be generated.

    InChI generation failure indicates structural representation issues.
    """

    name = "inchi_generation"
    description = "Verify valid InChI can be generated"
    category = "representation"

    def run(self, mol: Chem.Mol) -> CheckResult:
        """
        Attempt to generate InChI.

        Args:
            mol: RDKit molecule object

        Returns:
            CheckResult with WARNING if InChI generation fails
        """
        if mol is None:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message="Cannot generate InChI for None molecule",
            )

        try:
            # Attempt InChI generation
            inchi = Chem.MolToInchi(mol)

            if not inchi:
                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.WARNING,
                    message="Could not generate valid InChI",
                )

            # Also try InChIKey
            inchikey = Chem.MolToInchiKey(mol)

            if not inchikey:
                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.WARNING,
                    message="Generated InChI but InChIKey failed",
                    details={"inchi": inchi},
                )

            return CheckResult(
                check_name=self.name,
                passed=True,
                severity=Severity.INFO,
                message="InChI and InChIKey generated successfully",
                details={
                    "inchi": inchi,
                    "inchikey": inchikey,
                },
            )

        except Exception as e:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.WARNING,
                message=f"InChI generation error: {str(e)}",
            )


@CheckRegistry.register("inchi_roundtrip")
class InchiRoundtripCheck(BaseCheck):
    """
    Check InChI roundtrip consistency.

    Converts mol -> InChI -> mol -> InChI and compares.
    Some differences expected with complex stereochemistry.
    """

    name = "inchi_roundtrip"
    description = "Verify InChI roundtrip consistency"
    category = "representation"

    def run(self, mol: Chem.Mol) -> CheckResult:
        """
        Perform InChI roundtrip test.

        Args:
            mol: RDKit molecule object

        Returns:
            CheckResult with INFO severity (differences can be expected)
        """
        if mol is None:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message="Cannot perform roundtrip on None molecule",
            )

        try:
            # Generate original InChI
            original_inchi = Chem.MolToInchi(mol)
            if not original_inchi:
                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.INFO,
                    message="Cannot generate InChI for roundtrip",
                )

            # Convert back to mol
            roundtrip_mol = Chem.MolFromInchi(original_inchi)
            if roundtrip_mol is None:
                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.INFO,
                    message="InChI roundtrip failed to parse",
                    details={"inchi": original_inchi},
                )

            # Generate roundtrip InChI
            roundtrip_inchi = Chem.MolToInchi(roundtrip_mol)
            if not roundtrip_inchi:
                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.INFO,
                    message="Cannot generate InChI from roundtrip molecule",
                )

            # Compare InChIs (use InChIKey for better comparison)
            original_inchikey = Chem.MolToInchiKey(mol)
            roundtrip_inchikey = Chem.MolToInchiKey(roundtrip_mol)

            if original_inchikey == roundtrip_inchikey:
                return CheckResult(
                    check_name=self.name,
                    passed=True,
                    severity=Severity.INFO,
                    message="InChI roundtrip successful",
                    details={
                        "inchi": original_inchi,
                        "inchikey": original_inchikey,
                    },
                )
            else:
                # This is INFO level because some differences are expected with
                # complex stereochemistry or tautomers
                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.INFO,
                    message="InChI roundtrip shows minor differences (may be acceptable)",
                    details={
                        "original_inchikey": original_inchikey,
                        "roundtrip_inchikey": roundtrip_inchikey,
                        "note": "Differences common with complex stereochemistry",
                    },
                )

        except Exception as e:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.INFO,
                message=f"Error during InChI roundtrip: {str(e)}",
            )
