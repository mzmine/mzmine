/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping;


import com.google.common.util.concurrent.AtomicDouble;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.CachedFeatureDataAccess;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.correlation.CorrelationRowGroup;
import io.github.mzmine.datamodel.features.correlation.R2RCorrelationData;
import io.github.mzmine.datamodel.features.correlation.R2RFullCorrelationData;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.R2RSimpleCorrelationData;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureCorrelationUtil;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureShapeCorrelationParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.InterSampleHeightCorrParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.MinimumFeatureFilter;
import io.github.mzmine.parameters.parametertypes.MinimumFeatureFilter.OverlapResult;
import io.github.mzmine.parameters.parametertypes.MinimumFeaturesFilterParameters;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CorrelationGroupingUtils;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;

public class CorrelateGroupingTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(CorrelateGroupingTask.class.getName());

  private final AtomicDouble stageProgress = new AtomicDouble(0);
  private final boolean keepExtendedStats;
  private final int simplifyLargeDatasets;
  private final OriginalFeatureListOption handleOriginal;
  protected ParameterSet parameters;
  protected MZmineProject project;
  // GENERAL
  protected ModularFeatureList featureList;
  protected RTTolerance rtTolerance;
  protected boolean autoSuffix;
  protected String suffix;
  /**
   * Minimum percentage of samples (in group if useGroup) that have to contain a feature
   */
  protected MinimumFeatureFilter minFFilter;
  // min adduct height and feature height for minFFilter
  protected double minHeight;
  // FEATURE SHAPE CORRELATION
  // correlation r to identify negative correlation
  protected boolean groupByFShapeCorr;
  protected SimilarityMeasure shapeSimMeasure;
  protected boolean useTotalShapeCorrFilter;
  protected double minTotalShapeCorrR;
  protected double minShapeCorrR;
  protected double noiseLevelCorr;
  protected int minCorrelatedDataPoints;
  protected int minCorrDPOnFeatureEdge;
  // MAX INTENSITY PROFILE CORRELATION ACROSS SAMPLES
  protected SimilarityMeasure heightSimMeasure;
  protected boolean useHeightCorrFilter;
  protected double minHeightCorr;
  protected int minDPHeightCorr;
  // output
  protected ModularFeatureList groupedPKL;
  private int totalRows;
  private List<RowGroup> groups;


  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   * @param featureList  feature list.
   */
  public CorrelateGroupingTask(final MZmineProject project, final ParameterSet parameterSet,
      final ModularFeatureList featureList, @NotNull Instant moduleCallDate) {
    super(featureList.getMemoryMapStorage(), moduleCallDate);
    this.project = project;
    this.featureList = featureList;
    parameters = parameterSet;

    totalRows = 0;

    // height and noise
    noiseLevelCorr = parameters.getValue(CorrelateGroupingParameters.NOISE_LEVEL);
    minHeight = parameters.getParameter(CorrelateGroupingParameters.MIN_HEIGHT).getValue();

    // by min percentage of samples in a sample set that contain this feature MIN_SAMPLES
    MinimumFeaturesFilterParameters minS = parameterSet.getParameter(
        CorrelateGroupingParameters.MIN_SAMPLES_FILTER).getEmbeddedParameters();
    minFFilter = minS.createFilterWithGroups(project, featureList.getRawDataFiles(), "", minHeight);

    // tolerances
    rtTolerance = parameterSet.getParameter(CorrelateGroupingParameters.RT_TOLERANCE).getValue();

    // FEATURE SHAPE CORRELATION
    groupByFShapeCorr = parameterSet.getParameter(CorrelateGroupingParameters.FSHAPE_CORRELATION)
        .getValue();
    FeatureShapeCorrelationParameters corrp = parameterSet.getParameter(
        CorrelateGroupingParameters.FSHAPE_CORRELATION).getEmbeddedParameters();
    // filter
    // start with high abundant features >= mainPeakIntensity
    // In this way we directly filter out groups with no abundant features
    // fill in smaller features after
    minShapeCorrR = corrp.getParameter(FeatureShapeCorrelationParameters.MIN_R_SHAPE_INTRA)
        .getValue();
    shapeSimMeasure = corrp.getParameter(FeatureShapeCorrelationParameters.MEASURE).getValue();
    minCorrelatedDataPoints = corrp.getParameter(
        FeatureShapeCorrelationParameters.MIN_DP_CORR_PEAK_SHAPE).getValue();
    minCorrDPOnFeatureEdge = corrp.getParameter(
        FeatureShapeCorrelationParameters.MIN_DP_FEATURE_EDGE).getValue();

    // total corr
    useTotalShapeCorrFilter = corrp.getParameter(FeatureShapeCorrelationParameters.MIN_TOTAL_CORR)
        .getValue();
    minTotalShapeCorrR = corrp.getParameter(FeatureShapeCorrelationParameters.MIN_TOTAL_CORR)
        .getEmbeddedParameter().getValue();

    // intensity correlation across samples
    useHeightCorrFilter = parameterSet.getParameter(CorrelateGroupingParameters.IMAX_CORRELATION)
        .getValue();
    minHeightCorr = parameterSet.getParameter(CorrelateGroupingParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleHeightCorrParameters.MIN_CORRELATION)
        .getValue();
    minDPHeightCorr = parameterSet.getParameter(CorrelateGroupingParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleHeightCorrParameters.MIN_DP).getValue();

    heightSimMeasure = parameterSet.getParameter(CorrelateGroupingParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleHeightCorrParameters.MEASURE).getValue();

    // suffix
    autoSuffix = !parameters.getParameter(CorrelateGroupingParameters.SUFFIX).getValue();

    if (autoSuffix) {
      suffix = MessageFormat.format("corr {2} r greq {0} dp greq {1}", minShapeCorrR,
          minCorrelatedDataPoints, shapeSimMeasure);
    } else {
      suffix = parameters.getParameter(CorrelateGroupingParameters.SUFFIX).getEmbeddedParameter()
          .getValue();
    }

    handleOriginal = parameters.getValue(CorrelateGroupingParameters.handleOriginal);

    // advanced
    boolean isAdvanced = parameters.getValue(CorrelateGroupingParameters.advanced);
    var advanced = parameters.getEmbeddedParameterValue(CorrelateGroupingParameters.advanced);
    keepExtendedStats =
        isAdvanced ? advanced.getValue(AdvancedCorrelateGroupingParameters.keepExtendedStats)
            : false;
    simplifyLargeDatasets =
        isAdvanced ? advanced.getValue(AdvancedCorrelateGroupingParameters.simplifyLargeDatasets)
            : 20000;

  }

  public List<RowGroup> getGroups() {
    return groups;
  }

  @Override
  public double getFinishedPercentage() {
    return stageProgress.get();
  }

  @Override
  public String getTaskDescription() {
    return "Identification of groups in " + featureList.getName() + " scan events (lists)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.log(Level.INFO, () -> String.format("Starting metaCorrelation search in feature list %s",
        featureList.getName()));
    try {
      if (isCanceled()) {
        return;
      }

      int totalNumSamples = featureList.getRawDataFiles().size();
      var minFInSamples = minFFilter.getMinFInSamples();
      if (minFInSamples != null) {
        int minSamplesFilter = minFInSamples.getMaximumValue(totalNumSamples);
        if (minSamplesFilter > totalNumSamples) {
          error("""
              Correlation row grouping (metaCorr) has set a minimum number of samples requirement of %d but the feature list only contains %d samples. \
              Please define a matching filter. Relative filters in percentage help to scale this parameter for small and larger datasets.""".formatted(
              minSamplesFilter, totalNumSamples));
          return;
        }
      }

      // create new feature list for grouping
      groupedPKL = handleOriginal == OriginalFeatureListOption.PROCESS_IN_PLACE ? featureList
          : FeatureListUtils.createCopy(featureList, suffix, getMemoryMapStorage(), true);

      // create correlation map
      // do R2R comparison correlation
      // might also do annotation if selected
      R2RMap<R2RCorrelationData> corrMap = new R2RMap<>();
      doR2RComparison(groupedPKL, corrMap);
      if (isCanceled()) {
        return;
      }
      // set correlation map
      var r2rNetworkingMaps = groupedPKL.getRowMaps();
      r2rNetworkingMaps.removeAllRowRelationships(Type.MS1_FEATURE_CORR);
      r2rNetworkingMaps.addAllRowsRelationships(corrMap, Type.MS1_FEATURE_CORR);

      logger.fine("Corr: Starting to group by correlation");
      groups = CorrelationGroupingUtils.createCorrGroups(groupedPKL, keepExtendedStats);

      if (isCanceled()) {
        return;
      }
      // refinement:
      // filter by avg correlation in group
      // delete single connections between sub networks
      if (groups != null) {
        // set groups to pkl
        for (final RowGroup group : groups) {
          // not needed for RowGroupSimple
          if (group instanceof CorrelationRowGroup g) {
            g.recalcGroupCorrelation(corrMap);
          }
        }
        groupedPKL.setGroups(groups);

        if (isCanceled()) {
          return;
        }

        // Add task description to peakList.
        groupedPKL.addDescriptionOfAppliedTask(
            new SimpleFeatureListAppliedMethod(CorrelateGroupingModule.class, parameters,
                getModuleCallDate()));

        // add to project
        handleOriginal.reflectNewFeatureListToProject(suffix, project, groupedPKL, featureList);

        // Done.
        setStatus(TaskStatus.FINISHED);
        logger.log(Level.INFO, "Finished correlation grouping in feature list {0}",
            featureList.getName());
      }
    } catch (Exception t) {
      logger.log(Level.SEVERE, "Correlation error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      throw new MSDKRuntimeException(t);
    }
  }

  /**
   * Correlation and adduct network creation
   */
  private void doR2RComparison(ModularFeatureList featureList, R2RMap<R2RCorrelationData> map) {
    logger.fine("Corr: Creating row2row correlation map");
    final List<RawDataFile> raws = featureList.getRawDataFiles();
    // filter list by minimum number of features in all samples or at least one group
    // sort by avgRT (should actually be sorted already)
    final FeatureListRow[] rows = featureList.getRows().stream()
        .filter(row -> minFFilter.filterMinFeatures(raws, row))
        .sorted(new FeatureListRowSorter(SortingProperty.RT, SortingDirection.Ascending))
        .toArray(FeatureListRow[]::new);

    totalRows = rows.length;

    // preload all intensity values only if feature shape correlation active or if overlap is computed
    // overlap is not computed for large datasets
    boolean calculateShapeOverlap = raws.size() < simplifyLargeDatasets;
    boolean preloadIntensities =
        useTotalShapeCorrFilter || groupByFShapeCorr || calculateShapeOverlap;
    CachedFeatureDataAccess data = new CachedFeatureDataAccess(rows, false, preloadIntensities);

    // for all rows - do in parallel
    long correlated = IntStream.range(0, totalRows - 1).parallel().mapToLong(i -> {
      long addedCorrelations = 0;
      if (!isCanceled()) {
        try {
          FeatureListRow row = rows[i];
          // compare to the rest of rows
          for (int x = i + 1; x < totalRows; x++) {
            if (isCanceled()) {
              break;
            }

            FeatureListRow row2 = rows[x];

            // average retention time should be in range
            if (!rtTolerance.checkWithinTolerance(row.getAverageRT(), row2.getAverageRT())) {
              // end second loop
              break;
            }

            // has a minimum number/% of overlapping features in all samples / in at least one
            // groups
            OverlapResult overlap = minFFilter.filterMinFeaturesOverlap(data, raws, row, row2,
                rtTolerance, calculateShapeOverlap);
            if (overlap.equals(OverlapResult.TRUE)) {
              // correlate if in rt range
              R2RFullCorrelationData corr = FeatureCorrelationUtil.corrR2R(data, raws, row, row2,
                  groupByFShapeCorr, minCorrelatedDataPoints, minCorrDPOnFeatureEdge,
                  minDPHeightCorr, minHeight, noiseLevelCorr, useHeightCorrFilter, heightSimMeasure,
                  minHeightCorr);

              // corr is even present if only grouping by retention time
              // corr is only null if heightCorrelation was not met
              if (corr != null && //
                  (!groupByFShapeCorr || FeatureCorrelationUtil.checkFShapeCorr(groupedPKL,
                      minFFilter, corr, useTotalShapeCorrFilter, minTotalShapeCorrR, minShapeCorrR,
                      shapeSimMeasure))) {
                // add to map
                // can be because of any combination of
                // retention time, shape correlation, non-negative height correlation
                if (keepExtendedStats) {
                  map.add(row, row2, corr);
                } else {
                  // save simplified object
                  map.add(row, row2, new R2RSimpleCorrelationData(corr));
                }
                addedCorrelations++;
              }
            }
          }
          stageProgress.addAndGet(1d / totalRows);
        } catch (Exception e) {
          logger.log(Level.SEVERE, "Error in parallel R2Rcomparison: " + e.getMessage(), e);
          throw new MSDKRuntimeException(e);
        }
      }
      return addedCorrelations;
    }).sum();

    // number of f2f correlations
    logger.info(MessageFormat.format("Added {0} correlation edges", map.size()));
  }

}
