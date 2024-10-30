/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.group_imagecorrelate;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.FeatureDataType;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.R2RSimpleSimilarityList;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.collections.StreamUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.maths.Combinatorics;
import io.github.mzmine.util.maths.Transform;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.math.plot.utils.Array;

public class ImageCorrelateGroupingTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ImageCorrelateGroupingTask.class.getName());
  private final ParameterSet parameters;
  private final ModularFeatureList featureList;
  private long totalMaxPairs = 0;
  private final AtomicLong processedPairs = new AtomicLong(0);

  private final double noiseLevel;

  private final int minimumNumberOfCorrelatedPixels;
  private final double lowerQuantile;
  private final double upperQuantile;


  private final SimilarityMeasure similarityMeasure;
  private final double minR;

  public ImageCorrelateGroupingTask(final ParameterSet parameterSet,
      final ModularFeatureList featureList, @NotNull Instant moduleCallDate) {
    super(featureList.getMemoryMapStorage(), moduleCallDate);
    this.featureList = featureList;
    this.parameters = parameterSet;
    noiseLevel = parameters.getParameter(ImageCorrelateGroupingParameters.NOISE_LEVEL).getValue();
    minimumNumberOfCorrelatedPixels = parameters.getParameter(
        ImageCorrelateGroupingParameters.MIN_NUMBER_OF_PIXELS).getValue();
    lowerQuantile = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        ImageCorrelateGroupingParameters.QUANTILE_THRESHOLD, 0d);
    upperQuantile = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        ImageCorrelateGroupingParameters.HOTSPOT_REMOVAL, 1d);
    similarityMeasure = parameters.getValue(ImageCorrelateGroupingParameters.MEASURE);
    minR = parameters.getValue(ImageCorrelateGroupingParameters.MIN_R);
  }

  @Override
  public String getTaskDescription() {
    return "Identification of co-located images " + featureList.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return totalMaxPairs == 0 ? 0 : processedPairs.get() / (double) totalMaxPairs;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (isCanceled()) {
      setStatus(TaskStatus.CANCELED);
    }
    final R2RMap<RowsRelationship> mapImageSim = new R2RMap<>();
    checkAllFeatures(mapImageSim);
    logger.info("Image similarity check on rows done.");

//    printDebugStatistics(mapImageSim);
    if (featureList != null) {
      //remove old similarities of same type
      featureList.getRowMaps().removeAllRowRelationships(Type.MS1_FEATURE_CORR);
      featureList.getRowMaps().addAllRowsRelationships(mapImageSim, Type.MS1_FEATURE_CORR);
    }

    if (featureList != null) {
      featureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod(ImageCorrelateGroupingModule.class, parameters,
              getModuleCallDate()));
    }

    setStatus(TaskStatus.FINISHED);
  }

  private static void printDebugStatistics(final R2RMap<RowsRelationship> mapImageSim) {
    Comparator<RowsRelationship> comparator = Comparator.comparing(
            (RowsRelationship r) -> r.getRowA().getPreferredAnnotationName(),
            Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(r -> r.getRowB().getPreferredAnnotationName(),
            Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(RowsRelationship::getScore, Comparator.reverseOrder());

    // sorted
    String stats = mapImageSim.values().stream().sorted(comparator).map(
        r -> Stream.of(r.getRowA().getPreferredAnnotationName(),
                r.getRowB().getPreferredAnnotationName(), r.getRowA().getID(), r.getRowB().getID(),
                r.getScoreFormatted()).map(o -> o == null ? "" : o.toString())
            .collect(Collectors.joining("\t"))).collect(Collectors.joining("\n"));
    logger.info("""
                    Correlation results:
                    name_a\tname_b\tid_a\tid_b\tscore
                    """ + stats);
  }

  /**
   * Parallel check of all r2r similarities
   *
   * @param mapSimilarity map for all MS2 cosine similarity edges
   */
  public void checkAllFeatures(R2RMap<RowsRelationship> mapSimilarity)
      throws MissingMassListException {
    // prefilter rows: check feature height and sort data
    Map<Feature, FilteredRowData> mapFeatureData = new HashMap<>();
    FeatureDataAccess featureDataAccess = EfficientDataAccess.of(featureList,
        FeatureDataType.INCLUDE_ZEROS);

    while (featureDataAccess.hasNextFeature()) {
      Feature f = featureDataAccess.nextFeature();
      double[] intensities = featureDataAccess.getIntensityValuesCopy();
      // prepare data to lower the complexity within the pair comparison
      var data = FilteredRowData.create(intensities, lowerQuantile, upperQuantile, noiseLevel,
          Transform.SQRT);
      mapFeatureData.put(f, data);
    }
    List<FeatureListRow> rows = featureList.getRows();

    int numRows = rows.size();
    totalMaxPairs = Combinatorics.uniquePairs(rows);
    logger.log(Level.INFO,
        () -> MessageFormat.format("Checking image similarity on {0} rows", numRows));

    // try map multi for all pairs
    long comparedPairs = StreamUtils.processPairs(rows, this::isCanceled, true, //
        pair -> {
//           need to map to ensure thread is waiting for completion
          checkR2RAllFeaturesImageSimilarity(mapFeatureData, pair.left(), pair.right(),
              mapSimilarity);
          processedPairs.incrementAndGet();
        });
    logger.info(
        "Image correlation: Performed %d pairwise comparisons of rows.".formatted(comparedPairs));
  }

  //Intensities have to be sorted by scan number

  /**
   * @param intensities                 sorted by original scan sorting
   * @param noiseLevelOrLowerPercentile lower percentile intensity or noise level, depending which
   *                                    is larger
   * @param upperPercentile             upper percentile intensity. may be 0 if unused
   */
  private record FilteredRowData(double[] intensities, double noiseLevelOrLowerPercentile,
                                 double upperPercentile) {


    private static FilteredRowData create(final double[] intensities, final double lowerQuantile,
        final double upperQuantile, double noiseLevel, final Transform transform) {
      if (transform != null) {
        for (int i = 0; i < intensities.length; i++) {
          intensities[i] = transform.transform(intensities[i]);
        }
      }
      // percentiles are optional
      // use 1 as minimum to cut out 0 intensity
      double lowerPercentile = 1;
      double upperPercentile = 0;
      boolean useUpperQuantile = upperQuantile < 1;
      boolean useLowerQuantile = lowerQuantile > 0;
      if (useLowerQuantile || useUpperQuantile) {
        double[] sorted = Array.copy(intensities);
        Arrays.sort(sorted);
        // find first non-zero index and exclude all below from the quantile ranges
        // this means that quantiles are taken from non-0 intensities
        int nonZeroIndex = BinarySearch.binarySearch(sorted, 0.00001, DefaultTo.GREATER_EQUALS);
        if (useLowerQuantile && nonZeroIndex >= 0) { // otherwise filter off
          lowerPercentile = MathUtils.calcQuantileSorted(sorted, nonZeroIndex, sorted.length,
              lowerQuantile);
        }
        if (useUpperQuantile && nonZeroIndex >= 0) { // otherwise filter off
          upperPercentile = MathUtils.calcQuantileSorted(sorted, nonZeroIndex, sorted.length,
              upperQuantile);
        }
      }

      // minimum is 1
      noiseLevel = Math.max(1, Math.max(noiseLevel, lowerPercentile));

      // BitSet is better for memory but boolean[] is faster
      // however the best may be just checking the intensity conditions in isRemoved
      // this requires no additional memory but is a bit slower than boolean[]
//      boolean[] removed = new boolean[intensities.length];
//      for (int i = 0; i < intensities.length; i++) {
//        double value = intensities[i];
//        if (value < noiseLevel || (useUpperQuantile && value > upperPercentile)) {
//          removed[i] = true;
//        }
//      }

      return new FilteredRowData(intensities, noiseLevel, upperPercentile);
    }

    public int size() {
      return intensities.length;
    }

    public boolean isRemoved(final int index) {
      // boolean array was not faster in tests for 7000 orbitrap images
//      return removed[index];
      double value = intensities[index];
      return value < noiseLevelOrLowerPercentile || (upperPercentile > 0
                                                     && value > upperPercentile);
    }

    public double getIntensity(final int i) {
      return intensities[i];
    }
  }

  /**
   * This method is called for each pair so it was optimized to move precalculations to
   * {@link FilteredRowData#create(double[], double, double, double, Transform)}
   */
  private void checkR2RAllFeaturesImageSimilarity(
      Map<Feature, ImageCorrelateGroupingTask.FilteredRowData> mapFeatureData, FeatureListRow a,
      FeatureListRow b, final R2RMap<RowsRelationship> mapSimilarity) {

    R2RSimpleSimilarityList imageSimilarities = new R2RSimpleSimilarityList(a, b,
        Type.MS1_FEATURE_CORR);
    for (Feature fa : a.getFeatures()) {
      RawDataFile dataFile = fa.getRawDataFile();
      Feature fb = b.getFeature(dataFile);
      if (fb == null) {
        continue;
      }

      double similarity = 0;
      ImageCorrelateGroupingTask.FilteredRowData intensitiesA = mapFeatureData.get(fa);
      if (intensitiesA != null) {
        ImageCorrelateGroupingTask.FilteredRowData intensitiesB = mapFeatureData.get(fb);

        if (intensitiesB != null) {
          similarity = calculateSimilarity(intensitiesA, intensitiesB);
        }
      }
      // always add value also 0 if no correlation
      imageSimilarities.addSimilarity(similarity);
    }
    if (imageSimilarities.getAverageSimilarity() >= minR) {
      mapSimilarity.add(a, b, imageSimilarities);
    }
  }

  /**
   * This method is called for each pair so it was optimized to move precalculations to
   * {@link FilteredRowData#create(double[], double, double, double, Transform)}
   */
  private double calculateSimilarity(final FilteredRowData dataA, final FilteredRowData dataB) {
    if (similarityMeasure == SimilarityMeasure.PEARSON) {
      // optimized pearson with zero copy of data
      // pearson is the default measure
      return zeroCopyPearsonR(dataA, dataB);
    }

    // if other similarity measure is selected - create arrays of actual matching data and call regular similarity.calc
    int values = 0;
    for (int old = 0; old < dataA.size(); old++) {
      // only exclude if both a and b exclude a data point
      if (!(dataA.isRemoved(old) && dataB.isRemoved(old))) {
        values++;
      }
    }
    double[] a = new double[values];
    double[] b = new double[values];
    int fi = 0;
    for (int old = 0; old < dataA.size(); old++) {
      if (!(dataA.isRemoved(old) && dataB.isRemoved(old))) {
        a[fi] = dataA.getIntensity(old);
        b[fi] = dataB.getIntensity(old);
        fi++;
      }
    }

    return similarityMeasure.calc(a, b);
  }

  /**
   * Optimized to avoid data copies and GC
   */
  private double zeroCopyPearsonR(final FilteredRowData dataA, final FilteredRowData dataB) {
    int values = 0;
    double sumX = 0.0, sumY = 0.0, sumXY = 0.0;
    double sumX2 = 0.0, sumY2 = 0.0;

    for (int old = 0; old < dataA.size(); old++) {
      // only exclude if both a and b exclude a data point
      if (!(dataA.isRemoved(old) && dataB.isRemoved(old))) {
        values++;

        double x = dataA.getIntensity(old);
        double y = dataB.getIntensity(old);

        sumX += x;
        sumY += y;
        sumXY += x * y;
        sumX2 += x * x;
        sumY2 += y * y;
      }
    }
    if (values < minimumNumberOfCorrelatedPixels) {
      return 0d;
    }

    double numerator = values * sumXY - sumX * sumY;
    double denominator = Math.sqrt((values * sumX2 - sumX * sumX) * (values * sumY2 - sumY * sumY));

    if (denominator == 0) {
      return 0d;
    }

    return numerator / denominator;
  }

}
