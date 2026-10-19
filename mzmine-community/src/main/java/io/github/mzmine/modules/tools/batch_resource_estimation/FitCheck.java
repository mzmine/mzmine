package io.github.mzmine.modules.tools.batch_resource_estimation;

/**
 * Validation of the scaling model: the largest subset run is predicted from the smaller runs and
 * compared to its measurement.
 *
 * @param files            number of files of the largest subset
 * @param predictedHeapGB  heap predicted from the smaller subsets
 * @param measuredHeapGB   heap measured in the largest subset
 * @param predictedTempGB  temp file data predicted from the smaller subsets
 * @param measuredTempGB   temp file data measured in the largest subset
 * @param predictedSeconds time predicted from the smaller subsets
 * @param measuredSeconds  time measured in the largest subset
 */
public record FitCheck(int files, double predictedHeapGB, double measuredHeapGB,
                       double predictedTempGB, double measuredTempGB, double predictedSeconds,
                       double measuredSeconds) {

  /**
   * @return the largest relative deviation of heap, temp and time, 0.1 = 10 %
   */
  public double maxRelativeError() {
    return Math.max(relativeError(predictedHeapGB, measuredHeapGB),
        Math.max(relativeError(predictedTempGB, measuredTempGB),
            relativeError(predictedSeconds, measuredSeconds)));
  }

  /**
   * @return the larger relative deviation of heap and temp, 0.1 = 10 %
   */
  public double maxResourceError() {
    return Math.max(relativeError(predictedHeapGB, measuredHeapGB),
        relativeError(predictedTempGB, measuredTempGB));
  }

  private static double relativeError(final double predicted, final double measured) {
    // small absolute values like 1 MB of temp files would dominate a pure relative error
    final double denominator = Math.max(Math.abs(measured), ScalingFit.MIN_POSITIVE * 100);
    return Math.abs(predicted - measured) / denominator;
  }
}
