#!/usr/bin/env python3
"""
MZmine ChemAudit Validation Wrapper

Validates and scores chemical structures predicted by DiffMS.
Provides structure validation, ML-readiness scoring, and alert screening.

Input: JSON file with array of structures to validate
Output: JSON results to stdout

Usage:
    python mzmine_chemaudit_validate.py --input INPUT_JSON [--chemaudit-dir DIR] [OPTIONS]

Example Input JSON:
    [
        {"row_id": 123, "rank": 1, "smiles": "CCO", "formula": "C2H6O"},
        {"row_id": 123, "rank": 2, "smiles": "CC(O)C", "formula": "C3H8O"}
    ]

Example Output JSON:
    [
        {
            "row_id": 123,
            "rank": 1,
            "smiles": "CCO",
            "valid": true,
            "validation_score": 95,
            "ml_readiness_score": 88,
            "ml_interpretation": "Excellent ML-readiness. 451/451 descriptors...",
            "alerts": [],
            "standardized_smiles": "CCO",
            "failed_checks": [],
            "quality_category": "excellent"
        }
    ]
"""

import sys
import json
import argparse
import os
import warnings
from pathlib import Path
from typing import List, Dict, Any, Optional

# Suppress all Python warnings to keep stdout clean for JSON output
warnings.filterwarnings('ignore')

# Suppress RDKit warnings that go to stdout/stderr
os.environ['RDKIT_QUIET'] = '1'

# Add ChemAudit backend to Python path
def setup_chemaudit_path(chemaudit_dir: Optional[str] = None):
    """Add ChemAudit backend to Python path."""
    if chemaudit_dir:
        backend_path = Path(chemaudit_dir) / "backend"
    else:
        # Try to find ChemAudit relative to this script
        script_dir = Path(__file__).parent.parent
        backend_path = script_dir / "vendor" / "ChemAudit" / "backend"
    
    if not backend_path.exists():
        print(f"MZMINE_CHEMAUDIT_ERROR ChemAudit backend not found at: {backend_path}", file=sys.stderr)
        print(f"MZMINE_CHEMAUDIT_ERROR Script location: {Path(__file__).parent}", file=sys.stderr)
        print(f"MZMINE_CHEMAUDIT_ERROR Expected backend at: {backend_path}", file=sys.stderr)
        raise FileNotFoundError(f"ChemAudit backend not found at: {backend_path}")
    
    sys.path.insert(0, str(backend_path))
    print(f"MZMINE_CHEMAUDIT_LOG Added ChemAudit backend to path: {backend_path}", file=sys.stderr)


# Import after path setup
def import_chemaudit_modules():
    """Import ChemAudit modules after path is set up."""
    try:
        from rdkit import Chem
        from rdkit import RDLogger
    except ImportError as e:
        print(f"MZMINE_CHEMAUDIT_ERROR Failed to import RDKit: {e}", file=sys.stderr)
        raise
    
    try:
        from app.services.validation.engine import validation_engine
        from app.services.scoring.ml_readiness import calculate_ml_readiness
        from app.services.alerts.alert_manager import alert_manager
        from app.services.standardization.chembl_pipeline import (
            standardize_molecule,
            StandardizationOptions
        )
        print("MZMINE_CHEMAUDIT_LOG Imported ChemAudit services", file=sys.stderr)
    except ImportError as e:
        print(f"MZMINE_CHEMAUDIT_ERROR Failed to import ChemAudit services: {e}", file=sys.stderr)
        print(f"MZMINE_CHEMAUDIT_ERROR Python path: {sys.path}", file=sys.stderr)
        raise
    
    return Chem, validation_engine, calculate_ml_readiness, alert_manager, standardize_molecule, StandardizationOptions


class ChemAuditValidator:
    """Validates and scores molecules for MZmine."""
    
    def __init__(self, 
                 min_validation_score: int = 60,
                 min_ml_score: int = 40,
                 enable_alerts: bool = True,
                 enable_standardization: bool = True,
                 alert_catalogs: Optional[List[str]] = None):
        """
        Initialize validator.
        
        Args:
            min_validation_score: Minimum validation score (0-100) to pass
            min_ml_score: Minimum ML-readiness score (0-100) to pass
            enable_alerts: Whether to screen for structural alerts
            enable_standardization: Whether to apply ChEMBL standardization
            alert_catalogs: List of catalogs to screen (default: ["PAINS", "BRENK"])
        """
        self.min_validation_score = min_validation_score
        self.min_ml_score = min_ml_score
        self.enable_alerts = enable_alerts
        self.enable_standardization = enable_standardization
        self.alert_catalogs = alert_catalogs or ["PAINS", "BRENK"]
        
        # Import ChemAudit modules
        (self.Chem, self.validation_engine, self.calculate_ml_readiness, 
         self.alert_manager, self.standardize_molecule, 
         self.StandardizationOptions) = import_chemaudit_modules()
        
    def validate_smiles(self, smiles: str, row_id: Optional[int] = None, 
                       rank: Optional[int] = None, formula: Optional[str] = None) -> Dict[str, Any]:
        """
        Validate a single SMILES structure.
        
        Args:
            smiles: SMILES string to validate
            row_id: Feature row ID (for tracking)
            rank: Rank of this structure (for tracking)
            formula: Expected molecular formula (for validation)
            
        Returns:
            Dictionary with validation results
        """
        result = {
            "smiles": smiles,
            "row_id": row_id,
            "rank": rank,
            "valid": False,
            "validation_score": 0,
            "ml_readiness_score": 0,
            "ml_interpretation": "",
            "alerts": [],
            "standardized_smiles": smiles,
            "failed_checks": [],
            "quality_category": "invalid",
            "error": None
        }
        
        try:
            # Parse SMILES
            mol = self.Chem.MolFromSmiles(smiles)
            if mol is None:
                result["error"] = "Failed to parse SMILES"
                return result
            
            # Run validation checks
            try:
                checks, validation_score = self.validation_engine.validate(mol)
                result["validation_score"] = validation_score
                result["failed_checks"] = [
                    {
                        "name": check.check_name,
                        "severity": check.severity.value,
                        "message": check.message
                    }
                    for check in checks if not check.passed
                ]
            except Exception as e:
                result["error"] = f"Validation failed: {str(e)}"
                return result
            
            # Calculate ML-readiness score
            try:
                ml_result = self.calculate_ml_readiness(mol)
                result["ml_readiness_score"] = ml_result.score
                result["ml_interpretation"] = ml_result.interpretation
                result["ml_breakdown"] = {
                    "descriptors_score": ml_result.breakdown.descriptors_score,
                    "descriptors_successful": ml_result.breakdown.descriptors_successful,
                    "descriptors_total": ml_result.breakdown.descriptors_total,
                    "fingerprints_score": ml_result.breakdown.fingerprints_score,
                    "fingerprints_successful": ml_result.breakdown.fingerprints_successful,
                    "size_score": ml_result.breakdown.size_score,
                    "molecular_weight": ml_result.breakdown.molecular_weight,
                    "num_atoms": ml_result.breakdown.num_atoms
                }
            except Exception as e:
                result["error"] = f"ML-readiness calculation failed: {str(e)}"
                return result
            
            # Screen for structural alerts
            if self.enable_alerts:
                try:
                    alert_result = self.alert_manager.screen(mol, catalogs=self.alert_catalogs)
                    result["alerts"] = [
                        {
                            "pattern": alert.pattern_name,
                            "severity": alert.severity.value,
                            "catalog": alert.catalog_source,
                            "description": alert.description,
                            "matched_atoms": alert.matched_atoms
                        }
                        for alert in alert_result.alerts
                    ]
                except Exception as e:
                    # Alerts are non-critical, log but continue
                    result["alerts"] = []
            
            # Apply standardization
            standardized_smiles = smiles
            if self.enable_standardization:
                try:
                    std_options = self.StandardizationOptions(
                        include_tautomer=False,  # Don't lose E/Z stereochemistry
                        preserve_stereo=True,
                        return_excluded_fragments=True
                    )
                    std_result = self.standardize_molecule(mol, std_options)
                    
                    if std_result.success and std_result.standardized_smiles:
                        standardized_smiles = std_result.standardized_smiles
                        result["standardization"] = {
                            "success": True,
                            "excluded_fragments": std_result.excluded_fragments,
                            "mass_change_percent": std_result.mass_change_percent
                        }
                except Exception as e:
                    # Standardization failure is not critical
                    result["standardization"] = {
                        "success": False,
                        "error": str(e)
                    }
            
            result["standardized_smiles"] = standardized_smiles
            
            # Determine if structure passes quality thresholds
            passes_validation = validation_score >= self.min_validation_score
            passes_ml = ml_result.score >= self.min_ml_score
            has_critical_alerts = any(
                a["severity"] == "critical" for a in result["alerts"]
            )
            
            result["valid"] = passes_validation and passes_ml and not has_critical_alerts
            result["quality_category"] = self._get_quality_category(
                validation_score, ml_result.score, len(result["alerts"]), has_critical_alerts
            )
            
        except Exception as e:
            result["error"] = f"Unexpected error: {str(e)}"
            import traceback
            result["traceback"] = traceback.format_exc()
        
        return result
    
    def _get_quality_category(self, val_score: int, ml_score: int, 
                             alert_count: int, has_critical: bool) -> str:
        """
        Categorize overall quality.
        
        Args:
            val_score: Validation score (0-100)
            ml_score: ML-readiness score (0-100)
            alert_count: Number of structural alerts
            has_critical: Whether critical alerts are present
            
        Returns:
            Quality category string
        """
        if has_critical:
            return "poor"
        
        avg_score = (val_score + ml_score) / 2
        
        if avg_score >= 80 and alert_count == 0:
            return "excellent"
        elif avg_score >= 60 and alert_count <= 2:
            return "good"
        elif avg_score >= 40:
            return "moderate"
        else:
            return "poor"
    
    def validate_batch(self, structures: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Validate a batch of structures.
        
        Args:
            structures: List of structure dicts with "smiles", optional "row_id", "rank", "formula"
            
        Returns:
            List of validation result dicts
        """
        results = []
        total = len(structures)
        
        for i, item in enumerate(structures, 1):
            smiles = item.get("smiles")
            if not smiles:
                continue
            
            # Progress reporting for MZmine
            print(f"MZMINE_CHEMAUDIT_PROGRESS {i}/{total}", file=sys.stderr)
            
            result = self.validate_smiles(
                smiles,
                row_id=item.get("row_id"),
                rank=item.get("rank"),
                formula=item.get("formula")
            )
            results.append(result)
        
        return results


def main():
    """Main entry point for MZmine integration."""
    parser = argparse.ArgumentParser(
        description="Validate chemical structures using ChemAudit for MZmine"
    )
    parser.add_argument(
        "--input", required=True,
        help="Input JSON file with structures to validate"
    )
    parser.add_argument(
        "--output", 
        help="Output JSON file (default: stdout)"
    )
    parser.add_argument(
        "--chemaudit-dir",
        help="Path to ChemAudit root directory (auto-detected if not specified)"
    )
    parser.add_argument(
        "--min-validation-score", type=int, default=60,
        help="Minimum validation score (0-100) to pass (default: 60)"
    )
    parser.add_argument(
        "--min-ml-score", type=int, default=40,
        help="Minimum ML-readiness score (0-100) to pass (default: 40)"
    )
    parser.add_argument(
        "--disable-alerts", action="store_true",
        help="Disable structural alert screening"
    )
    parser.add_argument(
        "--disable-standardization", action="store_true",
        help="Disable ChEMBL standardization"
    )
    parser.add_argument(
        "--alert-catalogs", nargs="+", default=["PAINS", "BRENK"],
        help="Alert catalogs to screen (default: PAINS BRENK)"
    )
    
    args = parser.parse_args()
    
    try:
        # Set up ChemAudit Python path
        print("MZMINE_CHEMAUDIT_LOG Setting up ChemAudit environment...", file=sys.stderr)
        setup_chemaudit_path(args.chemaudit_dir)
        
        # Load input data
        print("MZMINE_CHEMAUDIT_LOG Loading input structures...", file=sys.stderr)
        with open(args.input, 'r') as f:
            input_data = json.load(f)
        
        print(f"MZMINE_CHEMAUDIT_LOG Validating {len(input_data)} structures...", file=sys.stderr)
        
        # Create validator
        try:
            validator = ChemAuditValidator(
                min_validation_score=args.min_validation_score,
                min_ml_score=args.min_ml_score,
                enable_alerts=not args.disable_alerts,
                enable_standardization=not args.disable_standardization,
                alert_catalogs=args.alert_catalogs
            )
            print("MZMINE_CHEMAUDIT_LOG ChemAudit validator initialized", file=sys.stderr)
        except Exception as e:
            print(f"MZMINE_CHEMAUDIT_ERROR Failed to initialize ChemAudit validator: {e}", file=sys.stderr)
            raise
        
        # Validate batch
        results = validator.validate_batch(input_data)
        
        # Output results
        output_json = json.dumps(results, indent=2)
        
        if args.output:
            with open(args.output, 'w') as f:
                f.write(output_json)
            print(f"MZMINE_CHEMAUDIT_LOG Results written to {args.output}", file=sys.stderr)
        else:
            print(output_json)
        
        # Summary statistics
        valid_count = sum(1 for r in results if r.get("valid", False))
        print(f"MZMINE_CHEMAUDIT_LOG Validation complete: {valid_count}/{len(results)} passed quality thresholds", file=sys.stderr)
        
    except Exception as e:
        print(f"MZMINE_CHEMAUDIT_ERROR {str(e)}", file=sys.stderr)
        import traceback
        traceback.print_exc(file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
