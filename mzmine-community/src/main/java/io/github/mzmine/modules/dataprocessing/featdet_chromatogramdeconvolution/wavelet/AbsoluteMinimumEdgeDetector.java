package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

public class AbsoluteMinimumEdgeDetector implements EdgeDetector {

  private final int allowedIncreasing;

  public AbsoluteMinimumEdgeDetector(int allowedIncreasing) {
    this.allowedIncreasing = allowedIncreasing;
  }

  private int detectMinimum(final double[] y, final int startIndex, final int directionStep) {
    int index = startIndex;
    int numIncreasing = 0;
    int absMinIndex = startIndex;

    while (index > 0 && index < y.length - 2) {
      index += directionStep;
      if (index > 1 && y[index + directionStep] < y[absMinIndex]) {
        // still decreasing
        numIncreasing = 0;
        absMinIndex = index + directionStep;
        continue;
      }
      numIncreasing++;
      if (numIncreasing > allowedIncreasing) {
        break;
      }
    }
    index = absMinIndex;

    return index;
  }

  @Override
  public int detectLeftMinimum(double[] y, int startIndex) {
    return detectMinimum(y, startIndex, -1);
  }

  @Override
  public int detectRightMinimum(double[] y, int startIndex) {
    return detectMinimum(y, startIndex, 1);
  }
}
