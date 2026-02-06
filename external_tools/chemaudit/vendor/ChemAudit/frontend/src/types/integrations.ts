/**
 * Types for external database integrations (PubChem, ChEMBL, COCONUT)
 */

// Request types
export interface IntegrationRequest {
  smiles?: string;
  inchikey?: string;
}

// PubChem
export interface PubChemResult {
  found: boolean;
  cid?: number;
  iupac_name?: string;
  molecular_formula?: string;
  molecular_weight?: number;
  canonical_smiles?: string;
  inchi?: string;
  inchikey?: string;
  synonyms?: string[];
  url?: string;
}

// ChEMBL
export interface BioactivityData {
  target_chembl_id: string;
  target_name?: string;
  target_type?: string;
  activity_type: string;
  activity_value?: number;
  activity_unit?: string;
  assay_chembl_id: string;
  document_chembl_id?: string;
}

export interface ChEMBLResult {
  found: boolean;
  chembl_id?: string;
  pref_name?: string;
  molecule_type?: string;
  max_phase?: number;
  molecular_formula?: string;
  molecular_weight?: number;
  bioactivities: BioactivityData[];
  bioactivity_count: number;
  url?: string;
}

// COCONUT
export interface COCONUTResult {
  found: boolean;
  coconut_id?: string;
  name?: string;
  smiles?: string;
  inchikey?: string;
  molecular_formula?: string;
  molecular_weight?: number;
  organism?: string;
  organism_type?: string;
  nplikeness?: number;
  url?: string;
}

// Combined lookup result
export interface DatabaseLookupResult {
  pubchem?: PubChemResult;
  chembl?: ChEMBLResult;
  coconut?: COCONUTResult;
}

export interface IntegrationError {
  error: string;
  details?: Record<string, unknown>;
}
