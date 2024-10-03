package io.github.mzmine.modules.dataprocessing.featdet_ML;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplitSeries {

  //@brief cuts out a single region of length @regionSize with start point @start
  public static final double[] extractSingleRegion(double[] fullSeries, int start, int regionSize) {
    double[] standardRegion = new double[regionSize];
    System.arraycopy(fullSeries, start, standardRegion, 0, regionSize);

    //How should .orElse be handeled?
    final double maxIntensity = Arrays.stream(standardRegion).max().orElse(0);
    if (maxIntensity != 0) {
      Arrays.stream(standardRegion).map(n -> (2*n / maxIntensity -1)).toArray();
    } else {
            Arrays.stream(standardRegion).map(n -> (n-1)).toArray();
        }
    return standardRegion;
  }

  //@brief iterates over the full series and cuts outs regions with overslap @regionSize/2. Adds zero padding to the end to get the right length.
  public static final List<double[]> extractRegionBatch(double[] fullSeries, int regionSize,
      int overlap, String paddingType) {
    int seriesLength = fullSeries.length;
    //The first region takes regionSize from seriesLength or regionSize-overlap from seriesLength-overlap.
    //Every additional region takes (regionSize-overlap).
    //Should check that SeriesLength, regionSize > overlap
    int numRegions = (seriesLength - overlap) / (regionSize - overlap);
    int leftover = (seriesLength - overlap) % (regionSize - overlap);
    int paddingLength = regionSize - leftover - overlap;
    double[] paddedSeries = new double[seriesLength + paddingLength];
    System.arraycopy(fullSeries, 0, paddedSeries, 0, seriesLength);
    if (paddingType == "lastValue") {
      double lastValue = fullSeries[fullSeries.length - 1];
      double[] padding = new double[paddingLength];
      Arrays.fill(padding, lastValue);
      System.arraycopy(padding, 0, paddedSeries, fullSeries.length, padding.length);
    }

    List<double[]> regionBatch = new ArrayList<>();
    for (int i = 0; i < numRegions + 1; i++) {
      regionBatch.add(extractSingleRegion(paddedSeries, i * (regionSize - overlap), regionSize));
    }
    return regionBatch;
  }

  //@brief extract batch with default length 128
  public static final List<double[]> extractRegionBatch(double[] fullSeries) {
    return extractRegionBatch(fullSeries, 128, 32, "zero");
  }
}
