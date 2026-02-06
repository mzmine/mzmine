/**
 * Standardization Types
 *
 * TypeScript interfaces for standardization requests and responses.
 */

export interface StandardizationOptions {
  /**
   * Include tautomer canonicalization.
   * WARNING: May lose E/Z double bond stereochemistry.
   */
  include_tautomer: boolean;

  /**
   * Attempt to preserve stereochemistry during standardization.
   */
  preserve_stereo: boolean;
}

export interface StandardizationStep {
  step_name: string;
  applied: boolean;
  description: string;
  changes: string;
}

export interface StereoComparison {
  before_count: number;
  after_count: number;
  lost: number;
  gained: number;
  double_bond_stereo_lost: number;
  warning: string | null;
}

export interface StructureComparison {
  original_atom_count: number;
  standardized_atom_count: number;
  original_formula: string | null;
  standardized_formula: string | null;
  original_mw: number | null;
  standardized_mw: number | null;
  mass_change_percent: number;
  is_identical: boolean;
  diff_summary: string[];
}

export interface CheckerIssue {
  penalty_score: number;
  message: string;
}

export interface StandardizationResult {
  original_smiles: string;
  standardized_smiles: string | null;
  success: boolean;
  error_message: string | null;
  steps_applied: StandardizationStep[];
  checker_issues: CheckerIssue[];
  excluded_fragments: string[];
  stereo_comparison: StereoComparison | null;
  structure_comparison: StructureComparison | null;
  mass_change_percent: number;
}

export interface MoleculeInfo {
  input_smiles: string;
  canonical_smiles: string | null;
  inchi: string | null;
  inchikey: string | null;
  molecular_formula: string | null;
  molecular_weight: number | null;
  num_atoms: number | null;
}

export interface StandardizeRequest {
  molecule: string;
  format?: 'auto' | 'smiles' | 'inchi' | 'mol';
  options?: StandardizationOptions;
}

export interface StandardizeResponse {
  molecule_info: MoleculeInfo;
  result: StandardizationResult;
  execution_time_ms: number;
}

export interface StandardizeError {
  error: string;
  details?: {
    errors?: string[];
    warnings?: string[];
    format_detected?: string;
  };
}

export interface StandardizeOptionsResponse {
  options: {
    include_tautomer: {
      type: string;
      default: boolean;
      description: string;
      warning: string;
    };
    preserve_stereo: {
      type: string;
      default: boolean;
      description: string;
    };
  };
  pipeline_steps: {
    name: string;
    description: string;
    always_run: boolean;
    requires_option?: string;
  }[];
}
