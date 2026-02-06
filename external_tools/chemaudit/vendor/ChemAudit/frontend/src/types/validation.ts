export type Severity = 'critical' | 'error' | 'warning' | 'info' | 'pass';

export interface CheckResult {
  check_name: string;
  passed: boolean;
  severity: Severity;
  message: string;
  affected_atoms: number[];
  details: Record<string, unknown>;
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

export interface ValidationRequest {
  molecule: string;
  format?: 'auto' | 'smiles' | 'inchi' | 'mol';
  checks?: string[];
  preserve_aromatic?: boolean;
}

export interface ValidationResponse {
  status: string;
  molecule_info: MoleculeInfo;
  overall_score: number;
  issues: CheckResult[];
  all_checks: CheckResult[];
  execution_time_ms: number;
}

export interface ValidationError {
  error: string;
  details?: {
    errors?: string[];
    warnings?: string[];
    format_detected?: string;
  };
}

export interface ChecksResponse {
  [category: string]: string[];
}
