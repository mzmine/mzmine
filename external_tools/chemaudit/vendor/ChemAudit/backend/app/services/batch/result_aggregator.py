"""
Result Aggregator Module

Computes statistics from batch processing results.
"""

import hashlib
import json
from collections import Counter
from dataclasses import asdict, dataclass, field
from typing import Any, Dict, List, Optional

import redis

from app.core.config import settings


@dataclass
class BatchStatisticsData:
    """Statistics computed from batch processing results."""

    total: int = 0
    successful: int = 0
    errors: int = 0
    avg_validation_score: Optional[float] = None
    avg_ml_readiness_score: Optional[float] = None
    avg_qed_score: Optional[float] = None
    avg_sa_score: Optional[float] = None
    lipinski_pass_rate: Optional[float] = None
    safety_pass_rate: Optional[float] = None
    score_distribution: Dict[str, int] = field(default_factory=dict)
    alert_summary: Dict[str, int] = field(default_factory=dict)
    issue_summary: Dict[str, int] = field(default_factory=dict)
    processing_time_seconds: Optional[float] = None


def compute_statistics(results: List[Dict[str, Any]]) -> BatchStatisticsData:
    """
    Compute aggregate statistics from batch results.

    Args:
        results: List of result dictionaries from molecule processing

    Returns:
        BatchStatisticsData with computed statistics
    """
    stats = BatchStatisticsData()
    stats.total = len(results)

    validation_scores = []
    ml_readiness_scores = []
    qed_scores = []
    sa_scores = []
    lipinski_passes = 0
    lipinski_total = 0
    safety_passes = 0
    safety_total = 0
    alert_counts: Counter = Counter()
    issue_counts: Counter = Counter()

    for result in results:
        if result.get("status") == "error" or result.get("error"):
            stats.errors += 1
        else:
            stats.successful += 1

            # Collect validation scores and issues
            if "validation" in result and result["validation"]:
                score = result["validation"].get("overall_score")
                if score is not None:
                    validation_scores.append(score)

                # Count failed validation issues by check name
                for issue in result["validation"].get("issues", []):
                    if not issue.get("passed", True):
                        check_name = issue.get("check_name", "Unknown")
                        issue_counts[check_name] += 1

            # Collect scoring data
            if "scoring" in result and result["scoring"]:
                scoring = result["scoring"]

                # ML-readiness scores
                ml_readiness = scoring.get("ml_readiness") or {}
                ml_score = ml_readiness.get("score")
                if ml_score is not None:
                    ml_readiness_scores.append(ml_score)

                # Drug-likeness scores
                druglikeness = scoring.get("druglikeness") or {}
                if druglikeness and "error" not in druglikeness:
                    qed = druglikeness.get("qed_score")
                    if qed is not None:
                        qed_scores.append(qed)
                    lipinski_passed = druglikeness.get("lipinski_passed")
                    if lipinski_passed is not None:
                        lipinski_total += 1
                        if lipinski_passed:
                            lipinski_passes += 1

                # Safety filter scores
                safety = scoring.get("safety_filters") or {}
                if safety and "error" not in safety:
                    all_passed = safety.get("all_passed")
                    if all_passed is not None:
                        safety_total += 1
                        if all_passed:
                            safety_passes += 1

                # ADMET scores
                admet = scoring.get("admet") or {}
                if admet and "error" not in admet:
                    sa = admet.get("sa_score")
                    if sa is not None:
                        sa_scores.append(sa)

            # Count alerts
            if "alerts" in result and result["alerts"]:
                for alert in result["alerts"].get("alerts", []):
                    alert_type = alert.get("catalog", "Unknown")
                    alert_counts[alert_type] += 1

    # Calculate averages
    if validation_scores:
        stats.avg_validation_score = round(
            sum(validation_scores) / len(validation_scores), 1
        )

    if ml_readiness_scores:
        stats.avg_ml_readiness_score = round(
            sum(ml_readiness_scores) / len(ml_readiness_scores), 1
        )

    if qed_scores:
        stats.avg_qed_score = round(sum(qed_scores) / len(qed_scores), 2)

    if sa_scores:
        stats.avg_sa_score = round(sum(sa_scores) / len(sa_scores), 1)

    # Calculate pass rates
    if lipinski_total > 0:
        stats.lipinski_pass_rate = round((lipinski_passes / lipinski_total) * 100, 1)

    if safety_total > 0:
        stats.safety_pass_rate = round((safety_passes / safety_total) * 100, 1)

    # Score distribution buckets
    stats.score_distribution = _compute_score_distribution(validation_scores)

    # Alert summary
    stats.alert_summary = dict(alert_counts)

    # Issue summary (failed validation checks by name)
    stats.issue_summary = dict(issue_counts)

    return stats


def _compute_score_distribution(scores: List[int]) -> Dict[str, int]:
    """
    Compute histogram buckets for validation scores.

    Buckets:
    - excellent: 90-100
    - good: 70-89
    - moderate: 50-69
    - poor: 0-49
    """
    distribution = {"excellent": 0, "good": 0, "moderate": 0, "poor": 0}

    for score in scores:
        if score >= 90:
            distribution["excellent"] += 1
        elif score >= 70:
            distribution["good"] += 1
        elif score >= 50:
            distribution["moderate"] += 1
        else:
            distribution["poor"] += 1

    return distribution


class ResultStorage:
    """
    Stores and retrieves batch results in Redis.

    Results are stored with pagination support and expiration.
    """

    RESULT_EXPIRY = 3600  # 1 hour
    VIEW_CACHE_EXPIRY = 300  # 5 minutes for sorted/filtered views
    PAGE_SIZE = 50  # Default page size

    def __init__(self, redis_url: str = None):
        self._redis_url = redis_url or settings.REDIS_URL
        self._redis: Optional[redis.Redis] = None

    def _get_redis(self) -> redis.Redis:
        """Get or create Redis connection."""
        if self._redis is None:
            self._redis = redis.from_url(self._redis_url)
        return self._redis

    def store_results(
        self,
        job_id: str,
        results: List[Dict[str, Any]],
        statistics: BatchStatisticsData,
    ) -> None:
        """
        Store batch results and statistics.

        Args:
            job_id: Job identifier
            results: List of result dictionaries
            statistics: Computed statistics
        """
        r = self._get_redis()

        # Store results as a JSON list (for smaller batches, this is fine)
        # For very large batches (10K+), consider chunked storage
        r.set(
            f"batch:results:{job_id}",
            json.dumps(results),
            ex=self.RESULT_EXPIRY,
        )

        # Store statistics separately
        r.set(
            f"batch:stats:{job_id}",
            json.dumps(asdict(statistics)),
            ex=self.RESULT_EXPIRY,
        )

    # Valid sort fields mapping to value extractors
    SORT_EXTRACTORS = {
        "index": lambda r: r.get("index", 0),
        "name": lambda r: (r.get("name") or "").lower(),
        "smiles": lambda r: r.get("smiles", "").lower(),
        "score": lambda r: (r.get("validation") or {}).get("overall_score", -1),
        "qed": lambda r: ((r.get("scoring") or {}).get("druglikeness") or {}).get(
            "qed_score", -1
        ),
        "safety": lambda r: (r.get("alerts") or {}).get("alert_count", 0),
        "status": lambda r: r.get("status", ""),
        "issues": lambda r: len(
            [
                i
                for i in ((r.get("validation") or {}).get("issues") or [])
                if not i.get("passed", True)
            ]
        ),
    }

    @staticmethod
    def _view_cache_key(
        job_id: str,
        status_filter: Optional[str],
        min_score: Optional[int],
        max_score: Optional[int],
        sort_by: Optional[str],
        sort_dir: Optional[str],
    ) -> str:
        """Build a deterministic Redis key for a filtered+sorted view."""
        params = f"{status_filter}|{min_score}|{max_score}|{sort_by}|{sort_dir}"
        param_hash = hashlib.md5(params.encode()).hexdigest()[:12]
        return f"batch:view:{job_id}:{param_hash}"

    def get_results(
        self,
        job_id: str,
        page: int = 1,
        page_size: int = 50,
        status_filter: Optional[str] = None,
        min_score: Optional[int] = None,
        max_score: Optional[int] = None,
        sort_by: Optional[str] = None,
        sort_dir: Optional[str] = None,
    ) -> Dict[str, Any]:
        """
        Get paginated results for a job with optional filtering and sorting.

        Uses a Redis view cache so that paginating through the same
        filter+sort combo doesn't re-parse, re-filter, or re-sort.

        Args:
            job_id: Job identifier
            page: Page number (1-indexed)
            page_size: Results per page
            status_filter: Filter by status ('success', 'error')
            min_score: Minimum validation score
            max_score: Maximum validation score
            sort_by: Field to sort by (index, name, smiles, score, qed, safety, status, issues)
            sort_dir: Sort direction ('asc' or 'desc')

        Returns:
            Dictionary with results, pagination info, and statistics
        """
        r = self._get_redis()

        empty = {
            "results": [],
            "page": page,
            "page_size": page_size,
            "total_results": 0,
            "total_pages": 0,
        }

        # Check view cache first
        view_key = self._view_cache_key(
            job_id, status_filter, min_score, max_score, sort_by, sort_dir
        )
        cached_view = r.get(view_key)

        if cached_view:
            filtered = json.loads(cached_view)
        else:
            # Cache miss — load raw results, filter, sort, then cache
            results_data = r.get(f"batch:results:{job_id}")
            if not results_data:
                return empty

            results = json.loads(results_data)
            filtered = self._apply_filters(results, status_filter, min_score, max_score)

            if sort_by and sort_by in self.SORT_EXTRACTORS:
                reverse = sort_dir == "desc"
                extractor = self.SORT_EXTRACTORS[sort_by]
                filtered.sort(key=extractor, reverse=reverse)

            # Cache the sorted+filtered view
            r.set(view_key, json.dumps(filtered), ex=self.VIEW_CACHE_EXPIRY)

        # Paginate from the (possibly cached) sorted list
        total_results = len(filtered)
        total_pages = (
            (total_results + page_size - 1) // page_size if total_results > 0 else 0
        )
        start_idx = (page - 1) * page_size
        end_idx = start_idx + page_size
        page_results = filtered[start_idx:end_idx]

        return {
            "results": page_results,
            "page": page,
            "page_size": page_size,
            "total_results": total_results,
            "total_pages": total_pages,
        }

    def get_statistics(self, job_id: str) -> Optional[BatchStatisticsData]:
        """Get statistics for a job."""
        r = self._get_redis()
        data = r.get(f"batch:stats:{job_id}")
        if data:
            return BatchStatisticsData(**json.loads(data))
        return None

    def _apply_filters(
        self,
        results: List[Dict[str, Any]],
        status_filter: Optional[str],
        min_score: Optional[int],
        max_score: Optional[int],
    ) -> List[Dict[str, Any]]:
        """Apply filters to results list."""
        filtered = results

        if status_filter:
            if status_filter == "success":
                filtered = [r for r in filtered if r.get("status") == "success"]
            elif status_filter == "error":
                filtered = [
                    r for r in filtered if r.get("status") == "error" or r.get("error")
                ]

        if min_score is not None:
            filtered = [
                r
                for r in filtered
                if r.get("validation", {}).get("overall_score", 0) >= min_score
            ]

        if max_score is not None:
            filtered = [
                r
                for r in filtered
                if r.get("validation", {}).get("overall_score", 100) <= max_score
            ]

        return filtered

    def delete_results(self, job_id: str) -> None:
        """Delete stored results and any cached views for a job."""
        r = self._get_redis()
        r.delete(f"batch:results:{job_id}")
        r.delete(f"batch:stats:{job_id}")
        # Clean up any cached sorted/filtered views
        for key in r.scan_iter(f"batch:view:{job_id}:*"):
            r.delete(key)


# Singleton instance
result_storage = ResultStorage()
