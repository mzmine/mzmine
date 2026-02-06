"""
Batch processing examples for ChemAudit client.

Demonstrates batch file submission, waiting for completion, and iterating results.
"""

import time
from pathlib import Path
from chemaudit import ChemAuditClient
from chemaudit.exceptions import TimeoutError, APIError

# Configuration
API_BASE_URL = "http://localhost:8000"
API_KEY = None  # Set to your API key for higher rate limits
SAMPLE_CSV = "sample_molecules.csv"


def create_sample_csv():
    """Create a sample CSV file for demonstration."""
    sample_data = """smiles,name
CCO,Ethanol
CC(=O)O,Acetic acid
c1ccccc1,Benzene
CC(C)Cc1ccc(cc1)C(C)C(=O)O,Ibuprofen
CN1C=NC2=C1C(=O)N(C(=O)N2C)C,Caffeine
CC(C)NCC(COc1ccccc1)O,Propranolol
"""
    with open(SAMPLE_CSV, "w") as f:
        f.write(sample_data)
    print(f"Created sample CSV file: {SAMPLE_CSV}")


def main():
    print("ChemAudit Client - Batch Processing Examples\n")
    print("=" * 60)

    # Create sample CSV if it doesn't exist
    if not Path(SAMPLE_CSV).exists():
        create_sample_csv()

    with ChemAuditClient(base_url=API_BASE_URL, api_key=API_KEY) as client:
        # Example 1: Submit batch job
        print("\n1. Submit Batch Job")
        print("-" * 60)
        try:
            response = client.submit_batch(
                file_path=SAMPLE_CSV,
                smiles_column="smiles",
            )
            job_id = response.job_id
            print(f"Job ID: {job_id}")
            print(f"Status: {response.status}")
            print(f"Total Molecules: {response.total_molecules}")
        except Exception as e:
            print(f"Error submitting batch: {e}")
            return

        # Example 2: Check job status
        print("\n2. Check Job Status")
        print("-" * 60)
        status = client.get_batch_status(job_id)
        print(f"Status: {status.status}")
        print(f"Progress: {status.progress}%")
        print(f"Processed: {status.processed}/{status.total}")
        if status.eta_seconds:
            print(f"ETA: {status.eta_seconds} seconds")

        # Example 3: Wait for completion
        print("\n3. Wait for Job Completion")
        print("-" * 60)
        try:
            print("Waiting for job to complete (polling every 2 seconds)...")
            job = client.wait_for_batch(
                job_id,
                poll_interval=2.0,
                timeout=300.0,  # 5 minutes
            )
            print(f"\nJob completed!")
            print(f"Final Status: {job.status}")
            print(f"Processed: {job.processed}/{job.total} molecules")
        except TimeoutError as e:
            print(f"Timeout: {e}")
            return
        except APIError as e:
            print(f"Job failed: {e}")
            return

        # Example 4: Get batch statistics
        print("\n4. Batch Statistics")
        print("-" * 60)
        try:
            stats = client.get_batch_stats(job_id)
            print(f"Total Molecules: {stats.total}")
            print(f"Successful: {stats.successful}")
            print(f"Errors: {stats.errors}")
            if stats.avg_validation_score is not None:
                print(f"Average Validation Score: {stats.avg_validation_score:.2f}")
            if stats.avg_ml_readiness_score is not None:
                print(f"Average ML-Readiness: {stats.avg_ml_readiness_score:.2f}")
            if stats.processing_time_seconds:
                print(f"Processing Time: {stats.processing_time_seconds:.2f} seconds")

            print("\nScore Distribution:")
            for category, count in stats.score_distribution.items():
                print(f"  - {category}: {count}")

            if stats.alert_summary:
                print("\nAlert Summary:")
                for catalog, count in stats.alert_summary.items():
                    print(f"  - {catalog}: {count}")
        except Exception as e:
            print(f"Error getting statistics: {e}")

        # Example 5: Get paginated results
        print("\n5. Get Paginated Results (Page 1)")
        print("-" * 60)
        results = client.get_batch_results(
            job_id,
            page=1,
            page_size=3,
        )
        print(f"Page {results.page} of {results.total_pages}")
        print(f"Showing {len(results.results)} of {results.total_results} results\n")

        for item in results.results:
            print(f"  [{item.index}] {item.smiles}")
            if item.name:
                print(f"      Name: {item.name}")
            print(f"      Status: {item.status}")
            if item.status == "success" and item.validation:
                print(f"      Score: {item.validation.get('overall_score', 'N/A')}")
            elif item.status == "error":
                print(f"      Error: {item.error}")
            print()

        # Example 6: Iterate all results
        print("\n6. Iterate All Results")
        print("-" * 60)
        print("Processing all results...\n")

        high_score_count = 0
        for item in client.iter_batch_results(job_id, page_size=100):
            if item.status == "success" and item.validation:
                score = item.validation.get("overall_score", 0)
                if score >= 80:
                    high_score_count += 1
                    print(f"  ✓ {item.smiles}: {score}")

        print(f"\nHigh-scoring molecules (≥80): {high_score_count}")

        # Example 7: Filter by score
        print("\n7. Filter Results by Score")
        print("-" * 60)
        filtered = client.get_batch_results(
            job_id,
            page=1,
            page_size=10,
            score_min=50,
            score_max=80,
        )
        print(f"Results with scores 50-80: {len(filtered.results)}\n")
        for item in filtered.results[:3]:  # Show first 3
            if item.status == "success" and item.validation:
                print(f"  - {item.smiles}: {item.validation['overall_score']}")

        # Example 8: Export results
        print("\n8. Export Results")
        print("-" * 60)
        for format_type in ["csv", "excel", "json"]:
            try:
                output_path = client.export_batch(
                    job_id,
                    format=format_type,
                    output_path=f"batch_results.{format_type if format_type != 'excel' else 'xlsx'}",
                )
                print(f"  ✓ Exported {format_type.upper()}: {output_path}")
            except Exception as e:
                print(f"  ✗ Export {format_type} failed: {e}")

        # Example 9: Export with filters
        print("\n9. Export Filtered Results")
        print("-" * 60)
        try:
            output_path = client.export_batch(
                job_id,
                format="excel",
                output_path="high_quality_molecules.xlsx",
                score_min=80,
                status="success",
            )
            print(f"  ✓ Exported high-quality molecules: {output_path}")
        except Exception as e:
            print(f"  ✗ Export failed: {e}")

    print("\n" + "=" * 60)
    print("Batch processing examples completed!")
    print(f"\nNote: Clean up the generated files:")
    print(f"  - {SAMPLE_CSV}")
    print(f"  - batch_results.*")
    print(f"  - high_quality_molecules.xlsx")


if __name__ == "__main__":
    main()
