/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_featurefilter;

import com.google.common.collect.Range;
import com.google.common.primitives.Booleans;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.shapeclassification.MobilityQualitySummaryType;
import io.github.mzmine.datamodel.features.types.annotations.shapeclassification.RtQualitySummaryType;
import io.github.mzmine.datamodel.features.types.annotations.shapeclassification.ShapeClassificationScoreType;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.AsymmetricGaussianPeak;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.GaussianDoublePeak;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.FitQuality;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.GaussianPeak;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakDimension;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakFitterUtils;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakModelFunction;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakQualitySummary;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Filters out peaks from feature list.
 */
public class FeatureFilterTask extends AbstractTask {

  // Logger
  private static final Logger logger = Logger.getLogger(FeatureFilterTask.class.getName());

  // Feature lists
  private final MZmineProject project;
  private final FeatureList origPeakList;
  private ModularFeatureList filteredPeakList;
  private final FeatureFilterChoices keepOrRemove;

  // Processed rows counter
  private int processedRows;
  private int totalRows;

  // Parameters
  private final ParameterSet parameters;
  private List<PeakModelFunction> peakModels = List.of(new GaussianPeak(),
      new AsymmetricGaussianPeak(), new GaussianDoublePeak());

  /**
   * Create the task.
   *
   * @param list         feature list to process.
   * @param parameterSet task parameters.
   */
  public FeatureFilterTask(final MZmineProject project, final FeatureList list,
      final ParameterSet parameterSet, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    // Initialize
    this.project = project;
    parameters = parameterSet;
    origPeakList = list;
    filteredPeakList = null;
    processedRows = 0;
    totalRows = 0;
    keepOrRemove = parameterSet.getValue(FeatureFilterParameters.keepMatching);
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Filtering feature list";
  }

  @Override
  public void run() {

    if (isCanceled()) {
      return;
    }

    try {
      setStatus(TaskStatus.PROCESSING);
      logger.info("Filtering feature list");
      final OriginalFeatureListOption handleOriginal = parameters.getParameter(
          FeatureFilterParameters.AUTO_REMOVE).getValue();

      // Filter the feature list
      filteredPeakList = filterPeakList((ModularFeatureList) origPeakList);

      if (!isCanceled()) {
        handleOriginal.reflectNewFeatureListToProject(
            parameters.getValue(FeatureFilterParameters.SUFFIX), project, filteredPeakList,
            origPeakList);

        setStatus(TaskStatus.FINISHED);
        logger.info("Finished feature list filter");
      }
    } catch (Throwable t) {
      t.printStackTrace();
      setErrorMessage(t.getMessage());
      logger.log(Level.SEVERE, t.getMessage());
      setStatus(TaskStatus.ERROR);
    }

  }

  /**
   * Filter the feature list.
   *
   * @param peakList feature list to filter.
   * @return a new feature list with entries of the original feature list that pass the filtering.
   */
  private ModularFeatureList filterPeakList(final ModularFeatureList peakList) {

    // Make a copy of the peakList
    final ModularFeatureList newPeakList = peakList.createCopy(
        peakList.getName() + ' ' + parameters.getParameter(RowsFilterParameters.SUFFIX).getValue(),
        getMemoryMapStorage(), false);

    // Get parameters - which filters are active
    final boolean filterByDuration = parameters.getParameter(FeatureFilterParameters.PEAK_DURATION)
        .getValue();
    final boolean filterByArea = parameters.getParameter(FeatureFilterParameters.PEAK_AREA)
        .getValue();
    final boolean filterByHeight = parameters.getParameter(FeatureFilterParameters.PEAK_HEIGHT)
        .getValue();
    final boolean filterByDatapoints = parameters.getParameter(
        FeatureFilterParameters.PEAK_DATAPOINTS).getValue();
    final boolean filterByFWHM = parameters.getParameter(FeatureFilterParameters.PEAK_FWHM)
        .getValue();
    final boolean filterByTailingFactor = parameters.getParameter(
        FeatureFilterParameters.PEAK_TAILINGFACTOR).getValue();
    final boolean filterByAsymmetryFactor = parameters.getParameter(
        FeatureFilterParameters.PEAK_ASYMMETRYFACTOR).getValue();
    final boolean keepMs2Only = parameters.getParameter(FeatureFilterParameters.KEEP_MS2_ONLY)
        .getValue();
    final boolean filterByShapeScore = parameters.getValue(FeatureFilterParameters.minRtShapeScore);
    final double minRtShapeScore = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        FeatureFilterParameters.minRtShapeScore, 0d);
    final boolean filterByMobilogramShape = parameters.getValue(
        FeatureFilterParameters.minMobilityShapeScore);
    final double minMobilogramScore = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        FeatureFilterParameters.minMobilityShapeScore, 0d);
    final boolean filterByTopToEdge = parameters.getValue(FeatureFilterParameters.topToEdge);
    final double topToEdgeThreshold = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        FeatureFilterParameters.topToEdge, 1d);

    final Range<Double> durationRange = parameters.getParameter(
        FeatureFilterParameters.PEAK_DURATION).getEmbeddedParameter().getValue();
    final Range<Double> areaRange = parameters.getParameter(FeatureFilterParameters.PEAK_AREA)
        .getEmbeddedParameter().getValue();
    final Range<Double> heightRange = parameters.getParameter(FeatureFilterParameters.PEAK_HEIGHT)
        .getEmbeddedParameter().getValue();
    final Range<Integer> datapointsRange = parameters.getParameter(
        FeatureFilterParameters.PEAK_DATAPOINTS).getEmbeddedParameter().getValue();
    final Range<Float> fwhmRange = RangeUtils.toFloatRange(
        parameters.getParameter(FeatureFilterParameters.PEAK_FWHM).getEmbeddedParameter()
            .getValue());
    final Range<Float> tailingRange = RangeUtils.toFloatRange(
        parameters.getParameter(FeatureFilterParameters.PEAK_TAILINGFACTOR).getEmbeddedParameter()
            .getValue());
    final Range<Float> asymmetryRange = RangeUtils.toFloatRange(
        parameters.getParameter(FeatureFilterParameters.PEAK_ASYMMETRYFACTOR).getEmbeddedParameter()
            .getValue());

    if (filterByShapeScore) {
      newPeakList.addFeatureType(new RtQualitySummaryType());
      newPeakList.addRowType(new RtQualitySummaryType());
    }
    if(filterByMobilogramShape) {
      newPeakList.addFeatureType(new MobilityQualitySummaryType());
      newPeakList.addRowType(new MobilityQualitySummaryType());
    }

    // Loop through all rows in feature list
    final ModularFeatureListRow[] rows = newPeakList.getRows()
        .toArray(ModularFeatureListRow[]::new);
    final RawDataFile[] rawdatafiles = newPeakList.getRawDataFiles().toArray(new RawDataFile[0]);
    int totalRawDataFiles = rawdatafiles.length;
    boolean[] keepPeak = new boolean[totalRawDataFiles];
    totalRows = rows.length;

    for (processedRows = 0; !isCanceled() && processedRows < totalRows; processedRows++) {
      final ModularFeatureListRow row = rows[processedRows];

      Arrays.fill(keepPeak, !getFailedTestValue());
      for (int i = 0; i < totalRawDataFiles; i++) {
        final Feature peak = row.getFeature(rawdatafiles[i]);
        // no feature for raw data file
        if (peak == null || peak.getFeatureStatus().equals(FeatureStatus.UNKNOWN)) {
          keepPeak[i] = false;
          continue;
        }

        final double peakDuration =
            peak.getRawDataPointsRTRange().upperEndpoint() - peak.getRawDataPointsRTRange()
                .lowerEndpoint();
        final double peakArea = peak.getArea();
        final double peakHeight = peak.getHeight();
        final int peakDatapoints = peak.getScanNumbers().size();
        final Scan bestMsMs = peak.getMostIntenseFragmentScan();

        Float peakFWHM = peak.getFWHM();
        Float peakTailingFactor = peak.getTailingFactor();
        Float peakAsymmetryFactor = peak.getAsymmetryFactor();

        final Range<Float> intensityRange = peak.getRawDataPointsIntensityRange();
        final double topToEdge =
            intensityRange != null ? intensityRange.upperEndpoint() / intensityRange.lowerEndpoint()
                : 1d;

        if (peakFWHM == null) {
          peakFWHM = -1.0f;
        }
        if (peakTailingFactor == null) {
          peakTailingFactor = -1.0f;
        }
        if (peakAsymmetryFactor == null) {
          peakAsymmetryFactor = -1.0f;
        }

        // Check Duration
        // Check Area
        // Check Height
        // Check # Data Points
        // Check FWHM
        // Check Tailing Factor
        // Check MS/MS filter
        if ((filterByDuration && !durationRange.contains(peakDuration)) || (filterByArea
            && !areaRange.contains(peakArea)) || (filterByHeight && !heightRange.contains(
            peakHeight)) || (filterByDatapoints && !datapointsRange.contains(peakDatapoints)) || (
            filterByFWHM && !fwhmRange.contains(peakFWHM)) || (filterByTailingFactor
            && !tailingRange.contains(peakTailingFactor)) || (filterByAsymmetryFactor
            && !asymmetryRange.contains(peakAsymmetryFactor)) || (keepMs2Only && bestMsMs == null)
            || (filterByTopToEdge && topToEdge < topToEdgeThreshold)) {
          // Mark peak to be removed
          keepPeak[i] = getFailedTestValue();
          continue;
        }

        if (keepPeak[i] == getFailedTestValue()) {
          continue;
        }

        // more expensive filters below here
        final IonTimeSeries<? extends Scan> featureData = peak.getFeatureData();
        if (filterByShapeScore) {
          final double[] rts = new double[featureData.getNumberOfValues()];
          final double[] intensities = new double[featureData.getNumberOfValues()];
          featureData.getIntensityValues(intensities);
          for (int j = 0; j < rts.length; j++) {
            rts[j] = featureData.getRetentionTime(j);
          }

          if (!matchesRtShapeFilter(peak, minRtShapeScore, rts, intensities)) {
            keepPeak[i] = getFailedTestValue();
            continue;
          }
        }

        if (filterByMobilogramShape && featureData instanceof IonMobilogramTimeSeries imts) {
          final SummedIntensityMobilitySeries mobilogram = imts.getSummedMobilogram();
          final double[] mobilities = new double[mobilogram.getNumberOfValues()];
          final double[] mobIntensities = new double[mobilogram.getNumberOfValues()];
          mobilogram.getIntensityValues(mobIntensities);
          mobilogram.getMobilityValues(mobilities);

          if (!matchesMobilogramShapeFilter(mobilities, mobIntensities, minMobilogramScore,
              (ModularFeature) peak)) {
            keepPeak[i] = getFailedTestValue();
            continue;
          }
        }
      }

      // empty row?
      boolean isEmpty = Booleans.asList(keepPeak).stream().allMatch(keep -> keep == false);
      if (isEmpty) {
        newPeakList.removeRow(row);
      } else {
        for (int i = 0; i < rawdatafiles.length; i++) {
          if (keepPeak[i] == false) {
            row.removeFeature(rawdatafiles[i]);
          }
        }
      }
    }

    newPeakList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(FeatureFilterModule.class, parameters,
            getModuleCallDate()));

    return newPeakList;
  }

  private boolean matchesMobilogramShapeFilter(double[] mobilities, double[] mobIntensities,
      final double minMobilogramScore, ModularFeature peak) {
    final FitQuality fit = PeakFitterUtils.fitPeakModels(mobilities, mobIntensities, peakModels);
    if (fit == null) {
      return false;
    }
    peak.set(MobilityQualitySummaryType.class,
        new PeakQualitySummary(PeakDimension.MOBILITY, fit.peakShapeClassification(),
            (float) fit.rSquared()));

    return fit.rSquared() >= minMobilogramScore;
  }

  private boolean matchesRtShapeFilter(final Feature f, final double minShapeScore,
      final double[] rts, final double[] intensities) {

    final FitQuality fitted = PeakFitterUtils.fitPeakModels(rts, intensities, peakModels);
    if (fitted == null) {
      return false;
    }

    ((ModularFeature) f).set(RtQualitySummaryType.class,
        new PeakQualitySummary(PeakDimension.RT, fitted.peakShapeClassification(),
            (float) fitted.rSquared()));

    return fitted.rSquared() >= minShapeScore;
  }

  private boolean getFailedTestValue() {
    return switch (keepOrRemove) {
      case FeatureFilterChoices.REMOVE_MATCHING -> true;
      case FeatureFilterChoices.KEEP_MATCHING -> false;
    };
  }
}
