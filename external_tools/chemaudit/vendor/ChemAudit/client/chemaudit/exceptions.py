"""
ChemAudit Client Exceptions

Custom exceptions for the ChemAudit API client.
"""


class ChemAuditError(Exception):
    """Base exception for all ChemAudit client errors."""

    pass


class APIError(ChemAuditError):
    """Raised when the API returns an error response."""

    def __init__(
        self, message: str, status_code: int = None, response_data: dict = None
    ):
        super().__init__(message)
        self.status_code = status_code
        self.response_data = response_data or {}

    def __str__(self):
        if self.status_code:
            return f"API Error {self.status_code}: {super().__str__()}"
        return super().__str__()


class RateLimitError(APIError):
    """Raised when rate limit is exceeded."""

    def __init__(self, message: str, retry_after: int = None):
        super().__init__(message, status_code=429)
        self.retry_after = retry_after

    def __str__(self):
        if self.retry_after:
            return f"Rate limit exceeded. Retry after {self.retry_after} seconds."
        return "Rate limit exceeded."


class AuthenticationError(APIError):
    """Raised when API key authentication fails."""

    def __init__(self, message: str = "Invalid or missing API key"):
        super().__init__(message, status_code=401)


class ValidationError(ChemAuditError):
    """Raised when request validation fails."""

    pass


class BatchJobNotFoundError(APIError):
    """Raised when batch job is not found."""

    def __init__(self, job_id: str):
        super().__init__(f"Batch job not found: {job_id}", status_code=404)
        self.job_id = job_id


class TimeoutError(ChemAuditError):
    """Raised when batch job times out."""

    pass
