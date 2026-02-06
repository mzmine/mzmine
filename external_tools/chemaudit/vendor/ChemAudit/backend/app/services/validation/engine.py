"""
Validation Engine

Orchestrates validation checks on molecules.
Initializes registered checks and calculates overall validation scores.
"""

from collections import defaultdict
from typing import Dict, List, Optional

from rdkit import Chem

# Import checks to trigger registration
import app.services.validation.checks.basic  # noqa: F401
import app.services.validation.checks.representation  # noqa: F401
import app.services.validation.checks.stereo  # noqa: F401
from app.schemas.common import Severity
from app.services.validation.checks.base import BaseCheck, CheckResult
from app.services.validation.registry import CheckRegistry


class ValidationEngine:
    """
    Orchestrates validation checks on molecules.

    Manages check lifecycle, execution, and score calculation.
    """

    def __init__(self):
        self._check_instances: Dict[str, BaseCheck] = {}
        self._initialize_checks()

    def _initialize_checks(self):
        """Instantiate all registered checks."""
        for name, check_class in CheckRegistry.get_all().items():
            self._check_instances[name] = check_class()

    def validate(
        self,
        mol: Chem.Mol,
        checks: Optional[List[str]] = None,
    ) -> tuple[List[CheckResult], int]:
        """
        Run validation checks and return results with score.

        Args:
            mol: RDKit molecule object
            checks: List of check names to run (None or "all" = run all checks)

        Returns:
            Tuple of (check_results, overall_score)
        """
        if checks is None or "all" in checks:
            checks_to_run = list(self._check_instances.keys())
        else:
            checks_to_run = [c for c in checks if c in self._check_instances]

        results = []
        for check_name in checks_to_run:
            check = self._check_instances[check_name]
            try:
                result = check.run(mol)
                results.append(result)
            except Exception as e:
                results.append(
                    CheckResult(
                        check_name=check_name,
                        passed=False,
                        severity=Severity.ERROR,
                        message=f"Check failed: {str(e)}",
                    )
                )

        score = self._calculate_score(results)
        return results, score

    def _calculate_score(self, results: List[CheckResult]) -> int:
        """
        Calculate 0-100 score from check results.

        Scoring:
        - CRITICAL: -50 points
        - ERROR: -20 points
        - WARNING: -5 points
        - INFO/PASS: 0 points

        Args:
            results: List of check results

        Returns:
            Score between 0 and 100
        """
        score = 100
        severity_impacts = {
            Severity.CRITICAL: -50,
            Severity.ERROR: -20,
            Severity.WARNING: -5,
            Severity.INFO: 0,
            Severity.PASS: 0,
        }
        for r in results:
            if not r.passed:
                score += severity_impacts.get(r.severity, 0)
        return max(0, min(100, score))

    def list_checks(self) -> Dict[str, List[str]]:
        """
        List available checks by category.

        Returns:
            Dictionary mapping category names to list of check names
        """
        categories: Dict[str, List[str]] = defaultdict(list)
        for name, check in self._check_instances.items():
            categories[check.category].append(name)
        return dict(categories)


# Singleton instance
validation_engine = ValidationEngine()
