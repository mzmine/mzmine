/**
 * Scoring Types
 *
 * TypeScript interfaces for ML-readiness, NP-likeness, scaffold scoring,
 * drug-likeness, safety filters, and ADMET predictions.
 */

/**
 * Breakdown of ML-readiness score components.
 */
export interface MLReadinessBreakdown {
  // Standard descriptors (CalcMolDescriptors - 217 descriptors)
  descriptors_score: number;
  descriptors_max: number;
  descriptors_successful: number;
  descriptors_total: number;

  // Additional descriptors (AUTOCORR2D + MQN)
  additional_descriptors_score: number;
  additional_descriptors_max: number;
  autocorr2d_successful: number;
  autocorr2d_total: number;
  mqn_successful: number;
  mqn_total: number;

  // Fingerprints (7 types)
  fingerprints_score: number;
  fingerprints_max: number;
  fingerprints_successful: string[];
  fingerprints_failed: string[];

  // Size constraints
  size_score: number;
  size_max: number;
  molecular_weight: number | null;
  num_atoms: number | null;
  size_category: 'optimal' | 'acceptable' | 'out_of_range' | 'error' | 'unknown';
}

/**
 * ML-readiness scoring result.
 */
export interface MLReadinessResult {
  score: number;
  breakdown: MLReadinessBreakdown;
  interpretation: string;
  failed_descriptors: string[];
}

/**
 * NP-likeness scoring result.
 */
export interface NPLikenessResult {
  score: number;
  interpretation: string;
  caveats: string[];
  details: Record<string, unknown>;
}

/**
 * Scaffold extraction result.
 */
export interface ScaffoldResult {
  scaffold_smiles: string;
  generic_scaffold_smiles: string;
  has_scaffold: boolean;
  message: string;
  details: Record<string, unknown>;
}

// =============================================================================
// Drug-likeness Types
// =============================================================================

/**
 * Lipinski's Rule of Five results.
 */
export interface LipinskiResult {
  passed: boolean;
  violations: number;
  mw: number;
  logp: number;
  hbd: number;
  hba: number;
  details: Record<string, boolean>;
}

/**
 * QED score results.
 */
export interface QEDResult {
  score: number;
  properties: Record<string, number | string>;
  interpretation: string;
}

/**
 * Veber rules results.
 */
export interface VeberResult {
  passed: boolean;
  rotatable_bonds: number;
  tpsa: number;
}

/**
 * Rule of Three (fragment-likeness) results.
 */
export interface RuleOfThreeResult {
  passed: boolean;
  violations: number;
  mw: number;
  logp: number;
  hbd: number;
  hba: number;
  rotatable_bonds: number;
  tpsa: number;
}

/**
 * Ghose filter results.
 */
export interface GhoseResult {
  passed: boolean;
  violations: number;
  mw: number;
  logp: number;
  atom_count: number;
  molar_refractivity: number;
}

/**
 * Egan filter results.
 */
export interface EganResult {
  passed: boolean;
  logp: number;
  tpsa: number;
}

/**
 * Muegge filter results.
 */
export interface MueggeResult {
  passed: boolean;
  violations: number;
  details: Record<string, boolean>;
}

/**
 * Complete drug-likeness scoring results.
 */
export interface DrugLikenessResult {
  lipinski: LipinskiResult;
  qed: QEDResult;
  veber: VeberResult;
  ro3: RuleOfThreeResult;
  ghose: GhoseResult | null;
  egan: EganResult | null;
  muegge: MueggeResult | null;
  interpretation: string;
}

// =============================================================================
// Safety Filters Types
// =============================================================================

/**
 * Result for a single filter category.
 */
export interface FilterAlertResult {
  passed: boolean;
  alerts: string[];
  alert_count: number;
}

/**
 * ChEMBL structural alerts results.
 */
export interface ChEMBLAlertsResult {
  passed: boolean;
  total_alerts: number;
  bms: FilterAlertResult | null;
  dundee: FilterAlertResult | null;
  glaxo: FilterAlertResult | null;
  inpharmatica: FilterAlertResult | null;
  lint: FilterAlertResult | null;
  mlsmr: FilterAlertResult | null;
  schembl: FilterAlertResult | null;
}

/**
 * Complete safety filter results.
 */
export interface SafetyFilterResult {
  pains: FilterAlertResult;
  brenk: FilterAlertResult;
  nih: FilterAlertResult | null;
  zinc: FilterAlertResult | null;
  chembl: ChEMBLAlertsResult | null;
  all_passed: boolean;
  total_alerts: number;
  interpretation: string;
}

// =============================================================================
// ADMET Types
// =============================================================================

/**
 * Synthetic accessibility score result.
 */
export interface SyntheticAccessibilityResult {
  score: number;
  classification: 'easy' | 'moderate' | 'difficult' | 'unknown';
  interpretation: string;
}

/**
 * ESOL solubility prediction result.
 */
export interface SolubilityResult {
  log_s: number;
  solubility_mg_ml: number;
  classification: 'highly_soluble' | 'soluble' | 'moderate' | 'poor' | 'insoluble' | 'unknown';
  interpretation: string;
}

/**
 * Molecular complexity metrics.
 */
export interface ComplexityResult {
  fsp3: number;
  num_stereocenters: number;
  num_rings: number;
  num_aromatic_rings: number;
  bertz_ct: number;
  classification: 'flat' | 'moderate' | '3d' | 'unknown';
  interpretation: string;
}

/**
 * CNS MPO score result.
 */
export interface CNSMPOResult {
  score: number;
  components: Record<string, number>;
  cns_penetrant: boolean;
  interpretation: string;
}

/**
 * Bioavailability indicators.
 */
export interface BioavailabilityResult {
  tpsa: number;
  rotatable_bonds: number;
  hbd: number;
  hba: number;
  mw: number;
  logp: number;
  oral_absorption_likely: boolean;
  cns_penetration_likely: boolean;
  interpretation: string;
}

/**
 * Pfizer 3/75 Rule result.
 */
export interface PfizerRuleResult {
  passed: boolean;
  logp: number;
  tpsa: number;
  interpretation: string;
}

/**
 * GSK 4/400 Rule result.
 */
export interface GSKRuleResult {
  passed: boolean;
  mw: number;
  logp: number;
  interpretation: string;
}

/**
 * Golden Triangle (Abbott) analysis.
 */
export interface GoldenTriangleResult {
  in_golden_triangle: boolean;
  mw: number;
  logd: number;
  interpretation: string;
}

/**
 * Complete ADMET prediction results.
 */
export interface ADMETResult {
  synthetic_accessibility: SyntheticAccessibilityResult;
  solubility: SolubilityResult;
  complexity: ComplexityResult;
  cns_mpo: CNSMPOResult | null;
  bioavailability: BioavailabilityResult;
  pfizer_rule: PfizerRuleResult | null;
  gsk_rule: GSKRuleResult | null;
  golden_triangle: GoldenTriangleResult | null;
  molar_refractivity: number | null;
  interpretation: string;
}

// =============================================================================
// Aggregator Likelihood Types
// =============================================================================

/**
 * Aggregator likelihood prediction result.
 */
export interface AggregatorLikelihoodResult {
  likelihood: 'low' | 'moderate' | 'high';
  risk_score: number;
  logp: number;
  tpsa: number;
  mw: number;
  aromatic_rings: number;
  risk_factors: string[];
  interpretation: string;
}

// =============================================================================
// Core Types
// =============================================================================

/**
 * Basic molecule information.
 */
export interface ScoringMoleculeInfo {
  input_string: string;
  canonical_smiles: string | null;
  molecular_formula: string | null;
  molecular_weight: number | null;
}

/**
 * Available scoring types.
 */
export type ScoringType =
  | 'ml_readiness'
  | 'np_likeness'
  | 'scaffold'
  | 'druglikeness'
  | 'safety_filters'
  | 'admet'
  | 'aggregator';

/**
 * Request for molecule scoring.
 */
export interface ScoringRequest {
  molecule: string;
  format?: 'auto' | 'smiles' | 'inchi' | 'mol';
  include?: ScoringType[];
}

/**
 * Response containing scoring results.
 */
export interface ScoringResponse {
  status: string;
  molecule_info: ScoringMoleculeInfo;
  ml_readiness: MLReadinessResult | null;
  np_likeness: NPLikenessResult | null;
  scaffold: ScaffoldResult | null;
  druglikeness: DrugLikenessResult | null;
  safety_filters: SafetyFilterResult | null;
  admet: ADMETResult | null;
  aggregator: AggregatorLikelihoodResult | null;
  execution_time_ms: number;
}

/**
 * Scoring error response.
 */
export interface ScoringError {
  error: string;
  details?: {
    errors?: string[];
    warnings?: string[];
    format_detected?: string;
  };
}
