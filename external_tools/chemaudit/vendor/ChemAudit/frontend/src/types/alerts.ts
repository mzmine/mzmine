/**
 * TypeScript interfaces for structural alert screening.
 */

export type AlertSeverity = 'critical' | 'warning' | 'info';

export interface AlertResult {
  pattern_name: string;
  description: string;
  severity: AlertSeverity;
  matched_atoms: number[];
  catalog_source: string;
  smarts?: string | null;
}

export interface MoleculeInfo {
  input_string: string;
  canonical_smiles: string | null;
  molecular_formula: string | null;
  num_atoms: number | null;
}

export interface AlertScreenRequest {
  molecule: string;
  format?: 'auto' | 'smiles' | 'inchi' | 'mol';
  catalogs?: string[];
}

export interface AlertScreenResponse {
  status: string;
  molecule_info: MoleculeInfo;
  alerts: AlertResult[];
  total_alerts: number;
  screened_catalogs: string[];
  has_critical: boolean;
  has_warning: boolean;
  execution_time_ms: number;
  educational_note: string;
}

export interface CatalogInfo {
  name: string;
  description: string;
  pattern_count: string;
  severity: string;
  note?: string | null;
}

export interface CatalogListResponse {
  catalogs: Record<string, CatalogInfo>;
  default_catalogs: string[];
}

export interface AlertError {
  error: string;
  details?: {
    errors?: string[];
    warnings?: string[];
    format_detected?: string;
  };
}
