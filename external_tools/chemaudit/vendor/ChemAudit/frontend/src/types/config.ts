/**
 * Configuration types for ChemAudit deployment profiles.
 */

/**
 * Deployment limits based on the selected profile.
 */
export interface DeploymentLimits {
  max_batch_size: number;
  max_file_size_mb: number;
  max_file_size_bytes: number;
}

/**
 * Full configuration response from the API.
 */
export interface ConfigResponse {
  app_name: string;
  app_version: string;
  deployment_profile: string;
  limits: DeploymentLimits;
}

/**
 * Default limits used when config hasn't loaded yet.
 * Uses conservative "medium" profile defaults.
 */
export const DEFAULT_LIMITS: DeploymentLimits = {
  max_batch_size: 10000,
  max_file_size_mb: 500,
  max_file_size_bytes: 500 * 1024 * 1024,
};

/**
 * Default configuration used before API response.
 */
export const DEFAULT_CONFIG: ConfigResponse = {
  app_name: 'ChemAudit',
  app_version: '1.0.0',
  deployment_profile: 'medium',
  limits: DEFAULT_LIMITS,
};
