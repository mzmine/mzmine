"""
Basic Validation Checks

Core chemistry validation checks: parsability, sanitization, valence, aromaticity, connectivity.
"""

from rdkit import Chem

from app.schemas.common import Severity

from ..registry import CheckRegistry
from .base import BaseCheck, CheckResult


@CheckRegistry.register("parsability")
class ParsabilityCheck(BaseCheck):
    """
    Check if molecule was successfully parsed.

    This always passes if we reach this point (molecule already parsed).
    """

    name = "parsability"
    description = "Verify molecule can be parsed from input format"
    category = "basic"

    def run(self, mol: Chem.Mol) -> CheckResult:
        """
        Check if molecule exists.

        Args:
            mol: RDKit molecule object

        Returns:
            CheckResult (always passes if mol is not None)
        """
        if mol is None:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.CRITICAL,
                message="Molecule is None - parsing failed",
            )

        return CheckResult(
            check_name=self.name,
            passed=True,
            severity=Severity.INFO,
            message="Molecule successfully parsed",
            details={"num_atoms": mol.GetNumAtoms(), "num_bonds": mol.GetNumBonds()},
        )


@CheckRegistry.register("sanitization")
class SanitizationCheck(BaseCheck):
    """
    Check if molecule can be sanitized.

    Tries to sanitize a copy of the molecule to detect chemistry issues.
    """

    name = "sanitization"
    description = "Verify molecule passes RDKit sanitization"
    category = "basic"

    def run(self, mol: Chem.Mol) -> CheckResult:
        """
        Attempt to sanitize molecule copy.

        Args:
            mol: RDKit molecule object

        Returns:
            CheckResult indicating sanitization success/failure
        """
        if mol is None:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.CRITICAL,
                message="Cannot sanitize None molecule",
            )

        # Create a copy to avoid modifying original
        mol_copy = Chem.Mol(mol)

        try:
            # Attempt sanitization with error catching
            san_result = Chem.SanitizeMol(mol_copy, catchErrors=True)

            if san_result == Chem.SanitizeFlags.SANITIZE_NONE:
                return CheckResult(
                    check_name=self.name,
                    passed=True,
                    severity=Severity.INFO,
                    message="Molecule sanitizes successfully",
                )
            else:
                # Collect which sanitization steps failed
                failed_steps = []
                if san_result & Chem.SanitizeFlags.SANITIZE_PROPERTIES:
                    failed_steps.append("properties")
                if san_result & Chem.SanitizeFlags.SANITIZE_KEKULIZE:
                    failed_steps.append("kekulization")
                if san_result & Chem.SanitizeFlags.SANITIZE_SETAROMATICITY:
                    failed_steps.append("aromaticity")

                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.ERROR,
                    message=f"Sanitization failed: {', '.join(failed_steps)}",
                    details={"failed_steps": failed_steps},
                )

        except Exception as e:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message=f"Sanitization error: {str(e)}",
            )


@CheckRegistry.register("valence")
class ValenceCheck(BaseCheck):
    """
    Check for valence errors using DetectChemistryProblems.

    CRITICAL severity as valence errors indicate chemically impossible structures.
    """

    name = "valence"
    description = "Verify all atoms have valid valences"
    category = "basic"

    def run(self, mol: Chem.Mol) -> CheckResult:
        """
        Detect chemistry problems, focusing on valence errors.

        Args:
            mol: RDKit molecule object

        Returns:
            CheckResult with CRITICAL severity if valence errors found
        """
        if mol is None:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.CRITICAL,
                message="Cannot check valence of None molecule",
            )

        try:
            problems = Chem.DetectChemistryProblems(mol)

            if not problems:
                return CheckResult(
                    check_name=self.name,
                    passed=True,
                    severity=Severity.INFO,
                    message="All atoms have valid valences",
                )

            # Collect valence-related problems
            valence_issues = []
            affected_atoms = set()

            for problem in problems:
                problem_type = problem.GetType()
                if "valence" in problem_type.lower() or "atom" in problem_type.lower():
                    valence_issues.append(problem.Message())
                    # Try to get affected atom index
                    if hasattr(problem, "GetAtomIdx"):
                        affected_atoms.add(problem.GetAtomIdx())

            if valence_issues:
                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.CRITICAL,
                    message=f"Valence errors detected: {'; '.join(valence_issues)}",
                    affected_atoms=list(affected_atoms),
                    details={"issues": valence_issues},
                )

            # Other chemistry problems but no valence issues
            return CheckResult(
                check_name=self.name,
                passed=True,
                severity=Severity.INFO,
                message="No valence errors (other chemistry issues may exist)",
                details={"other_problems": [p.Message() for p in problems]},
            )

        except Exception as e:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message=f"Error checking valence: {str(e)}",
            )


@CheckRegistry.register("aromaticity")
class AromaticityCheck(BaseCheck):
    """
    Check if aromatic rings can be kekulized.

    Tests if aromatic structures have valid Kekulé forms.
    """

    name = "aromaticity"
    description = "Verify aromatic rings can be kekulized"
    category = "basic"

    def run(self, mol: Chem.Mol) -> CheckResult:
        """
        Attempt to kekulize aromatic rings.

        Args:
            mol: RDKit molecule object

        Returns:
            CheckResult indicating kekulization success/failure
        """
        if mol is None:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message="Cannot check aromaticity of None molecule",
            )

        # Check if molecule has aromatic atoms
        has_aromatic = any(atom.GetIsAromatic() for atom in mol.GetAtoms())

        if not has_aromatic:
            return CheckResult(
                check_name=self.name,
                passed=True,
                severity=Severity.INFO,
                message="No aromatic atoms present",
            )

        # Try to kekulize
        mol_copy = Chem.Mol(mol)

        try:
            Chem.Kekulize(mol_copy, clearAromaticFlags=False)
            return CheckResult(
                check_name=self.name,
                passed=True,
                severity=Severity.INFO,
                message="Aromatic rings kekulize successfully",
            )
        except Exception as e:
            # Collect aromatic atom indices
            aromatic_atoms = [
                atom.GetIdx() for atom in mol.GetAtoms() if atom.GetIsAromatic()
            ]

            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message=f"Kekulization failed: {str(e)}",
                affected_atoms=aromatic_atoms,
            )


@CheckRegistry.register("connectivity")
class ConnectivityCheck(BaseCheck):
    """
    Check for disconnected fragments.

    Multiple fragments may be intentional (e.g., salt forms) so this is WARNING level.
    """

    name = "connectivity"
    description = "Check for disconnected molecular fragments"
    category = "basic"

    def run(self, mol: Chem.Mol) -> CheckResult:
        """
        Check if molecule has multiple disconnected fragments.

        Args:
            mol: RDKit molecule object

        Returns:
            CheckResult with WARNING if multiple fragments found
        """
        if mol is None:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message="Cannot check connectivity of None molecule",
            )

        try:
            frags = Chem.GetMolFrags(mol, asMols=False, sanitizeFrags=False)
            num_frags = len(frags)

            if num_frags == 1:
                return CheckResult(
                    check_name=self.name,
                    passed=True,
                    severity=Severity.INFO,
                    message="Molecule is fully connected",
                    details={"num_fragments": 1},
                )
            else:
                # Collect fragment sizes
                frag_sizes = [len(frag) for frag in frags]

                return CheckResult(
                    check_name=self.name,
                    passed=False,
                    severity=Severity.WARNING,
                    message=f"Molecule has {num_frags} disconnected fragments",
                    details={
                        "num_fragments": num_frags,
                        "fragment_sizes": frag_sizes,
                    },
                )

        except Exception as e:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message=f"Error checking connectivity: {str(e)}",
            )
