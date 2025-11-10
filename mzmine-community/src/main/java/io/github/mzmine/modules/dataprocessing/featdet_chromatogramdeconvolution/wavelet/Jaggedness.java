package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

record Jaggedness(DetectedPeak peak, int changes) {

  Jaggedness(final DetectedPeak peak, double[] y) {
    int lastSign = 1;
    int changes = 0;

    for (int i = peak.leftBoundaryIndex(); i < peak.rightBoundaryIndex(); i++) {
      if (y[i] < y[i + 1] && lastSign < 0) {
        changes++;
        lastSign = 1;
      } else if(y[i] > y[i + 1] && lastSign > 0) {
        changes++;
        lastSign = -1;
      }
    }

    this(peak, changes);
  }

  /**
   *
   * @return sign changes per 5 data points
   */
  public double jaggedness() {
    return (double) changes / ((peak.rightBoundaryIndex() - peak.leftBoundaryIndex()) / 5d);
  }
}
