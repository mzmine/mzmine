/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.correlation.R2RImageSimilarityList;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.maths.Combinatorics;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import it.unimi.dsi.fastutil.Pair;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImageCorrelateGroupingTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ImageCorrelateGroupingTask.class.getName());
  private final ParameterSet parameters;
  private final ModularFeatureList featureList;
  private final List<FeatureListRow> rows;
  private long totalMaxPairs = 0;
  private final AtomicLong processedPairs = new AtomicLong(0);

  private final double noiseLevel;

  private final int minimumNumberOfCorrelatedPixels;
  private final int medianFilter;
  private final double quantileThreshold;
  private final double hotspotRemovalThreshold;

  private final boolean useMedianFilter;
  private final boolean useQuantileThreshold;
  private final boolean useHotspotRemoval;

  private final SimilarityMeasure similarityMeasure;
  private final double minR;

  public ImageCorrelateGroupingTask(final ParameterSet parameterSet,
      final ModularFeatureList featureList, @NotNull Instant moduleCallDate) {
    super(featureList.getMemoryMapStorage(), moduleCallDate);
    this.featureList = featureList;
    this.parameters = parameterSet;
    rows = featureList.getRows();
    noiseLevel = parameters.getParameter(ImageCorrelateGroupingParameters.NOISE_LEVEL).getValue();
    minimumNumberOfCorrelatedPixels = parameters.getParameter(
        ImageCorrelateGroupingParameters.MIN_NUMBER_OF_PIXELS).getValue();
    useMedianFilter = parameters.getParameter(ImageCorrelateGroupingParameters.MEDIAN_FILTER_WINDOW)
        .getValue();
    if (useMedianFilter) {
      medianFilter = parameters.getParameter(ImageCorrelateGroupingParameters.MEDIAN_FILTER_WINDOW)
          .getEmbeddedParameter().getValue();
    } else {
      medianFilter = 0;
    }
    useQuantileThreshold = parameters.getParameter(
        ImageCorrelateGroupingParameters.QUANTILE_THRESHOLD).getValue();
    if (useQuantileThreshold) {
      quantileThreshold = parameters.getParameter(
          ImageCorrelateGroupingParameters.QUANTILE_THRESHOLD).getEmbeddedParameter().getValue();
    } else {
      quantileThreshold = 0.0;
    }
    useHotspotRemoval = parameters.getParameter(ImageCorrelateGroupingParameters.HOTSPOT_REMOVAL)
        .getValue();
    if (useHotspotRemoval) {
      hotspotRemovalThreshold = parameters.getParameter(
          ImageCorrelateGroupingParameters.HOTSPOT_REMOVAL).getEmbeddedParameter().getValue();
    } else {
      hotspotRemovalThreshold = 0.0;
    }
    similarityMeasure = parameters.getParameter(ImageCorrelateGroupingParameters.MEASURE)
        .getValue();
    minR = parameters.getParameter(ImageCorrelateGroupingParameters.MIN_R).getValue();
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

    checkAllFeatures(mapImageSim, rows);
    logger.info("Image similarity check on rows done.");

    if (featureList != null) {
      //remove old similarities of same type
      featureList.getRowMaps().remove(Type.MS1_FEATURE_CORR);
      featureList.addRowsRelationships(mapImageSim, Type.MS1_FEATURE_CORR);
    }

    if (featureList != null) {
      featureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod(ImageCorrelateGroupingModule.class, parameters,
              getModuleCallDate()));
    }

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Parallel check of all r2r similarities
   *
   * @param mapSimilarity map for all MS2 cosine similarity edges
   * @param rows          match rows
   */
  public void checkAllFeatures(R2RMap<RowsRelationship> mapSimilarity, List<FeatureListRow> rows)
      throws MissingMassListException {
    // prefilter rows: check feature height and sort data
    Map<Feature, FilteredRowData> mapFeatureData = new HashMap<>();
    List<FeatureListRow> filteredRows = new ArrayList<>();
    for (FeatureListRow row : rows) {
      if (prepareRows(mapFeatureData, row)) {
        filteredRows.add(row);
      }
    }
    int numRows = filteredRows.size();
    totalMaxPairs = Combinatorics.uniquePairs(filteredRows);
    logger.log(Level.INFO,
        () -> MessageFormat.format("Checking image similarity on {0} rows", numRows));

    // try map multi for all pairs
    long comparedPairs = IntStream.range(0, numRows - 1).boxed()
        .<Pair<FeatureListRow, FeatureListRow>>mapMulti((i, consumer) -> {
          if (isCanceled()) {
            return;
          }
          FeatureListRow a = filteredRows.get(i);
          for (int j = i + 1; j < numRows; j++) {
            FeatureListRow b = filteredRows.get(j);
            consumer.accept(Pair.of(a, b));
          }
        }).parallel().mapToLong(pair -> {
          // need to map to ensure thread is waiting for completion
          checkR2RAllFeaturesImageSimilarity(mapFeatureData, pair.left(), pair.right(),
              mapSimilarity);
          // count comparisons
          processedPairs.incrementAndGet();
          return 1;
        }).sum();

    logger.info(
        "Image correlation: Performed %d pairwise comparisons of rows.".formatted(comparedPairs));
  }

  /**
   * Checks the minimum requirements for the best MS2 for each feature in a row to be matched by MS2
   * similarity (minimum number of data points and MS2 data availability). Results are sorted by
   * intensity and stored in the mapRowData
   *
   * @param mapFeatureData the target map to store filtered and sorted data point arrays
   * @param row            the test row
   * @return true if the row matches all criteria, false otherwise
   */
  private boolean prepareRows(
      @NotNull Map<Feature, ImageCorrelateGroupingTask.FilteredRowData> mapFeatureData,
      @NotNull FeatureListRow row) throws MissingMassListException {
    boolean result = false;
    for (Feature feature : row.getFeatures()) {
      ImageCorrelateGroupingTask.FilteredRowData data = getDataAndFilter(row, feature);
      mapFeatureData.put(feature, data);
      result = true;
    }
    return result;
  }

  //TODO improve performance here
  @Nullable
  private ImageCorrelateGroupingTask.FilteredRowData getDataAndFilter(@NotNull FeatureListRow row,
      Feature feature) {
    ObservableList<Scan> scans = feature.getRawDataFile().getScans();
    List<Double> intensitiesList = new ArrayList<>();
    for (Scan scan : scans) {
      double intensity = feature.getFeatureData().getIntensityForSpectrum(scan);

      //fill empty scan
      if (intensity < 0 || intensity < noiseLevel) {
        intensitiesList.add(0.0);
      } else {
        intensitiesList.add(intensity);
      }
    }

    double[] intensities = intensitiesList.stream().mapToDouble(Double::doubleValue).toArray();
    return intensities.length > 0 ? new ImageCorrelateGroupingTask.FilteredRowData(row, intensities)
        : null;
  }

  //Intensities have to be sorted by scan number
  private record FilteredRowData(FeatureListRow row, double[] intensities) {

  }

  private void checkR2RAllFeaturesImageSimilarity(Map<Feature, FilteredRowData> mapFeatureData,
      FeatureListRow a, FeatureListRow b, final R2RMap<RowsRelationship> mapSimilarity) {

    R2RImageSimilarityList imageSimilarities = new R2RImageSimilarityList(a, b,
        Type.MS1_FEATURE_CORR);
    for (Feature fa : a.getFeatures()) {
      double[] intensitiesA = mapFeatureData.get(fa).intensities;
      if (intensitiesA != null) {

        for (Feature fb : b.getFeatures()) {
          double[] intensitiesB = mapFeatureData.get(fb).intensities;
          if (intensitiesB != null) {
            List<IntensityPair> intensityPairs = new ArrayList<>();

            // Remove signals below noise level and create intensity pairs
            for (int i = 0; i < intensitiesA.length; i++) {
              if (intensitiesA[i] >= noiseLevel && intensitiesB[i] >= noiseLevel) {
                intensityPairs.add(new IntensityPair(intensitiesA[i], intensitiesB[i]));
              }
            }

            List<IntensityPair> filteredIntensityPairs;
            if (useMedianFilter && intensitiesA.length >= minimumNumberOfCorrelatedPixels) {
              filteredIntensityPairs = applyMedianFilter(intensityPairs, medianFilter);
            } else {
              filteredIntensityPairs = intensityPairs;
            }

            if (useQuantileThreshold
                && filteredIntensityPairs.size() >= minimumNumberOfCorrelatedPixels) {
              double quantileThresholdA = calculateQuantile(
                  filteredIntensityPairs.stream().map(IntensityPair::intensityA)
                      .collect(Collectors.toList()), quantileThreshold);
              double quantileThresholdB = calculateQuantile(
                  filteredIntensityPairs.stream().map(IntensityPair::intensityB)
                      .collect(Collectors.toList()), quantileThreshold);
              filteredIntensityPairs.removeIf(pair -> pair.intensityA() < quantileThresholdA
                  || pair.intensityB() < quantileThresholdB);
            }

            if (useHotspotRemoval
                && filteredIntensityPairs.size() >= minimumNumberOfCorrelatedPixels) {
              double hotSpotThresholdA = calculateQuantile(
                  filteredIntensityPairs.stream().map(IntensityPair::intensityA)
                      .collect(Collectors.toList()), hotspotRemovalThreshold);
              double hotSpotThresholdB = calculateQuantile(
                  filteredIntensityPairs.stream().map(IntensityPair::intensityB)
                      .collect(Collectors.toList()), hotspotRemovalThreshold);
              filteredIntensityPairs.removeIf(pair -> pair.intensityA() > hotSpotThresholdA
                  || pair.intensityB() > hotSpotThresholdB);
            }

            if (filteredIntensityPairs.size() >= minimumNumberOfCorrelatedPixels) {
              double[][] correlationDataInput = new double[filteredIntensityPairs.size()][2];
              for (int i = 0; i < filteredIntensityPairs.size(); i++) {
                correlationDataInput[i][0] = filteredIntensityPairs.get(i).intensityA;
                correlationDataInput[i][1] = filteredIntensityPairs.get(i).intensityB;
              }

              double similarity = similarityMeasure.calc(correlationDataInput);
              if (similarity >= minR) {
                imageSimilarities.addSimilarity(similarity);
              }
            }
          }
        }
      }
    }
    if (imageSimilarities.size() > 0) {
      mapSimilarity.add(a, b, imageSimilarities);
    }
  }

  private List<IntensityPair> applyMedianFilter(List<IntensityPair> intensityPairs,
      int windowSize) {
    List<IntensityPair> result = new ArrayList<>();
    int halfWindowSize = windowSize / 2;
    for (int i = 0; i < intensityPairs.size(); i++) {
      int start = Math.max(0, i - halfWindowSize);
      int end = Math.min(intensityPairs.size() - 1, i + halfWindowSize);
      List<IntensityPair> subarray = intensityPairs.subList(start, end + 1);
      double medianA = calculateMedian(
          subarray.stream().mapToDouble(IntensityPair::intensityA).toArray());
      double medianB = calculateMedian(
          subarray.stream().mapToDouble(IntensityPair::intensityB).toArray());
      result.add(new IntensityPair(medianA, medianB));
    }

    return result;
  }

  private double calculateMedian(double[] values) {
    Arrays.sort(values);
    int middle = values.length / 2;
    return values.length % 2 == 0 ? (values[middle - 1] + values[middle]) / 2.0 : values[middle];
  }

  private double calculateQuantile(List<Double> values, double quantile) {
    double[] array = values.stream().mapToDouble(Double::doubleValue).toArray();
    Arrays.sort(array);
    int index = (int) Math.ceil(quantile * array.length) - 1;
    return array[index];
  }

  private record IntensityPair(double intensityA, double intensityB) {

  }
}
