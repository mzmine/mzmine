"""
Stereochemistry Validation Checks

Checks for undefined/conflicting stereochemistry in molecular structures.
"""

from rdkit import Chem

from app.schemas.common import Severity

from ..registry import CheckRegistry
from .base import BaseCheck, CheckResult


@CheckRegistry.register("undefined_stereocenters")
class UndefinedStereoCentersCheck(BaseCheck):
    """
    Check for undefined chiral centers.

    Identifies tetrahedral chiral centers that lack stereochemical assignment.
    """

    name = "undefined_stereocenters"
    description = "Detect chiral centers with undefined stereochemistry"
    category = "stereochemistry"

    def run(self, mol: Chem.Mol) -> CheckResult:
        """
        Find chiral centers marked as undefined.

        Args:
            mol: RDKit molecule object

        Returns:
            CheckResult with WARNING if undefined centers found
        """
        if mol is None:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message="Cannot check stereochemistry of None molecule",
            )

        try:
            # Find chiral centers including unassigned ones
            chiral_centers = Chem.FindMolChiralCenters(
                mol, includeUnassigned=True, useLegacyImplementation=False
            )

            # Filter for undefined (marked with '?')
            undefined_centers = [
                (idx, stereo) for idx, stereo in chiral_centers if stereo == "?"
            ]

            if not undefined_centers:
                # Check if there are any defined centers for better message
                if chiral_centers:
                    return CheckResult(
                        check_name=self.name,
                        passed=True,
                        severity=Severity.INFO,
                        message=f"All {len(chiral_centers)} stereocenter(s) properly defined",
                        details={"total_centers": len(chiral_centers)},
                    )
                else:
                    return CheckResult(
                        check_name=self.name,
                        passed=True,
                        severity=Severity.INFO,
                        message="No chiral centers present",
                    )

            # Extract atom indices
            affected_atoms = [idx for idx, _ in undefined_centers]

            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.WARNING,
                message=f"Found {len(undefined_centers)} undefined stereocenter(s) out of a total of {len(chiral_centers)} stereocenter(s)",
                affected_atoms=affected_atoms,
                details={
                    "undefined_count": len(undefined_centers),
                    "total_centers": len(chiral_centers),
                },
            )

        except Exception as e:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message=f"Error checking stereocenters: {str(e)}",
            )


@CheckRegistry.register("undefined_doublebond_stereo")
class UndefinedDoubleBondStereoCheck(BaseCheck):
    """
    Check for double bonds with undefined E/Z stereochemistry.

    Identifies double bonds that could have E/Z specification but don't.
    """

    name = "undefined_doublebond_stereo"
    description = "Detect double bonds with undefined E/Z stereochemistry"
    category = "stereochemistry"

    def run(self, mol: Chem.Mol) -> CheckResult:
        """
        Find double bonds with undefined stereochemistry.

        Args:
            mol: RDKit molecule object

        Returns:
            CheckResult with WARNING if undefined double bonds found
        """
        if mol is None:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message="Cannot check double bond stereo of None molecule",
            )

        try:
            undefined_bonds = []
            affected_atoms = []
            total_double_bonds = 0

            for bond in mol.GetBonds():
                # Check for double bonds
                if bond.GetBondType() == Chem.BondType.DOUBLE:
                    total_double_bonds += 1

                    # Skip bonds in rings (E/Z not applicable)
                    if bond.IsInRing():
                        continue

                    # Check if stereochemistry is defined
                    stereo = bond.GetStereo()

                    # STEREONONE means no stereo specified for a potentially stereogenic bond
                    if stereo == Chem.BondStereo.STEREONONE:
                        # Get atoms at each end
                        begin_atom = bond.GetBeginAtom()
                        end_atom = bond.GetEndAtom()

                        # Check if both atoms have substituents (required for E/Z)
                        # A double bond needs at least 2 non-hydrogen substituents on each carbon
                        begin_neighbors = [
                            n
                            for n in begin_atom.GetNeighbors()
                            if n.GetIdx() != end_atom.GetIdx()
                        ]
                        end_neighbors = [
                            n
                            for n in end_atom.GetNeighbors()
                            if n.GetIdx() != begin_atom.GetIdx()
                        ]

                        # If both carbons have substituents, E/Z could be specified
                        if len(begin_neighbors) >= 1 and len(end_neighbors) >= 1:
                            undefined_bonds.append(bond.GetIdx())
                            affected_atoms.extend(
                                [begin_atom.GetIdx(), end_atom.GetIdx()]
                            )

            if not undefined_bonds:
                if total_double_bonds > 0:
                    return CheckResult(
                        check_name=self.name,
                        passed=True,
                        severity=Severity.INFO,
                        message=f"All {total_double_bonds} double bond(s) properly defined or not stereogenic",
                        details={"total_double_bonds": total_double_bonds},
                    )
                else:
                    return CheckResult(
                        check_name=self.name,
                        passed=True,
                        severity=Severity.INFO,
                        message="No double bonds present",
                    )

            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.WARNING,
                message=f"Found {len(undefined_bonds)} double bond(s) with undefined E/Z stereo out of a total of {total_double_bonds} double bond(s)",
                affected_atoms=list(set(affected_atoms)),  # Remove duplicates
                details={
                    "undefined_count": len(undefined_bonds),
                    "total_double_bonds": total_double_bonds,
                },
            )

        except Exception as e:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message=f"Error checking double bond stereo: {str(e)}",
            )


@CheckRegistry.register("conflicting_stereo")
class ConflictingStereoCheck(BaseCheck):
    """
    Check for conflicting stereochemistry specifications.

    Uses RDKit's chemistry problem detection to find stereo conflicts.
    """

    name = "conflicting_stereo"
    description = "Detect conflicting stereochemistry specifications"
    category = "stereochemistry"

    def run(self, mol: Chem.Mol) -> CheckResult:
        """
        Detect stereochemistry conflicts.

        Args:
            mol: RDKit molecule object

        Returns:
            CheckResult with ERROR severity if conflicts found
        """
        if mol is None:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message="Cannot check stereo conflicts of None molecule",
            )

        try:
            # Use DetectChemistryProblems to find issues
            problems = Chem.DetectChemistryProblems(mol)

            # Filter for stereochemistry-related problems
            stereo_problems = []
            affected_atoms = set()

            for problem in problems:
                problem_type = problem.GetType()
                problem_msg = problem.Message()

                # Look for stereo-related issues
                if any(
                    keyword in problem_type.lower()
                    for keyword in ["stereo", "chiral", "cis", "trans"]
                ) or any(
                    keyword in problem_msg.lower()
                    for keyword in ["stereo", "chiral", "cis", "trans"]
                ):
                    stereo_problems.append(problem_msg)

                    # Try to get affected atom
                    if hasattr(problem, "GetAtomIdx"):
                        affected_atoms.add(problem.GetAtomIdx())

            if not stereo_problems:
                return CheckResult(
                    check_name=self.name,
                    passed=True,
                    severity=Severity.INFO,
                    message="No stereochemistry conflicts detected",
                )

            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message=f"Found conflicting stereochemistry: {'; '.join(stereo_problems)}",
                affected_atoms=list(affected_atoms),
                details={"conflicts": stereo_problems},
            )

        except Exception as e:
            return CheckResult(
                check_name=self.name,
                passed=False,
                severity=Severity.ERROR,
                message=f"Error checking stereo conflicts: {str(e)}",
            )
