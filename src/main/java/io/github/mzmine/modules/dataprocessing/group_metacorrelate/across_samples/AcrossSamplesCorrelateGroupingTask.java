/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.across_samples;


import com.google.common.util.concurrent.AtomicDouble;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.PreloadedFeatureDataAccess;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.RowGroupList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.correlation.CorrelationRowGroup;
import io.github.mzmine.datamodel.features.correlation.R2RCorrMap;
import io.github.mzmine.datamodel.features.correlation.R2RCorrelationData;
import io.github.mzmine.datamodel.features.correlation.R2RFullCorrelationData;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureCorrelationUtil;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureShapeCorrelationParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.InterSampleHeightCorrParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AcrossSamplesCorrelateGroupingTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger
      .getLogger(AcrossSamplesCorrelateGroupingTask.class.getName());

  private AtomicDouble stageProgress = new AtomicDouble(0);
  private int totalRows;

  protected ParameterSet parameters;
  protected MZmineProject project;
  // GENERAL
  protected ModularFeatureList featureList;
  protected RTTolerance rtTolerance;
  protected boolean autoSuffix;
  protected String suffix;

  // GROUP and MIN SAMPLES FILTER
  protected String groupingParameter;
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

  private RowGroupList groups;
  // group param
  private UserParameter<?, ?> groupParam;


  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   * @param featureList  feature list.
   */
  public AcrossSamplesCorrelateGroupingTask(final MZmineProject project,
      final ParameterSet parameterSet,
      final ModularFeatureList featureList) {
    super(featureList.getMemoryMapStorage());
    this.project = project;
    this.featureList = featureList;
    parameters = parameterSet;

    totalRows = 0;

    // sample groups parameter
    groupingParameter =
        (String) parameters.getParameter(
            AcrossSamplesCorrelateGroupingParameters.GROUPSPARAMETER).getValue();

    // height and noise
    noiseLevelCorr = parameters.getParameter(
        AcrossSamplesCorrelateGroupingParameters.NOISE_LEVEL).getValue();
    minHeight = parameters.getParameter(
        AcrossSamplesCorrelateGroupingParameters.MIN_HEIGHT).getValue();

    // tolerances
    rtTolerance = parameterSet.getParameter(
        AcrossSamplesCorrelateGroupingParameters.RT_TOLERANCE).getValue();

    // FEATURE SHAPE CORRELATION
    groupByFShapeCorr =
        parameterSet.getParameter(
            AcrossSamplesCorrelateGroupingParameters.FSHAPE_CORRELATION).getValue();
    FeatureShapeCorrelationParameters corrp = parameterSet
        .getParameter(
            AcrossSamplesCorrelateGroupingParameters.FSHAPE_CORRELATION).getEmbeddedParameters();
    // filter
    // start with high abundant features >= mainPeakIntensity
    // In this way we directly filter out groups with no abundant features
    // fill in smaller features after
    minShapeCorrR =
        corrp.getParameter(FeatureShapeCorrelationParameters.MIN_R_SHAPE_INTRA).getValue();
    shapeSimMeasure = corrp.getParameter(FeatureShapeCorrelationParameters.MEASURE).getValue();
    minCorrelatedDataPoints =
        corrp.getParameter(FeatureShapeCorrelationParameters.MIN_DP_CORR_PEAK_SHAPE).getValue();
    minCorrDPOnFeatureEdge =
        corrp.getParameter(FeatureShapeCorrelationParameters.MIN_DP_FEATURE_EDGE).getValue();

    // total corr
    useTotalShapeCorrFilter =
        corrp.getParameter(FeatureShapeCorrelationParameters.MIN_TOTAL_CORR).getValue();
    minTotalShapeCorrR = corrp.getParameter(FeatureShapeCorrelationParameters.MIN_TOTAL_CORR)
        .getEmbeddedParameter().getValue();

    // intensity correlation across samples
    useHeightCorrFilter =
        parameterSet.getParameter(
            AcrossSamplesCorrelateGroupingParameters.IMAX_CORRELATION).getValue();
    minHeightCorr = parameterSet.getParameter(
        AcrossSamplesCorrelateGroupingParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleHeightCorrParameters.MIN_CORRELATION)
        .getValue();
    minDPHeightCorr = parameterSet.getParameter(
        AcrossSamplesCorrelateGroupingParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleHeightCorrParameters.MIN_DP).getValue();

    heightSimMeasure = parameterSet.getParameter(
        AcrossSamplesCorrelateGroupingParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleHeightCorrParameters.MEASURE).getValue();

    // suffix
    autoSuffix = !parameters.getParameter(
        AcrossSamplesCorrelateGroupingParameters.SUFFIX).getValue();

    if (autoSuffix) {
      suffix = MessageFormat.format("corr {2} r>={0} dp>={1}", minShapeCorrR,
          minCorrelatedDataPoints, shapeSimMeasure);
    } else {
      suffix = parameters.getParameter(AcrossSamplesCorrelateGroupingParameters.SUFFIX)
          .getEmbeddedParameter()
          .getValue();
    }
  }

  public RowGroupList getGroups() {
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
    groups = featureList.getGroups();
    if (groups == null || groups.isEmpty()) {
      setStatus(TaskStatus.FINISHED);
      LOG.warning("Cannot run metaCorrelate grouping on feature list (" + featureList.getName()
                  + ") with no groups. Make sure to run any row grouping method before (e.g., the standard metaCorrelate). Maybe there are just no groups. Then do not mind this message.");
      return;
    }

    groupParam = project
        .getParameterByName(groupingParameter);
    if (groupParam == null) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Cannot find project paramter with name " + groupingParameter
                      + "  (Make sure to set the grouping parameter)");
      return;
    }

    setStatus(TaskStatus.PROCESSING);
    LOG.info("Starting metaCorrelation (across samples) search in " + featureList.getName()
             + " feature list");
    try {
      if (isCanceled()) {
        return;
      }

      // create correlation map
      // do R2R comparison correlation
      // might also do annotation if selected
      R2RCorrMap corrMap = featureList.getCorrelationMap();
      doR2RComparison(featureList, groups, corrMap);
      if (isCanceled()) {
        return;
      }

      LOG.info("Corr: Starting to group by correlation");
      groups = corrMap.createCorrGroups(featureList, stageProgress);

      if (isCanceled()) {
        return;
      }
      // refinement:
      // filter by avg correlation in group
      // delete single connections between sub networks
      if (groups != null) {
        // set groups to pkl
        groups.stream().map(g -> (CorrelationRowGroup) g)
            .forEach(g -> g.recalcGroupCorrelation(corrMap));
        featureList.setGroups(groups);
        groups.setGroupsToAllRows();

        if (isCanceled()) {
          return;
        }

        // Add task description to peakList.
        featureList.addDescriptionOfAppliedTask(
            new SimpleFeatureListAppliedMethod(AcrossSamplesCorrelateGroupingModule.class,
                parameters));

        // Done.
        setStatus(TaskStatus.FINISHED);
        LOG.info("Finished correlation grouping in " + featureList);
      }
    } catch (

        Exception t) {
      LOG.log(Level.SEVERE, "Correlation error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      throw new MSDKRuntimeException(t);
    }
  }

  /**
   * Correlation and adduct network creation
   *
   * @return
   */
  private void doR2RComparison(ModularFeatureList featureList,
      RowGroupList groups, R2RCorrMap map) {

    // sort all rows into two groups
    List<FeatureListRow> rowsMethodA = new ArrayList<>();
    List<FeatureListRow> rowsMethodB = new ArrayList<>();
    Object valueA = null;
    Object valueB = null;
    for (FeatureListRow row : featureList.getRows()) {
      Object value = project
          .getParameterValue(groupParam, row.getBestFeature().getRawDataFile());
      if (value == null || value.toString().isEmpty()) {
        continue;
      }
      if (valueA == null || valueA.equals(value)) {
        rowsMethodA.add(row);
        valueA = value;
      } else if (valueB == null || valueB.equals(value)) {
        rowsMethodB.add(row);
        valueB = value;
      } else {
        throw new IllegalArgumentException(MessageFormat
            .format("Provided project sample group ({0}) with more than 2 values. {1}, {2}, {3}",
                groupingParameter, valueA, valueB, value));
      }
    }

    LOG.info("Corr: Creating row2row correlation map");
    totalRows = rowsMethodA.size() + rowsMethodB.size();
    final List<RawDataFile> raws = featureList.getRawDataFiles();

    // sort by avgRT (should actually be already sorted)
    rowsMethodA.sort(new FeatureListRowSorter(SortingProperty.RT, SortingDirection.Ascending));
    rowsMethodB.sort(new FeatureListRowSorter(SortingProperty.RT, SortingDirection.Ascending));

    PreloadedFeatureDataAccess data = new PreloadedFeatureDataAccess(featureList);
    data.loadIntensityValues();

    // for all rows - do in parallel
    rowsMethodA.stream().parallel().forEach(row -> {
      if (!isCanceled()) {
        try {
          // compare to rows of other method
          for (FeatureListRow row2 : rowsMethodB) {
            if (isCanceled()) {
              break;
            }

            // correlate if in rt range
            R2RCorrelationData corr =
                FeatureCorrelationUtil.corrR2RAcrossSamples(data, row, row2,
                    minCorrelatedDataPoints, minCorrDPOnFeatureEdge, noiseLevelCorr);

            // corr is even present if only grouping by retention time
            // corr is only null if heightCorrelation was not met
            if (corr != null &&
                (!groupByFShapeCorr || corr.getAvgShapeR() >= minShapeCorrR)) {
              // add to map
              // can be because of any combination of
              // retention time, shape correlation, non-negative height correlation
              map.add(row, row2, corr);
            }
          }
          stageProgress.addAndGet(1d / totalRows);
        } catch (Exception e) {
          LOG.log(Level.SEVERE, "Error in parallel R2Rcomparison", e);
          throw new MSDKRuntimeException(e);
        }
      }
    });

    // number of f2f correlations
    int nR2Rcorr = 0;
    int nF2F = 0;
    for (R2RCorrelationData r2r : map.values()) {
      if (r2r instanceof R2RFullCorrelationData corrData) {
        if (corrData.hasFeatureShapeCorrelation()) {
          nR2Rcorr++;
          nF2F += corrData.getCorrFeatureShape().size();
        }
      }
    }

    LOG.info(MessageFormat.format(
        "Corr: Correlations done with {0} R2R correlations and {1} F2F correlations", nR2Rcorr,
        nF2F));
  }

  /**
   * direct exclusion for high level filtering check rt of all peaks of all raw files
   *
   * @param row
   * @param row2
   * @param minHeight minimum feature height to check for RT
   * @return true only if there was at least one RawDataFile with features in both rows with
   * height>minHeight and within rtTolerance
   */
  public boolean checkRTRange(RawDataFile[] raw, FeatureListRow row, FeatureListRow row2,
      double minHeight, RTTolerance rtTolerance) {
    for (int r = 0; r < raw.length; r++) {
      Feature f = row.getFeature(raw[r]);
      Feature f2 = row2.getFeature(raw[r]);
      if (f != null && f2 != null && f.getHeight() >= minHeight && f2.getHeight() >= minHeight
          && rtTolerance.checkWithinTolerance(f.getRT(), f2.getRT())) {
        return true;
      }
    }
    return false;
  }

}
