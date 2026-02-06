package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

import java.util.LinkedList;
import java.util.Queue;
import org.apache.commons.math3.stat.regression.SimpleRegression;

public class SlopeEdgeDetector implements EdgeDetector {

  private final int windowSize;
  private final Queue<Data> history = new LinkedList<>();

  public SlopeEdgeDetector(int windowSize) {
    this.windowSize = windowSize;
    if(windowSize < 2) {
      throw new RuntimeException("Window size must be at least 2");
    }
  }

  private final SimpleRegression regression = new SimpleRegression();

  @Override
  public int detectLeftMinimum(double[] y, int startIndex) {
    history.clear();
    regression.clear();
    final double normFactor = 1; //y[startIndex];

    if (startIndex - windowSize < 0) {
      return 0;
    }

    // fill initial points
    for (int i = startIndex; i > startIndex - windowSize; i--) {
      final Data data = new Data(i, y[i] / normFactor);
      regression.addData(data.x(), data.y());
      history.add(data);
    }

    if (regression.getSlope() < 0) {
      return startIndex - windowSize;
    }

    int absMinIndex = startIndex;

    for (int i = startIndex - windowSize; i > 0; i--) {
      final Data data = new Data(i, y[i] / normFactor);
      regression.addData(data.x(), data.y());
      history.add(data);

      if (y[i] < y[absMinIndex]) {
        absMinIndex = i;
      }

      Data old = history.poll();
      regression.removeData(old.x(), old.y());
      if (regression.getSlope() < 0) {
        return i + windowSize / 2;
//        return absMinIndex;
      }
    }
    return absMinIndex;
  }

  @Override
  public int detectRightMinimum(double[] y, int startIndex) {
    history.clear();
    regression.clear();
    final double normFactor = 1; //y[startIndex];

    if (startIndex - windowSize < 0) {
      return 0;
    }

    // fill initial points
    for (int i = startIndex; i < startIndex + windowSize && i < y.length; i++) {
      final Data data = new Data(i, y[i] / normFactor);
      regression.addData(data.x(), data.y());
      history.add(data);
    }

    if (regression.getSlope() > 0) {
      return startIndex - windowSize;
    }

    int absMinIndex = startIndex;

    for (int i = startIndex + windowSize; i < y.length; i++) {
      final Data data = new Data(i, y[i] / normFactor);
      regression.addData(data.x(), data.y());
      history.add(data);

      if (y[i] < y[absMinIndex]) {
        absMinIndex = i;
      }

      Data old = history.poll();
      regression.removeData(old.x(), old.y());
      if (regression.getSlope() > 0) {
        return i - windowSize / 2;
//        return absMinIndex;
      }
    }
    return absMinIndex;
  }

  record Data(int x, double y) {

  }
}
