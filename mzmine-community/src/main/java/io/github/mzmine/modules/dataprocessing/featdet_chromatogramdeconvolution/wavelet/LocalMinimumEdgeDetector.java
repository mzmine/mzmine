package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

public class LocalMinimumEdgeDetector implements EdgeDetector {

  private final int allowedIncreasing;

  public LocalMinimumEdgeDetector(int allowedIncreasing) {
    this.allowedIncreasing = allowedIncreasing;
  }

  private int detectMinimum(double[] y, int startIndex, int directionStep) {
    int index = startIndex;
    int numIncreasing = 0;

    while (index > 0 && index < y.length - 2) {
      index += directionStep;
      if (index > 1 && y[index + directionStep] < y[index + numIncreasing]) {
        // still decreasing
        numIncreasing = 0;
        continue;
      }
      numIncreasing++;
      if (numIncreasing > allowedIncreasing) {
        index += (numIncreasing - 1) * directionStep; // reset to valley
        break;
      }
    }

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
