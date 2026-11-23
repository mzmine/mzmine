package io.github.mzmine.datamodel;

import org.jetbrains.annotations.NotNull;

/**
 * Kinds of problems detected while a raw data file was imported. Critical warnings indicate that
 * data may be missing or wrong, non-critical ones hint at a possibly suboptimal conversion.
 */
public enum RawFileWarningType {

  ZERO_INTENSITY_CENTROIDS(false, """
      Scans were detected as centroid but contain zero-intensity values. This might indicate incorrect conversion by msconvert.
      Make sure to run "peak picking" with vendor algorithm as the first step (even before title maker), otherwise msconvert uses
      a different algorithm that picks the highest data point of a profile spectral peak and adds zero intensities next to each signal.
      This leads to degraded mass accuracies."""),

  EMPTY_SCANS(false, """
      Some MS1 scans were recognized as empty (no detected signals).
      The possible reason might be high noise levels influencing mzml conversion.
      Files can be processed anyway, but consider re-converting if you encounter unexpected results.""");

  private final boolean critical;
  private final @NotNull String description;

  RawFileWarningType(boolean critical, @NotNull String description) {
    this.critical = critical;
    this.description = description;
  }

  /**
   * @return true if data may be missing or wrong
   */
  public boolean isCritical() {
    return critical;
  }

  /**
   * @return user-facing description of the problem
   */
  public @NotNull String getDescription() {
    return description;
  }
}
