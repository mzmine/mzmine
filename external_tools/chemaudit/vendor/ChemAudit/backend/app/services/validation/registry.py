"""
Check Registry with Decorator Pattern

Allows validation checks to self-register using @CheckRegistry.register() decorator.
"""

from typing import Type


class CheckRegistry:
    """
    Registry for validation checks using decorator pattern.

    Checks self-register using @CheckRegistry.register("check_name") decorator.
    """

    _checks: dict[str, Type] = {}

    @classmethod
    def register(cls, name: str):
        """
        Decorator to register a validation check.

        Usage:
            @CheckRegistry.register("valence")
            class ValenceCheck(BaseCheck):
                ...

        Args:
            name: Unique identifier for the check

        Returns:
            Decorator function
        """

        def decorator(check_class: Type) -> Type:
            if name in cls._checks:
                raise ValueError(f"Check '{name}' is already registered")
            cls._checks[name] = check_class
            return check_class

        return decorator

    @classmethod
    def get(cls, name: str) -> Type | None:
        """
        Get a check class by name.

        Args:
            name: Check identifier

        Returns:
            Check class or None if not found
        """
        return cls._checks.get(name)

    @classmethod
    def get_all(cls) -> dict[str, Type]:
        """
        Get all registered checks.

        Returns:
            Dictionary mapping check names to check classes
        """
        return cls._checks.copy()

    @classmethod
    def list_names(cls) -> list[str]:
        """
        Get list of all registered check names.

        Returns:
            List of check names
        """
        return list(cls._checks.keys())

    @classmethod
    def clear(cls) -> None:
        """
        Clear all registered checks.

        Used for testing purposes.
        """
        cls._checks.clear()
