/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.NotNull;

/**
 * How the QC drift curve of each feature and batch is smoothed: automatic QC-RLSC or a manual
 * LOESS with fixed bandwidth and degree.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = FeatureQcSmoothing.QcRlsc.class, name = "qc_rlsc"),
    @JsonSubTypes.Type(value = FeatureQcSmoothing.ManualLoess.class, name = "manual_loess")})
public sealed interface FeatureQcSmoothing {

  /**
   * @return number of robust bisquare reweighting iterations of the LOESS fit
   */
  int robustnessIterations();

  @NotNull String description();

  /**
   * Dunn et al. 2011: selects the neighborhood size and polynomial degree per feature and batch by
   * leave-one-out cross-validation.
   *
   * @param robustnessIterations fixed for QC-RLSC, see {@link #DEFAULT}. Stored to document the
   *                             applied method.
   */
  record QcRlsc(int robustnessIterations) implements FeatureQcSmoothing {

    /**
     * decision: Dunn does not state the number of robustness iterations; the cited robust LOESS by
     * Cleveland (1979) uses two. Limits the influence of single outlier QC injections.
     */
    public static final QcRlsc DEFAULT = new QcRlsc(2);

    public QcRlsc {
      if (robustnessIterations < 0) {
        throw new IllegalArgumentException("robustness_iterations must be nonnegative.");
      }
    }

    @Override
    public @NotNull String description() {
      return "QC-RLSC";
    }
  }

  /**
   * LOESS with a fixed bandwidth and polynomial degree for all features and batches. No
   * cross-validation.
   *
   * @param bandwidth fraction of the usable QCs in each local fit, rounded up to whole neighbors
   */
  record ManualLoess(double bandwidth, int polynomialDegree, int robustnessIterations) implements
      FeatureQcSmoothing {

    public ManualLoess {
      if (!Double.isFinite(bandwidth) || bandwidth <= 0d || bandwidth > 1d) {
        throw new IllegalArgumentException("bandwidth must be in (0, 1].");
      }
      if (polynomialDegree < 1 || polynomialDegree > 2) {
        throw new IllegalArgumentException("polynomial_degree must be 1 or 2.");
      }
      if (robustnessIterations < 0) {
        throw new IllegalArgumentException("robustness_iterations must be nonnegative.");
      }
    }

    @Override
    public @NotNull String description() {
      return "Manual LOESS";
    }
  }
}
