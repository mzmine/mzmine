package io.github.mzmine.modules.dataprocessing.featdet_smoothing2;

import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import javax.annotation.Nonnull;

public class SGIntensitySmoothing {

  /**
   * Defines how values that were previously zero shall be handled when returning the smoothed values.
   */
  enum ZeroHandlingType {
    /**
     * Values that were previously zero will be zero after smoothing.
     */
    KEEP, //
    /**
     * Values that were previously zero might get an intensity if determined by the smoothing
     * algorithm.
     */
    OVERRIDE
  }

  private final IntensitySeries access;
  private final ZeroHandlingType zht;
  private final double[] normWeights;

  /**
   * @param dataAccess         The intensity series to be smoothed. Ideally an instance of {@link
   *                           io.github.mzmine.datamodel.data_access.EfficientDataAccess} for best
   *                           performance.
   * @param zeroHandlingType defines how zero values shall be handled when comparing the old and
   *                           new intensities {@link ZeroHandlingType#KEEP}, {@link
   *                           ZeroHandlingType#OVERRIDE}.
   * @param normWeights        The normalized weights for smoothing.
   */
  public SGIntensitySmoothing(@Nonnull final IntensitySeries dataAccess,
      @Nonnull final ZeroHandlingType zeroHandlingType, @Nonnull final double[] normWeights) {
    this.access = dataAccess;
    this.zht = zeroHandlingType;
    this.normWeights = normWeights;
  }

  public double[] smooth() {
    // Initialise.
    final int numPoints = access.getNumberOfValues();
    final int fullWidth = normWeights.length;
    final int halfWidth = (fullWidth - 1) / 2;

    double[] smoothed = new double[numPoints];
    for (int i = 0; i < numPoints; i++) {
      final int k = i - halfWidth;
      for (int j = Math.max(0, -k); j < Math.min(fullWidth, numPoints - k); j++) {
        smoothed[i] += access.getIntensity(k + j) * normWeights[j];
      }

      // if values that were previously 0 shall remain 0, we process that here.
      if(zht == ZeroHandlingType.KEEP && Double.compare(access.getIntensity(i), 0d) == 0) {
        smoothed[i] = 0;
      }
    }

    return smoothed;
  }

}
