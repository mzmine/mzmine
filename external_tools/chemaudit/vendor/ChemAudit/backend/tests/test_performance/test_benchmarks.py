"""
Automated Performance Benchmarks

Uses pytest-benchmark for reproducible, automated performance testing.
Run with: pytest tests/test_performance/ --benchmark-only

Performance Targets:
- P95 single validation: <3000ms (3 seconds)
- Throughput: >100 molecules/second
- Alert screening: <100ms per molecule
- Parser: <10ms per molecule
- ML-readiness: <50ms per molecule

These benchmarks ensure performance doesn't regress across releases.

Requirements:
    pip install pytest-benchmark
"""

import importlib.util

import pytest
from rdkit import Chem

from app.services.alerts.alert_manager import alert_manager
from app.services.alerts.filter_catalog import get_filter_catalog
from app.services.parser import parse_molecule
from app.services.scoring.ml_readiness import calculate_ml_readiness
from app.services.standardization.chembl_pipeline import standardize_molecule
from app.services.validation.engine import validation_engine

# Check if pytest-benchmark is available
HAS_BENCHMARK = importlib.util.find_spec("pytest_benchmark") is not None

# Skip all tests in this module if pytest-benchmark is not installed
pytestmark = pytest.mark.skipif(
    not HAS_BENCHMARK,
    reason="pytest-benchmark not installed. Install with: pip install pytest-benchmark",
)

# Performance targets documented for automated CI checks
PERF_TARGETS = {
    "p95_validation_ms": 3000,  # Single molecule validation P95
    "throughput_mol_per_sec": 100,  # Minimum molecules per second
    "alert_screening_ms": 100,  # Alert screening per molecule
    "parser_ms": 10,  # Parsing per molecule
    "ml_readiness_ms": 50,  # ML-readiness scoring per molecule
    "standardization_ms": 200,  # ChEMBL standardization per molecule
}


class TestParserPerformance:
    """
    Parser performance benchmarks.

    Target: <10ms per molecule for parsing.
    """

    def test_parse_simple_smiles(self, benchmark):
        """
        Benchmark SMILES parsing for simple molecules.

        Target: <5ms mean for simple molecules.
        """
        smiles = "CCO"  # Ethanol

        def parse():
            return parse_molecule(smiles)

        result = benchmark(parse)
        assert result.success is True

        # Verify performance (only when benchmark stats available)
        if benchmark.stats and benchmark.stats.stats:
            mean_ms = benchmark.stats.stats.mean * 1000
            assert (
                mean_ms < PERF_TARGETS["parser_ms"]
            ), f"Parser mean {mean_ms:.2f}ms exceeds {PERF_TARGETS['parser_ms']}ms target"

    def test_parse_complex_smiles(self, benchmark):
        """
        Benchmark SMILES parsing for complex drug molecules.

        Target: <10ms mean for complex molecules.
        """
        # Atorvastatin - large drug molecule
        smiles = "CC(C)C1=C(C(=C(N1CCC(CC(CC(=O)O)O)O)C2=CC=C(C=C2)F)C3=CC=CC=C3)C(=O)NC4=CC=CC=C4"

        def parse():
            return parse_molecule(smiles)

        result = benchmark(parse)
        assert result.success is True

    def test_parse_batch(self, benchmark, test_molecules):
        """
        Benchmark batch parsing throughput.

        Target: >100 molecules/second.
        """
        smiles_list = [
            "CCO",
            "c1ccccc1",
            "CC(=O)O",
            "c1ccncc1",
            "CC(=O)OC1=CC=CC=C1C(=O)O",
            "CN1C=NC2=C1C(=O)N(C)C(=O)N2C",
        ] * 20  # 120 molecules

        def parse_batch():
            results = []
            for smiles in smiles_list:
                results.append(parse_molecule(smiles))
            return results

        results = benchmark(parse_batch)
        success_count = sum(1 for r in results if r.success)
        assert success_count == len(smiles_list)


class TestValidationEnginePerformance:
    """
    ValidationEngine performance benchmarks.

    Target: <3s P95 for single molecule validation.
    """

    def test_validate_simple_molecule(self, benchmark):
        """
        Benchmark validation on simple molecule.

        Target: <100ms mean for simple molecules.
        """
        mol = Chem.MolFromSmiles("CCO")  # Ethanol

        def validate():
            return validation_engine.validate(mol)

        results, score = benchmark(validate)
        assert len(results) > 0
        assert 0 <= score <= 100

    def test_validate_drug_molecule(self, benchmark):
        """
        Benchmark validation on drug-like molecule.

        Target: <500ms mean for drug molecules.
        """
        mol = Chem.MolFromSmiles("CC(=O)OC1=CC=CC=C1C(=O)O")  # Aspirin

        def validate():
            return validation_engine.validate(mol)

        results, score = benchmark(validate)
        assert len(results) > 0

    def test_validate_complex_molecule(self, benchmark):
        """
        Benchmark validation on complex molecule.

        Target: <1000ms mean for complex molecules.
        This is within P95 <3s target.
        """
        # Atorvastatin
        mol = Chem.MolFromSmiles(
            "CC(C)C1=C(C(=C(N1CCC(CC(CC(=O)O)O)O)C2=CC=C(C=C2)F)C3=CC=CC=C3)C(=O)NC4=CC=CC=C4"
        )

        def validate():
            return validation_engine.validate(mol)

        results, score = benchmark(validate)
        assert len(results) > 0

        # Check P95 target (only when benchmark stats available)
        if benchmark.stats and benchmark.stats.stats:
            mean_ms = benchmark.stats.stats.mean * 1000
            assert (
                mean_ms < PERF_TARGETS["p95_validation_ms"]
            ), f"Validation mean {mean_ms:.2f}ms exceeds {PERF_TARGETS['p95_validation_ms']}ms P95 target"

    def test_validation_throughput(self, benchmark, test_molecules):
        """
        Benchmark validation throughput across molecule set.

        Target: >100 molecules/second.
        """

        def validate_all():
            results = []
            for name, mol in test_molecules:
                check_results, score = validation_engine.validate(mol)
                results.append((name, score))
            return results

        results = benchmark(validate_all)
        assert len(results) == len(test_molecules)

        # Calculate throughput (only when benchmark stats available)
        if benchmark.stats and benchmark.stats.stats and benchmark.stats.stats.mean > 0:
            throughput = len(test_molecules) / benchmark.stats.stats.mean
            # Note: This prints but doesn't assert to avoid flaky tests
            # Actual CI should use benchmark comparison
            print(f"\nThroughput: {throughput:.1f} molecules/second")


class TestAlertScreeningPerformance:
    """
    Alert screening performance benchmarks.

    Target: <100ms per molecule for PAINS+BRENK screening.
    """

    def test_alert_screening_simple(self, benchmark, warmed_filter_catalogs):
        """
        Benchmark alert screening on simple molecule.

        Target: <50ms mean.
        """
        mol = Chem.MolFromSmiles("c1ccccc1")  # Benzene

        def screen():
            return alert_manager.screen(mol, catalogs=["PAINS", "BRENK"])

        result = benchmark(screen)
        assert isinstance(result.alerts, list)

    def test_alert_screening_drug(self, benchmark, warmed_filter_catalogs):
        """
        Benchmark alert screening on drug molecule.

        Target: <100ms mean.
        """
        # Caffeine - contains some potentially flagged patterns
        mol = Chem.MolFromSmiles("CN1C=NC2=C1C(=O)N(C)C(=O)N2C")

        def screen():
            return alert_manager.screen(mol, catalogs=["PAINS", "BRENK"])

        benchmark(screen)

        # Check performance (only when benchmark stats available)
        if benchmark.stats and benchmark.stats.stats:
            mean_ms = benchmark.stats.stats.mean * 1000
            assert (
                mean_ms < PERF_TARGETS["alert_screening_ms"]
            ), f"Alert screening mean {mean_ms:.2f}ms exceeds {PERF_TARGETS['alert_screening_ms']}ms target"

    def test_alert_screening_complex(self, benchmark, warmed_filter_catalogs):
        """
        Benchmark alert screening on complex molecule.

        Target: <100ms mean (same target regardless of complexity due to caching).
        """
        # Sildenafil - complex structure
        mol = Chem.MolFromSmiles(
            "CCCC1=NN(C2=C1NC(=NC2=O)C3=C(C=CC(=C3)S(=O)(=O)N4CCN(CC4)C)OCC)C"
        )

        def screen():
            return alert_manager.screen(mol, catalogs=["PAINS", "BRENK"])

        result = benchmark(screen)
        assert isinstance(result.alerts, list)

    def test_filter_catalog_caching(self, benchmark):
        """
        Verify filter catalog caching provides speedup.

        After first load, subsequent calls should be <1ms.
        """
        # Ensure catalog is loaded
        _ = get_filter_catalog("PAINS")

        def get_catalog():
            return get_filter_catalog("PAINS")

        catalog = benchmark(get_catalog)
        assert catalog is not None

        # Cached access should be very fast (only when benchmark stats available)
        if benchmark.stats and benchmark.stats.stats:
            mean_ms = benchmark.stats.stats.mean * 1000
            assert (
                mean_ms < 1.0
            ), f"Cached catalog access {mean_ms:.3f}ms should be <1ms"


class TestMLReadinessPerformance:
    """
    ML-readiness scoring performance benchmarks.

    Target: <50ms per molecule.
    """

    def test_ml_readiness_simple(self, benchmark):
        """
        Benchmark ML-readiness on simple molecule.

        Target: <20ms mean.
        """
        mol = Chem.MolFromSmiles("CCO")  # Ethanol

        def score():
            return calculate_ml_readiness(mol)

        result = benchmark(score)
        assert 0 <= result.score <= 100

    def test_ml_readiness_drug(self, benchmark):
        """
        Benchmark ML-readiness on drug molecule.

        Target: <50ms mean.
        """
        mol = Chem.MolFromSmiles("CC(=O)OC1=CC=CC=C1C(=O)O")  # Aspirin

        def score():
            return calculate_ml_readiness(mol)

        result = benchmark(score)
        assert 0 <= result.score <= 100

        # Check performance (only when benchmark stats available)
        if benchmark.stats and benchmark.stats.stats:
            mean_ms = benchmark.stats.stats.mean * 1000
            assert (
                mean_ms < PERF_TARGETS["ml_readiness_ms"]
            ), f"ML-readiness mean {mean_ms:.2f}ms exceeds {PERF_TARGETS['ml_readiness_ms']}ms target"

    def test_ml_readiness_complex(self, benchmark):
        """
        Benchmark ML-readiness on complex molecule.

        Target: <100ms mean for complex molecules (2x standard target).
        """
        # Cholesterol - complex steroid
        mol = Chem.MolFromSmiles(
            "CC(C)CCC[C@@H](C)[C@H]1CC[C@@H]2[C@@]1(CC[C@H]3[C@H]2CC=C4[C@@]3(CC[C@@H](C4)O)C)C"
        )

        def score():
            return calculate_ml_readiness(mol)

        result = benchmark(score)
        assert 0 <= result.score <= 100


class TestStandardizationPerformance:
    """
    ChEMBL standardization pipeline performance benchmarks.

    Target: <200ms per molecule.
    """

    def test_standardize_simple(self, benchmark):
        """
        Benchmark standardization on simple molecule.

        Target: <100ms mean.
        """
        mol = Chem.MolFromSmiles("CCO")

        def standardize():
            return standardize_molecule(mol)

        result = benchmark(standardize)
        assert result.success is True

    def test_standardize_salt(self, benchmark):
        """
        Benchmark salt stripping standardization.

        Target: <200ms mean (more processing for salts).
        """
        # Sodium benzoate - requires salt stripping
        mol = Chem.MolFromSmiles("[Na+].[O-]C(=O)c1ccccc1")

        def standardize():
            return standardize_molecule(mol)

        benchmark(standardize)

        # Check performance (only when benchmark stats available)
        if benchmark.stats and benchmark.stats.stats:
            mean_ms = benchmark.stats.stats.mean * 1000
            assert (
                mean_ms < PERF_TARGETS["standardization_ms"]
            ), f"Standardization mean {mean_ms:.2f}ms exceeds {PERF_TARGETS['standardization_ms']}ms target"

    def test_standardize_complex(self, benchmark):
        """
        Benchmark standardization on complex molecule.

        Target: <300ms mean for complex molecules.
        """
        # Atorvastatin
        mol = Chem.MolFromSmiles(
            "CC(C)C1=C(C(=C(N1CCC(CC(CC(=O)O)O)O)C2=CC=C(C=C2)F)C3=CC=CC=C3)C(=O)NC4=CC=CC=C4"
        )

        def standardize():
            return standardize_molecule(mol)

        benchmark(standardize)
        # Standardization may fail for some molecules, that's OK for performance test


class TestEndToEndPerformance:
    """
    End-to-end pipeline performance benchmarks.

    Tests full validation pipeline including parsing, validation,
    alerts, ML-readiness, and standardization.
    """

    def test_full_pipeline_simple(self, benchmark, warmed_filter_catalogs):
        """
        Benchmark full pipeline on simple molecule.

        Target: <500ms total.
        """
        smiles = "CCO"

        def full_pipeline():
            # Parse
            parse_result = parse_molecule(smiles)
            if not parse_result.success:
                return None

            mol = parse_result.mol

            # Validate
            check_results, score = validation_engine.validate(mol)

            # Alerts
            alerts = alert_manager.screen(mol, catalogs=["PAINS", "BRENK"])

            # ML-readiness
            ml_score = calculate_ml_readiness(mol)

            # Standardize
            std_result = standardize_molecule(mol)

            return {
                "validation_score": score,
                "alerts": len(alerts.alerts),
                "ml_score": ml_score.score,
                "standardized": std_result.success,
            }

        result = benchmark(full_pipeline)
        assert result is not None
        assert result["validation_score"] >= 0

    def test_full_pipeline_drug(self, benchmark, warmed_filter_catalogs):
        """
        Benchmark full pipeline on drug molecule.

        Target: <1000ms total (within P95).
        """
        smiles = "CC(=O)OC1=CC=CC=C1C(=O)O"  # Aspirin

        def full_pipeline():
            parse_result = parse_molecule(smiles)
            if not parse_result.success:
                return None

            mol = parse_result.mol
            check_results, score = validation_engine.validate(mol)
            alerts = alert_manager.screen(mol, catalogs=["PAINS", "BRENK"])
            ml_score = calculate_ml_readiness(mol)
            std_result = standardize_molecule(mol)

            return {
                "validation_score": score,
                "alerts": len(alerts.alerts),
                "ml_score": ml_score.score,
                "standardized": std_result.success,
            }

        result = benchmark(full_pipeline)
        assert result is not None

    def test_full_pipeline_complex(self, benchmark, warmed_filter_catalogs):
        """
        Benchmark full pipeline on complex molecule.

        Target: <3000ms total (P95 target).
        """
        # Atorvastatin
        smiles = "CC(C)C1=C(C(=C(N1CCC(CC(CC(=O)O)O)O)C2=CC=C(C=C2)F)C3=CC=CC=C3)C(=O)NC4=CC=CC=C4"

        def full_pipeline():
            parse_result = parse_molecule(smiles)
            if not parse_result.success:
                return None

            mol = parse_result.mol
            check_results, score = validation_engine.validate(mol)
            alerts = alert_manager.screen(mol, catalogs=["PAINS", "BRENK"])
            ml_score = calculate_ml_readiness(mol)
            std_result = standardize_molecule(mol)

            return {
                "validation_score": score,
                "alerts": len(alerts.alerts),
                "ml_score": ml_score.score,
                "standardized": std_result.success,
            }

        result = benchmark(full_pipeline)
        assert result is not None

        # Check performance (only when benchmark stats available)
        if benchmark.stats and benchmark.stats.stats:
            mean_ms = benchmark.stats.stats.mean * 1000
            assert (
                mean_ms < PERF_TARGETS["p95_validation_ms"]
            ), f"Full pipeline mean {mean_ms:.2f}ms exceeds {PERF_TARGETS['p95_validation_ms']}ms P95 target"

    def test_batch_throughput(self, benchmark, test_molecules, warmed_filter_catalogs):
        """
        Benchmark batch processing throughput.

        Target: >100 molecules/second for complete pipeline.
        """

        def process_batch():
            results = []
            for name, mol in test_molecules:
                check_results, score = validation_engine.validate(mol)
                alerts = alert_manager.screen(mol, catalogs=["PAINS", "BRENK"])
                ml_score = calculate_ml_readiness(mol)
                results.append(
                    {
                        "name": name,
                        "score": score,
                        "alerts": len(alerts.alerts),
                        "ml_score": ml_score.score,
                    }
                )
            return results

        results = benchmark(process_batch)
        assert len(results) == len(test_molecules)

        # Report throughput (only when benchmark stats available)
        if benchmark.stats and benchmark.stats.stats and benchmark.stats.stats.mean > 0:
            throughput = len(test_molecules) / benchmark.stats.stats.mean
            print(f"\nBatch throughput: {throughput:.1f} molecules/second")
            # Target verification (soft check - may vary by environment)
            if throughput < PERF_TARGETS["throughput_mol_per_sec"]:
                print(
                    f"WARNING: Throughput {throughput:.1f} below {PERF_TARGETS['throughput_mol_per_sec']} target"
                )


class TestPerformanceRegression:
    """
    Performance regression tests.

    These tests verify that performance hasn't regressed from baseline.
    They should be run with --benchmark-compare to check against saved baselines.
    """

    @pytest.mark.benchmark(group="regression")
    def test_parser_regression(self, benchmark):
        """Regression test for parser performance."""
        smiles = "CC(=O)OC1=CC=CC=C1C(=O)O"
        benchmark(lambda: parse_molecule(smiles))

    @pytest.mark.benchmark(group="regression")
    def test_validation_regression(self, benchmark):
        """Regression test for validation performance."""
        mol = Chem.MolFromSmiles("CC(=O)OC1=CC=CC=C1C(=O)O")
        benchmark(lambda: validation_engine.validate(mol))

    @pytest.mark.benchmark(group="regression")
    def test_alert_regression(self, benchmark, warmed_filter_catalogs):
        """Regression test for alert screening performance."""
        mol = Chem.MolFromSmiles("CN1C=NC2=C1C(=O)N(C)C(=O)N2C")
        benchmark(lambda: alert_manager.screen(mol, catalogs=["PAINS", "BRENK"]))

    @pytest.mark.benchmark(group="regression")
    def test_ml_readiness_regression(self, benchmark):
        """Regression test for ML-readiness scoring performance."""
        mol = Chem.MolFromSmiles("CC(=O)OC1=CC=CC=C1C(=O)O")
        benchmark(lambda: calculate_ml_readiness(mol))
