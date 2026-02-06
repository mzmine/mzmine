"""
Base Check Classes

Abstract base class and result dataclass for validation checks.
"""

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Any, Dict, List

from rdkit import Chem

from app.schemas.common import Severity


@dataclass
class CheckResult:
    """
    Result of a validation check.

    Attributes:
        check_name: Name of the check that produced this result
        passed: Whether the check passed
        severity: Severity level if check failed
        message: Human-readable description of the result
        affected_atoms: List of atom indices affected by the issue
        details: Additional structured data about the result
    """

    check_name: str
    passed: bool
    severity: Severity = Severity.INFO
    message: str = ""
    affected_atoms: List[int] = field(default_factory=list)
    details: Dict[str, Any] = field(default_factory=dict)


class BaseCheck(ABC):
    """
    Abstract base class for all validation checks.

    All checks must:
    1. Define name, description, category class attributes
    2. Implement run() method that returns CheckResult
    """

    name: str = "base_check"
    description: str = "Base validation check"
    category: str = "general"

    @abstractmethod
    def run(self, mol: Chem.Mol) -> CheckResult:
        """
        Execute the validation check on a molecule.

        Args:
            mol: RDKit molecule object to validate

        Returns:
            CheckResult with pass/fail status and details
        """
        pass
