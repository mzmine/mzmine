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
import io.github.mzmine.util.collections.StreamUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.maths.Combinatorics;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
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
import org.jetbrains.annotations.NotNull;

public class ImageCorrelateGroupingTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ImageCorrelateGroupingTask.class.getName());
  private final ParameterSet parameters;
  private final ModularFeatureList featureList;
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
    noiseLevel = parameters.getParameter(ImageCorrelateGroupingParameters.NOISE_LEVEL).getValue();
    minimumNumberOfCorrelatedPixels = parameters.getParameter(
        ImageCorrelateGroupingParameters.MIN_NUMBER_OF_PIXELS).getValue();
    useMedianFilter = parameters.getValue(ImageCorrelateGroupingParameters.MEDIAN_FILTER_WINDOW);
    if (useMedianFilter) {
      medianFilter = parameters.getEmbeddedParameterValue(
          ImageCorrelateGroupingParameters.MEDIAN_FILTER_WINDOW);
    } else {
      medianFilter = 0;
    }
    useQuantileThreshold = parameters.getValue(ImageCorrelateGroupingParameters.QUANTILE_THRESHOLD);
    if (useQuantileThreshold) {
      quantileThreshold = parameters.getEmbeddedParameterValue(
          ImageCorrelateGroupingParameters.QUANTILE_THRESHOLD);
    } else {
      quantileThreshold = 0.0;
    }
    useHotspotRemoval = parameters.getValue(ImageCorrelateGroupingParameters.HOTSPOT_REMOVAL);
    if (useHotspotRemoval) {
      hotspotRemovalThreshold = parameters.getEmbeddedParameterValue(
          ImageCorrelateGroupingParameters.HOTSPOT_REMOVAL);
    } else {
      hotspotRemovalThreshold = 0.0;
    }
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
      var data = new FilteredRowData(intensities);
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
          // need to map to ensure thread is waiting for completion
          checkR2RAllFeaturesImageSimilarity(mapFeatureData, pair.left(), pair.right(),
              mapSimilarity);
          processedPairs.incrementAndGet();
        });

    logger.info(
        "Image correlation: Performed %d pairwise comparisons of rows.".formatted(comparedPairs));
  }


  //Intensities have to be sorted by scan number
  private record FilteredRowData(double[] intensities) {

  }

  private void checkR2RAllFeaturesImageSimilarity(Map<Feature, FilteredRowData> mapFeatureData,
      FeatureListRow a, FeatureListRow b, final R2RMap<RowsRelationship> mapSimilarity) {

    R2RSimpleSimilarityList imageSimilarities = new R2RSimpleSimilarityList(a, b,
        Type.MS1_FEATURE_CORR);
    for (Feature fa : a.getFeatures()) {
      RawDataFile dataFile = fa.getRawDataFile();
      Feature fb = b.getFeature(dataFile);
      if (fb == null) {
        continue;
      }

      double similarity = 0;
      double[] intensitiesA = mapFeatureData.get(fa).intensities;
      if (intensitiesA != null) {
        double[] intensitiesB = mapFeatureData.get(fb).intensities;

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

  private double calculateSimilarity(final double[] intensitiesA, final double[] intensitiesB) {
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

    if (useQuantileThreshold && filteredIntensityPairs.size() >= minimumNumberOfCorrelatedPixels) {
      double quantileThresholdA = calculateQuantile(
          filteredIntensityPairs.stream().map(IntensityPair::intensityA)
              .collect(Collectors.toList()), quantileThreshold);
      double quantileThresholdB = calculateQuantile(
          filteredIntensityPairs.stream().map(IntensityPair::intensityB)
              .collect(Collectors.toList()), quantileThreshold);
      filteredIntensityPairs.removeIf(
          pair -> pair.intensityA() < quantileThresholdA || pair.intensityB() < quantileThresholdB);
    }

    if (useHotspotRemoval && filteredIntensityPairs.size() >= minimumNumberOfCorrelatedPixels) {
      double hotSpotThresholdA = calculateQuantile(
          filteredIntensityPairs.stream().map(IntensityPair::intensityA)
              .collect(Collectors.toList()), hotspotRemovalThreshold);
      double hotSpotThresholdB = calculateQuantile(
          filteredIntensityPairs.stream().map(IntensityPair::intensityB)
              .collect(Collectors.toList()), hotspotRemovalThreshold);
      filteredIntensityPairs.removeIf(
          pair -> pair.intensityA() > hotSpotThresholdA || pair.intensityB() > hotSpotThresholdB);
    }

    if (filteredIntensityPairs.size() >= minimumNumberOfCorrelatedPixels) {
      double[][] correlationDataInput = new double[filteredIntensityPairs.size()][2];
      for (int i = 0; i < filteredIntensityPairs.size(); i++) {
        correlationDataInput[i][0] = filteredIntensityPairs.get(i).intensityA;
        correlationDataInput[i][1] = filteredIntensityPairs.get(i).intensityB;
      }

      return similarityMeasure.calc(correlationDataInput);
    }
    return 0;
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
