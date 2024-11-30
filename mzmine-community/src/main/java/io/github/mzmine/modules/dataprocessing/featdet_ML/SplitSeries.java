package io.github.mzmine.modules.dataprocessing.featdet_ML;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplitSeries {

  // @brief cuts out a single region of length @regionSize with start point @start
  public static final double[] extractSingleRegion(double[] fullSeries, int start, int regionSize,
      boolean normalize) {
    double[] standardRegion = new double[regionSize];
    System.arraycopy(fullSeries, start, standardRegion, 0, regionSize);

    // How should .orElse be handeled?
    final double maxIntensity = Arrays.stream(standardRegion).max().orElse(0);
    if (normalize) {
      if (maxIntensity != 0) {
        // normalized intensity to values between -1 and 1
        standardRegion = Arrays.stream(standardRegion).map(n -> (2 * n / maxIntensity - 1))
            .toArray();
      } else {
        standardRegion = Arrays.stream(standardRegion).map(n -> (n - 1)).toArray();
      }
    }
    return standardRegion;
  }

  // @brief iterates over the full series and cuts outs regions with overslap
  // @regionSize/2. Adds zero padding to the end to get the right length.
  public static final List<double[]> extractRegionBatch(double[] fullSeries, int regionSize,
      int overlap, String paddingType, boolean normalize) {
    int initialSeriesLength = fullSeries.length;
    //adds padding of size overlap/2 on each side so and moves peaks on the edges more on the inside
    double[] extendedSeries = new double[initialSeriesLength + overlap];
    System.arraycopy(fullSeries, 0, extendedSeries, overlap / 2, initialSeriesLength);
    if (paddingType.equals("time")) {
      double firstValue = fullSeries[0];
      double lastValue = fullSeries[fullSeries.length - 1];
      double[] leftPadding = new double[overlap / 2];
      double[] rightPadding = new double[overlap / 2];
      Arrays.fill(leftPadding, firstValue);
      Arrays.fill(rightPadding, lastValue);
      System.arraycopy(leftPadding, 0, extendedSeries, 0, overlap / 2);
      System.arraycopy(rightPadding, 0, extendedSeries, overlap / 2 + initialSeriesLength,
          overlap / 2);
    }
    int seriesLength = extendedSeries.length;
    // The first region takes regionSize from seriesLength or regionSize-overlap
    // from seriesLength-overlap.
    // Every additional region takes (regionSize-overlap).
    // Should check that SeriesLength, regionSize > overlap
    int numRegions = (seriesLength - overlap) / (regionSize - overlap);
    int leftover = (seriesLength - overlap) % (regionSize - overlap);
    int paddingLength = regionSize - leftover - overlap;
    double[] paddedSeries = new double[seriesLength + paddingLength];
    System.arraycopy(extendedSeries, 0, paddedSeries, 0, seriesLength);
    if (paddingType.equals("time")) {
      double lastValue = fullSeries[fullSeries.length - 1];
      double[] padding = new double[paddingLength];
      Arrays.fill(padding, lastValue);
      System.arraycopy(padding, 0, paddedSeries, extendedSeries.length, padding.length);
    }

    List<double[]> regionBatch = new ArrayList<>();
    for (int i = 0; i < numRegions + 1; i++) {
      regionBatch.add(
          extractSingleRegion(paddedSeries, i * (regionSize - overlap), regionSize, normalize));
    }
    return regionBatch;
  }

  // @brief extract batch with default length 128
  public static final List<double[]> extractRegionBatch(double[] fullSeries) {
    return extractRegionBatch(fullSeries, 128, 32, "zero", true);
  }
}
