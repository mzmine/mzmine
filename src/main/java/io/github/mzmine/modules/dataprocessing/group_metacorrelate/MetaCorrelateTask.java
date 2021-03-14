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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate;


import com.google.common.util.concurrent.AtomicDouble;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.RowGroupList;
import io.github.mzmine.datamodel.features.correlation.R2RCorrMap;
import io.github.mzmine.datamodel.features.correlation.R2RCorrelationData;
import io.github.mzmine.datamodel.features.correlation.R2RFullCorrelationData;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.HeadLessDesktop;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureCorrelationUtil;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureShapeCorrelationParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.InterSampleHeightCorrParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingTask;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity.MS2SimilarityParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity.MS2SimilarityTask;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.MinimumFeatureFilter;
import io.github.mzmine.parameters.parametertypes.MinimumFeatureFilter.OverlapResult;
import io.github.mzmine.parameters.parametertypes.MinimumFeaturesFilterParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javafx.collections.ObservableList;

public class MetaCorrelateTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(MetaCorrelateTask.class.getName());

  public enum Stage {
    CORRELATION_ANNOTATION(0.5), GROUPING(0.6), MS2_SIMILARITY(0.8), ANNOTATION(0.95), REFINEMENT(
        1d);
    private double finalProgress;

    Stage(double finalProgress) {
      this.finalProgress = finalProgress;
    }

    public double getFinalProgress() {
      return finalProgress;
    }
  }


  private AtomicDouble stageProgress = new AtomicDouble(0);
  private int totalRows;

  protected ParameterSet parameters;
  protected MZmineProject project;
  // GENERAL
  protected ModularFeatureList featureList;
  protected RTTolerance rtTolerance;
  protected boolean autoSuffix;
  protected String suffix;

  // ADDUCTS
  protected IonNetworkingParameters annotationParameters;
  protected IonNetworkLibrary library;
  protected boolean searchAdducts;

  // MS2 similarity
  protected MS2SimilarityParameters ms2SimilarityCheckParam;


  // GROUP and MIN SAMPLES FILTER
  protected boolean useGroups;
  protected String groupingParameter;
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

  // perform MS2Similarity check
  protected boolean checkMS2Similarity;

  // stage of processing
  private Stage stage;

  // output
  protected FeatureList groupedPKL;
  protected boolean performAnnotationRefinement;
  protected CorrelateGroupingParameters groupParam;
  protected MinimumFeaturesFilterParameters minFeatureFilter;
  protected FeatureShapeCorrelationParameters corrParam;
  protected InterSampleHeightCorrParameters heightCorrParam;



  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   */
  public MetaCorrelateTask(final MZmineProject project, final ParameterSet parameterSet,
      final ModularFeatureList featureList) {
    super(featureList.getMemoryMapStorage());
    this.project = project;
    this.featureList = featureList;
    parameters = parameterSet;

    totalRows = 0;

    // sample groups parameter
    useGroups = parameters.getParameter(MetaCorrelateParameters.GROUPSPARAMETER).getValue();
    groupingParameter = (String) parameters.getParameter(MetaCorrelateParameters.GROUPSPARAMETER)
        .getEmbeddedParameter().getValue();

    // height and noise
    noiseLevelCorr = parameters.getParameter(MetaCorrelateParameters.NOISE_LEVEL).getValue();
    minHeight = parameters.getParameter(MetaCorrelateParameters.MIN_HEIGHT).getValue();

    minFeatureFilter = (MinimumFeaturesFilterParameters) parameterSet
        .getParameter(MetaCorrelateParameters.MIN_SAMPLES_FILTER).getEmbeddedParameters();
    minFFilter = minFeatureFilter.createFilterWithGroups(project, featureList.getRawDataFiles(),
        groupingParameter, minHeight);

    // tolerances
    rtTolerance = parameterSet.getParameter(MetaCorrelateParameters.RT_TOLERANCE).getValue();

    // FEATURE SHAPE CORRELATION
    groupByFShapeCorr =
        parameterSet.getParameter(MetaCorrelateParameters.FSHAPE_CORRELATION).getValue();
    corrParam = parameterSet.getParameter(MetaCorrelateParameters.FSHAPE_CORRELATION)
        .getEmbeddedParameters();
    // filter
    // start with high abundant features >= mainPeakIntensity
    // In this way we directly filter out groups with no abundant features
    // fill in smaller features after
    minShapeCorrR =
        corrParam.getParameter(FeatureShapeCorrelationParameters.MIN_R_SHAPE_INTRA).getValue();
    shapeSimMeasure = corrParam.getParameter(FeatureShapeCorrelationParameters.MEASURE).getValue();
    minCorrelatedDataPoints =
        corrParam.getParameter(FeatureShapeCorrelationParameters.MIN_DP_CORR_PEAK_SHAPE).getValue();
    minCorrDPOnFeatureEdge =
        corrParam.getParameter(FeatureShapeCorrelationParameters.MIN_DP_FEATURE_EDGE).getValue();

    // total corr
    useTotalShapeCorrFilter =
        corrParam.getParameter(FeatureShapeCorrelationParameters.MIN_TOTAL_CORR).getValue();
    minTotalShapeCorrR = corrParam.getParameter(FeatureShapeCorrelationParameters.MIN_TOTAL_CORR)
        .getEmbeddedParameter().getValue();
    // ADDUCTS
    searchAdducts = parameterSet.getParameter(MetaCorrelateParameters.ADDUCT_LIBRARY).getValue();
    annotationParameters =
        parameterSet.getParameter(MetaCorrelateParameters.ADDUCT_LIBRARY).getEmbeddedParameters();
    annotationParameters =
        IonNetworkingParameters.createFullParamSet(annotationParameters, minHeight);
    library = new IonNetworkLibrary(
        annotationParameters.getParameter(IonNetworkingParameters.LIBRARY).getEmbeddedParameters(),
        annotationParameters.getParameter(IonNetworkingParameters.MZ_TOLERANCE).getValue());
    // END OF ADDUCTS AND REFINEMENT

    checkMS2Similarity =
        parameterSet.getParameter(MetaCorrelateParameters.MS2_SIMILARITY).getValue();
    ms2SimilarityCheckParam =
        parameterSet.getParameter(MetaCorrelateParameters.MS2_SIMILARITY).getEmbeddedParameters();

    // intensity correlation across samples
    useHeightCorrFilter =
        parameterSet.getParameter(MetaCorrelateParameters.IMAX_CORRELATION).getValue();
    heightCorrParam =
        parameterSet.getParameter(MetaCorrelateParameters.IMAX_CORRELATION).getEmbeddedParameters();
    minHeightCorr =
        parameterSet.getParameter(MetaCorrelateParameters.IMAX_CORRELATION).getEmbeddedParameters()
            .getParameter(InterSampleHeightCorrParameters.MIN_CORRELATION).getValue();
    minDPHeightCorr = parameterSet.getParameter(MetaCorrelateParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleHeightCorrParameters.MIN_DP).getValue();

    heightSimMeasure = parameterSet.getParameter(MetaCorrelateParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleHeightCorrParameters.MEASURE).getValue();


    // suffix
    autoSuffix = !parameters.getParameter(MetaCorrelateParameters.SUFFIX).getValue();

    if (autoSuffix)
      suffix = MessageFormat.format("corr {2} r>={0} dp>={1}, {3}", minShapeCorrR,
          minCorrelatedDataPoints, shapeSimMeasure, searchAdducts ? "MS annot" : "");
    else
      suffix =
          parameters.getParameter(MetaCorrelateParameters.SUFFIX).getEmbeddedParameter().getValue();

    // create grouping param
    groupParam = new CorrelateGroupingParameters(rtTolerance, useGroups, groupingParameter,
        minHeight, noiseLevelCorr, autoSuffix, suffix, minFeatureFilter, groupByFShapeCorr,
        useHeightCorrFilter, corrParam, heightCorrParam);

  }

  @Override
  public double getFinishedPercentage() {
    if (stage == null)
      return 0;
    else {
      double prevProgress =
          stage.ordinal() == 0 ? 0 : Stage.values()[stage.ordinal() - 1].getFinalProgress();
      return prevProgress + (stage.getFinalProgress() - prevProgress) * stageProgress.get();
    }
  }

  @Override
  public String getTaskDescription() {
    return "Identification of groups in " + featureList.getName() + " scan events (lists)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    LOG.info("Starting MSE correlation search in " + featureList.getName() + " peaklists");

    if (isCanceled())
      return;

    // grouping
    CorrelateGroupingTask groupTask = new CorrelateGroupingTask(project, groupParam, featureList);
    groupTask.addTaskStatusListener((task, newStatus, oldStatus) -> {
      switch (newStatus) {
        case CANCELED -> cancel();
        case ERROR -> cancel();
        case FINISHED -> {
          groupedPKL = groupTask.getGroupedPKL();
          RowGroupList groups = groupTask.getGroups();
          analyseGroups(groupedPKL, groups);
        }
      }
    });
    MZmineCore.getTaskController().addTask(groupTask);

    while (getStatus().equals(TaskStatus.PROCESSING)) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    // // create new PKL for grouping
    // groupedPKL = copyPeakList(peakList, suffix);
    //
    // // MAIN STEP
    // // create correlation map
    // setStage(Stage.CORRELATION_ANNOTATION);
    //
    // // do R2R comparison correlation
    // // might also do annotation if selected
    // R2RCorrMap corrMap = new R2RCorrMap(rtTolerance, minFFilter);
    // doR2RComparison(groupedPKL, corrMap);
    // if (isCanceled())
    // return;
    //
    // LOG.info("Corr: Starting to group by correlation");
    // setStage(Stage.GROUPING);
    // RowGroupList groups = corrMap.createCorrGroups(groupedPKL, stageProgress);
    //
    // if (isCanceled())
    // return;
    // // refinement:
    // // filter by avg correlation in group
    // // delete single connections between sub networks
    // if (groups != null) {
    // // set groups to pkl
    // groups.stream().map(g -> (CorrelationRowGroup) g)
    // .forEach(g -> g.recalcGroupCorrelation(corrMap));
    // groupedPKL.setGroups(groups);
    // groups.setGroupsToAllRows();
    //
    // // do MSMS comparison of group
    // setStage(Stage.MS2_SIMILARITY);
  }

  public void analyseGroups(FeatureList groupedPKL, RowGroupList groups) {
    try {
      List<AbstractTask> steps = new ArrayList<>();

      if (checkMS2Similarity) {
        // calc MS2 similarity for later visualisation
        MS2SimilarityTask ms2Sim =
            new MS2SimilarityTask(ms2SimilarityCheckParam, groupedPKL, groups);
        steps.add(ms2Sim);
      }

      // annotation at groups stage
      if (searchAdducts) {
        LOG.info("Corr: Annotation of groups only");
        setStage(Stage.ANNOTATION);
        IonNetworkingTask annTask =
            new IonNetworkingTask(project, annotationParameters, groupedPKL);
        steps.add(annTask);
      }

      for (AbstractTask task : steps) {
        AtomicBoolean state = new AtomicBoolean(true);
        task.addTaskStatusListener(new TaskStatusListener() {
          @Override
          public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
            switch (newStatus) {
              case FINISHED:
                state.set(false);
                break;
              case CANCELED:
              case ERROR:
                cancel();
                break;
            }
          }
        });
        MZmineCore.getTaskController().addTask(task);
        while (state.get() && getStatus().equals(TaskStatus.PROCESSING)) {
          try {
            Thread.sleep(300);
          } catch (Exception e) {
          }
        }
      }

      // // add to project
      // project.addPeakList(groupedPKL);
      //
      // // do adduct search
      // // searchAdducts();
      // // Add task description to peakList.
      // groupedPKL.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
      // "Correlation grouping and identification of adducts", parameters));

      // Repaint the window to reflect the change in the peak list
      Desktop desktop = MZmineCore.getDesktop();
      if (!(desktop instanceof HeadLessDesktop))
        desktop.getMainWindow().repaint();

      // Done.
      setStatus(TaskStatus.FINISHED);
      LOG.info("Finished correlation grouping and adducts search in " + featureList);
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Correlation and adduct search error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      throw new MSDKRuntimeException(t);
    }
  }

  private FeatureList copyFeatureList(FeatureList featureList, String suffix) {
    SimpleFeatureList pkl = new SimpleFeatureList(featureList + " " + suffix, featureList.getRawDataFiles());
    for (FeatureListRow row : featureList.getRows()) {
      pkl.addRow(copyFeatureRow(row));
    }
    return pkl;
  }

  /**
   * Create a copy of a peak list row.
   *
   * @param row the row to copy.
   * @return the newly created copy.
   */
  private static FeatureListRow copyFeatureRow(final FeatureListRow row) {
    // Copy the peak list row.
    final FeatureListRow newRow = new SimpleFeatureListRow(row.getID());
    FeatureUtils.copyFeatureListRowProperties(row, newRow);

    // Copy the peaks.
    for (final Feature feature : row.getFeatures()) {
      final Feature newFeature = new SimpleFeature(feature);
      FeatureUtils.copyFeatureProperties(feature, newFeature);
      newRow.addFeature(feature.getDataFile(), newFeature);
    }

    return newRow;
  }

  private void setStage(Stage grouping) {
    stage = grouping;
    stageProgress.set(0d);
  }

  /**
   * Correlation and adduct network creation
   * 
   * @param peakList
   * @return
   */
  private void doR2RComparison(FeatureList featureList, R2RCorrMap map) throws Exception {
    LOG.info("Corr: Creating row2row correlation map");
    FeatureListRow rows[] = featureList.getRows();
    totalRows = rows.length;
    final ObservableList<RawDataFile> raw[] = featureList.getRawDataFiles();

    // sort by avgRT
    Arrays.sort(rows, new FeatureListRowSorter(SortingProperty.RT, SortingDirection.Ascending));

    // for all rows
    AtomicInteger annotPairs = new AtomicInteger(0);
    AtomicInteger compared = new AtomicInteger(0);

    IntStream.range(0, rows.length - 1).parallel().forEach(i -> {
      if (!isCanceled()) {
        try {
          FeatureListRow row = rows[i];
          // has a minimum number/% of features in all samples / in at least one groups
          if (minFFilter.filterMinFeatures(raw, row)) {
            for (int x = i + 1; x < totalRows; x++) {
              if (isCanceled())
                break;

              FeatureListRow row2 = rows[x];

              // has a minimum number/% of overlapping features in all samples / in at least one
              // groups
              OverlapResult overlap =
                  minFFilter.filterMinFeaturesOverlap(raw, row, row2, rtTolerance);
              if (overlap.equals(OverlapResult.TRUE)) {
                // correlate if in rt range
                R2RFullCorrelationData corr =
                    FeatureCorrelationUtil.corrR2R(data, raw, row, row2, groupByFShapeCorr,
                        minCorrelatedDataPoints, minCorrDPOnFeatureEdge, minDPHeightCorr, minHeight,
                        noiseLevelCorr, useHeightCorrFilter, heightSimMeasure, minHeightCorr);

                // corr is even present if only grouping by retention time
                // corr is only null if heightCorrelation was not met
                if (corr != null && //
                (!groupByFShapeCorr || FeatureCorrelationUtil.checkFShapeCorr(groupedPKL,
                    minFFilter, corr, useTotalShapeCorrFilter, minTotalShapeCorrR, minShapeCorrR,
                    shapeSimMeasure))) {
                  // add to map
                  // can be because of any combination of
                  // retention time, shape correlation, non-negative height correlation
                  map.add(row, row2, corr);
                }
              }
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
      if (r2r instanceof R2RFullCorrelationData) {
        R2RFullCorrelationData data = (R2RFullCorrelationData) r2r;
        if (data.hasFeatureShapeCorrelation()) {
          nR2Rcorr++;
          nF2F += data.getCorrFeatureShape().size();
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
   *         height>minHeight and within rtTolerance
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
