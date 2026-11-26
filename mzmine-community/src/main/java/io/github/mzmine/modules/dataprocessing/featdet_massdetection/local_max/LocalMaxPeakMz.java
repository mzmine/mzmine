package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import org.jetbrains.annotations.NotNull;

/**
 * Calculates the m/z of one peak found by the {@link LocalMaxMassDetector}. Implementations must be
 * immutable, a detector may be shared between threads (e.g., the advanced import).
 */
@FunctionalInterface
public interface LocalMaxPeakMz {

  /**
   * The intensity weighted centroid of the {@link LocalMaxMassDetector}
   */
  LocalMaxPeakMz WEIGHTED_CENTROID = (mzs, intensities, spectrumBaseIntensity, startIdx, endIdx, apexIdx, weightedCentroidMz) -> weightedCentroidMz;

  /**
   * @param mzs                   m/z values of the spectrum
   * @param intensities           original (not smoothed) intensities of the spectrum
   * @param spectrumBaseIntensity highest intensity of the spectrum
   * @param startIdx              inclusive start of the peak
   * @param endIdx                exclusive end of the peak
   * @param apexIdx               index of the highest original intensity within the peak
   * @param weightedCentroidMz    intensity weighted centroid of the symmetric apex window
   * @return the m/z of the peak
   */
  double peakMz(double @NotNull [] mzs, double @NotNull [] intensities,
      double spectrumBaseIntensity, int startIdx, int endIdx, int apexIdx,
      double weightedCentroidMz);
}
