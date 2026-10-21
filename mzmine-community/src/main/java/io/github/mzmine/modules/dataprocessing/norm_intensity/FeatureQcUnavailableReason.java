/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

/** Why a feature/sample pair has no feature-QC normalized abundance. */
public enum FeatureQcUnavailableReason {
  NO_QC_REFERENCES,
  NO_POSITIVE_FINITE_INPUT,
  GLOBAL_MISSING_QC_THRESHOLD,
  BATCH_INSUFFICIENT_USABLE_QCS,
  BATCH_MISSING_QC_THRESHOLD,
  FIT_FAILED,
  INVALID_PREDICTION,
  FACTOR_OUT_OF_BOUNDS
}
