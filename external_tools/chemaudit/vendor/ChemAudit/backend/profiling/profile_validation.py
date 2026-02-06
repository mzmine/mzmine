#!/usr/bin/env python3
"""
Validation Profiling Script

Profiles single molecule validation performance using pyinstrument.
Outputs both console summary and HTML report.

Usage:
    python -m profiling.profile_validation [--iterations N] [--output-dir DIR]

Requirements:
    pip install pyinstrument
"""

import argparse
import sys
import time
from pathlib import Path
from statistics import mean, median, stdev
from typing import List, Tuple

# Add backend to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))


from app.services.alerts.alert_manager import alert_manager
from app.services.alerts.filter_catalog import get_filter_catalog
from app.services.parser.molecule_parser import parse_molecule
from app.services.scoring.ml_readiness import calculate_ml_readiness
from app.services.standardization.chembl_pipeline import standardize_molecule
from app.services.validation.engine import validation_engine

# Diverse test set covering various molecule types and complexities
TEST_MOLECULES: List[Tuple[str, str]] = [
    # Simple drug-like molecules
    ("aspirin", "CC(=O)OC1=CC=CC=C1C(=O)O"),
    ("caffeine", "CN1C=NC2=C1C(=O)N(C)C(=O)N2C"),
    ("ibuprofen", "CC(C)CC1=CC=C(C=C1)C(C)C(=O)O"),
    # Complex drug-like molecules
    (
        "atorvastatin",
        "CC(C)C1=C(C(=C(N1CCC(CC(CC(=O)O)O)O)C2=CC=C(C=C2)F)C3=CC=CC=C3)C(=O)NC4=CC=CC=C4",
    ),
    (
        "ritonavir",
        "CC(C)C(NC(=O)N(C)CC1=CSC(=N1)C(C)C)C(=O)NC(CC(O)C(CC2=CC=CC=C2)NC(=O)OCC3=CN=CS3)CC4=CC=CC=C4",
    ),
    # Natural products
    ("quercetin", "OC1=CC(=C2C(=O)C(=C(OC2=C1)C3=CC=C(O)C(=C3)O)O)O"),
    ("resveratrol", "OC1=CC=C(C=C1)/C=C/C2=CC(=CC(=C2)O)O"),
    # Challenging structures
    ("metal_complex", "[Na+].[O-]C(=O)C1=CC=CC=C1"),  # Sodium benzoate
    ("stereochemistry", "C[C@H](O)[C@@H](O)C"),  # Meso compound
    ("macrocycle", "C1CCCCCCCCCCCC1"),  # 13-membered ring
    # Edge cases
    ("single_atom", "[Fe]"),
    ("small_fragment", "C"),
    ("aromatic_heterocycle", "C1=CN=CC=C1"),  # Pyridine
]


def profile_single_validation(smiles: str, name: str) -> dict:
    """
    Profile a single molecule validation.

    Returns:
        Dict with timing for each stage
    """
    timings = {"name": name, "smiles": smiles}

    # Parse
    start = time.perf_counter()
    result = parse_molecule(smiles)
    timings["parse_ms"] = (time.perf_counter() - start) * 1000

    if not result.success or result.mol is None:
        timings["error"] = "Parse failed"
        return timings

    mol = result.mol

    # Validation engine
    start = time.perf_counter()
    results, score = validation_engine.validate(mol)
    timings["validation_ms"] = (time.perf_counter() - start) * 1000

    # Alert screening
    start = time.perf_counter()
    _ = alert_manager.screen(mol, catalogs=["PAINS", "BRENK"])
    timings["alerts_ms"] = (time.perf_counter() - start) * 1000

    # ML-readiness scoring
    start = time.perf_counter()
    _ = calculate_ml_readiness(mol)
    timings["ml_scoring_ms"] = (time.perf_counter() - start) * 1000

    # Standardization
    start = time.perf_counter()
    _ = standardize_molecule(mol)
    timings["standardization_ms"] = (time.perf_counter() - start) * 1000

    # Total
    timings["total_ms"] = (
        timings["parse_ms"]
        + timings["validation_ms"]
        + timings["alerts_ms"]
        + timings["ml_scoring_ms"]
        + timings["standardization_ms"]
    )

    return timings


def run_benchmark(iterations: int = 3, warmup: int = 1) -> dict:
    """
    Run full benchmark across test molecules.

    Args:
        iterations: Number of iterations per molecule
        warmup: Number of warmup iterations (not counted)

    Returns:
        Benchmark results dictionary
    """
    print(
        f"Running benchmark: {len(TEST_MOLECULES)} molecules x {iterations} iterations"
    )
    print(f"Warmup iterations: {warmup}")
    print("-" * 60)

    # Warmup - pre-load catalogs and caches
    print("Warming up caches...")
    for name, smiles in TEST_MOLECULES[:3]:
        for _ in range(warmup):
            profile_single_validation(smiles, name)

    # Pre-load filter catalogs
    get_filter_catalog("PAINS")
    get_filter_catalog("BRENK")
    print("Warmup complete.\n")

    all_timings = []

    for name, smiles in TEST_MOLECULES:
        molecule_timings = []
        for i in range(iterations):
            timing = profile_single_validation(smiles, name)
            if "error" not in timing:
                molecule_timings.append(timing)

        if molecule_timings:
            avg_timing = {
                "name": name,
                "smiles": smiles,
                "iterations": len(molecule_timings),
                "parse_ms": mean([t["parse_ms"] for t in molecule_timings]),
                "validation_ms": mean([t["validation_ms"] for t in molecule_timings]),
                "alerts_ms": mean([t["alerts_ms"] for t in molecule_timings]),
                "ml_scoring_ms": mean([t["ml_scoring_ms"] for t in molecule_timings]),
                "standardization_ms": mean(
                    [t["standardization_ms"] for t in molecule_timings]
                ),
                "total_ms": mean([t["total_ms"] for t in molecule_timings]),
            }
            all_timings.append(avg_timing)
            print(
                f"  {name}: {avg_timing['total_ms']:.2f}ms (parse: {avg_timing['parse_ms']:.2f}, "
                f"validate: {avg_timing['validation_ms']:.2f}, alerts: {avg_timing['alerts_ms']:.2f})"
            )

    # Calculate aggregate statistics
    if not all_timings:
        return {"error": "No successful validations"}

    total_times = [t["total_ms"] for t in all_timings]

    results = {
        "molecules_tested": len(TEST_MOLECULES),
        "successful_validations": len(all_timings),
        "iterations_per_molecule": iterations,
        "aggregate": {
            "mean_ms": mean(total_times),
            "median_ms": median(total_times),
            "min_ms": min(total_times),
            "max_ms": max(total_times),
            "stdev_ms": stdev(total_times) if len(total_times) > 1 else 0,
        },
        "by_stage": {
            "parse_ms": mean([t["parse_ms"] for t in all_timings]),
            "validation_ms": mean([t["validation_ms"] for t in all_timings]),
            "alerts_ms": mean([t["alerts_ms"] for t in all_timings]),
            "ml_scoring_ms": mean([t["ml_scoring_ms"] for t in all_timings]),
            "standardization_ms": mean([t["standardization_ms"] for t in all_timings]),
        },
        "per_molecule": all_timings,
    }

    # Calculate throughput
    results["throughput"] = {
        "molecules_per_second": 1000 / results["aggregate"]["mean_ms"],
        "p95_target_ms": 3000,  # 3 seconds target
        "p95_met": results["aggregate"]["max_ms"] < 3000,
    }

    return results


def print_results(results: dict):
    """Print benchmark results in a formatted way."""
    print("\n" + "=" * 60)
    print("BENCHMARK RESULTS")
    print("=" * 60)

    print(f"\nMolecules tested: {results['molecules_tested']}")
    print(f"Successful: {results['successful_validations']}")
    print(f"Iterations per molecule: {results['iterations_per_molecule']}")

    print("\n--- Aggregate Timing ---")
    agg = results["aggregate"]
    print(f"  Mean:   {agg['mean_ms']:.2f} ms")
    print(f"  Median: {agg['median_ms']:.2f} ms")
    print(f"  Min:    {agg['min_ms']:.2f} ms")
    print(f"  Max:    {agg['max_ms']:.2f} ms")
    print(f"  StdDev: {agg['stdev_ms']:.2f} ms")

    print("\n--- Timing by Stage ---")
    stage = results["by_stage"]
    total = sum(stage.values())
    for name, ms in stage.items():
        pct = (ms / total) * 100 if total > 0 else 0
        print(f"  {name.replace('_ms', ''):20s}: {ms:7.2f} ms ({pct:5.1f}%)")

    print("\n--- Throughput ---")
    tp = results["throughput"]
    print(f"  Molecules/second: {tp['molecules_per_second']:.1f}")
    print(f"  P95 target: {tp['p95_target_ms']} ms")
    print(f"  P95 met: {'YES' if tp['p95_met'] else 'NO'}")

    print("\n--- Slowest Molecules ---")
    sorted_mols = sorted(
        results["per_molecule"], key=lambda x: x["total_ms"], reverse=True
    )
    for mol in sorted_mols[:5]:
        print(f"  {mol['name']:20s}: {mol['total_ms']:.2f} ms")


def profile_with_pyinstrument(output_dir: Path = None):
    """
    Run profiling with pyinstrument and save HTML report.

    Args:
        output_dir: Directory to save HTML report
    """
    try:
        from pyinstrument import Profiler
    except ImportError:
        print("pyinstrument not installed. Run: pip install pyinstrument")
        print("Falling back to basic timing...")
        return run_benchmark(iterations=3)

    profiler = Profiler()

    print("Running profiled benchmark...")
    profiler.start()
    results = run_benchmark(iterations=3)
    profiler.stop()

    # Print console output
    print(profiler.output_text(unicode=True, color=True))

    # Save HTML report
    if output_dir:
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        html_path = output_dir / "profile_report.html"
        with open(html_path, "w") as f:
            f.write(profiler.output_html())
        print(f"\nHTML report saved to: {html_path}")

    return results


def main():
    parser = argparse.ArgumentParser(description="Profile validation performance")
    parser.add_argument(
        "--iterations", type=int, default=3, help="Iterations per molecule"
    )
    parser.add_argument("--output-dir", type=str, help="Output directory for reports")
    parser.add_argument(
        "--pyinstrument", action="store_true", help="Use pyinstrument profiler"
    )
    args = parser.parse_args()

    output_dir = Path(args.output_dir) if args.output_dir else Path(__file__).parent

    if args.pyinstrument:
        results = profile_with_pyinstrument(output_dir)
    else:
        results = run_benchmark(iterations=args.iterations)

    print_results(results)

    # Return exit code based on performance target
    if results.get("throughput", {}).get("p95_met", False):
        print("\n[PASS] Performance target met!")
        return 0
    else:
        print("\n[WARN] Performance target NOT met - optimization needed")
        return 1


if __name__ == "__main__":
    sys.exit(main())
