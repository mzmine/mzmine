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
package io.github.mzmine.modules.dataprocessing.adap_hierarchicalclustering;

import dulab.adap.common.algorithms.FeatureTools;
import dulab.adap.datamodel.Component;
import dulab.adap.datamodel.Peak;
import dulab.adap.datamodel.PeakInfo;
import dulab.adap.workflow.TwoStepDecomposition;
import dulab.adap.workflow.TwoStepDecompositionParameters;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionV1_5Task extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(ADAP3DecompositionV1_5Task.class.getName());

  // Feature lists.
  private final MZmineProject project;
  private final FeatureList originalPeakList;
  private FeatureList newPeakList;
  private final TwoStepDecomposition decomposition;

  // User parameters
  private final ParameterSet parameters;

  ADAP3DecompositionV1_5Task(final MZmineProject project, final FeatureList list,
      final ParameterSet parameterSet, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    // Initialize.
    this.project = project;
    parameters = parameterSet;
    originalPeakList = list;
    newPeakList = null;
    decomposition = new TwoStepDecomposition();
  }

  @Override
  public String getTaskDescription() {
    return "ADAP Peak decomposition on " + originalPeakList;
  }

  @Override
  public double getFinishedPercentage() {
    return decomposition.getProcessedPercent();
  }

  @Override
  public void run() {
    if (!isCanceled()) {
      String errorMsg = null;

      setStatus(TaskStatus.PROCESSING);
      logger.info("Started ADAP Peak Decomposition on " + originalPeakList);

      // Check raw data files.
      if (originalPeakList.getNumberOfRawDataFiles() > 1) {

        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            "Peak Decomposition can only be performed on feature lists with a single raw data file");

      } else {

        try {

          newPeakList = decomposePeaks(originalPeakList);

          if (!isCanceled()) {

            // Add new peaklist to the project.
            project.addFeatureList(newPeakList);

            //// Add quality parameters to peaks
            //QualityParameters.calculateQualityParameters(newPeakList);

            // Remove the original peaklist if requested.
            if (parameters.getParameter(ADAP3DecompositionV1_5Parameters.AUTO_REMOVE).getValue()) {
              project.removeFeatureList(originalPeakList);
            }

            setStatus(TaskStatus.FINISHED);
            logger.info("Finished peak decomposition on " + originalPeakList);
          }

        } catch (IllegalArgumentException e) {
          errorMsg = "Incorrect Feature List selected:\n" + e.getMessage();
        } catch (IllegalStateException e) {
          errorMsg = "Peak decompostion error:\n" + e.getMessage();
        } catch (Exception e) {
          errorMsg = "'Unknown error' during peak decomposition. \n" + e.getMessage();
        } catch (Throwable t) {

          setStatus(TaskStatus.ERROR);
          setErrorMessage(t.getMessage());
          logger.log(Level.SEVERE, "Peak decompostion error", t);
        }

        // Report error.
        if (errorMsg != null) {
          setErrorMessage(errorMsg);
          setStatus(TaskStatus.ERROR);
        }
      }
    }
  }

  private FeatureList decomposePeaks(FeatureList peakList)
      throws CloneNotSupportedException, IOException {
    RawDataFile dataFile = peakList.getRawDataFile(0);

    // Create new feature list.
    final ModularFeatureList resolvedPeakList = new ModularFeatureList(
        peakList + " "
            + parameters.getParameter(ADAP3DecompositionV1_5Parameters.SUFFIX).getValue(),
        getMemoryMapStorage(), dataFile);

    resolvedPeakList.setSelectedScans(dataFile, peakList.getSeletedScans(dataFile));

    // Load previous applied methods.
    for (final FeatureList.FeatureListAppliedMethod method : peakList.getAppliedMethods()) {
      resolvedPeakList.addDescriptionOfAppliedTask(method);
    }

    // Add task description to feature list.
    resolvedPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Peak deconvolution by ADAP-3",
            ADAPHierarchicalClusteringModule.class, parameters, getModuleCallDate()));

    // Collect peak information
    List<Peak> peaks = getPeaks(peakList,
        this.parameters.getParameter(ADAP3DecompositionV1_5Parameters.EDGE_TO_HEIGHT_RATIO)
            .getValue(),
        this.parameters.getParameter(ADAP3DecompositionV1_5Parameters.DELTA_TO_HEIGHT_RATIO)
            .getValue());

    // Find components (a.k.a. clusters of peaks with fragmentation spectra)
    List<Component> components = getComponents(peaks);

    // Create PeakListRow for each components
    List<FeatureListRow> newPeakListRows = new ArrayList<>();

    int rowID = 0;

    for (final Component component : components) {
      if (component.getSpectrum().isEmpty()) {
        continue;
      }

      ModularFeatureListRow row = new ModularFeatureListRow(resolvedPeakList, ++rowID);

      // Add the reference peak
      FeatureListRow originalPeakRow = originalPeakList.getRow(component.getBestPeak().getInfo().peakID);
      // ?
      originalPeakRow.setFeatureList(resolvedPeakList);
      // ?
      Feature refPeak = new ModularFeature(resolvedPeakList, originalPeakRow.getBestFeature());

      // Add spectrum
      List<DataPoint> dataPoints = new ArrayList<>();
      for (Map.Entry<Double, Double> entry : component.getSpectrum().entrySet()) {
        dataPoints.add(new SimpleDataPoint(entry.getKey(), entry.getValue()));
      }

      refPeak.setIsotopePattern(
          new SimpleIsotopePattern(dataPoints.toArray(new DataPoint[dataPoints.size()]), -1,
              IsotopePattern.IsotopePatternStatus.PREDICTED, "Spectrum"));

      row.addFeature(dataFile, refPeak);

      // Add PeakInformation
      if (originalPeakRow.getFeatureInformation() != null) {
        SimpleFeatureInformation information = new SimpleFeatureInformation(
            new HashMap<>(originalPeakRow.getFeatureInformation().getAllProperties()));
        row.setFeatureInformation(information);
      }

      // resolvedPeakList.addRow(row);
      newPeakListRows.add(row);
    }

    // ------------------------------------
    // Sort new peak rows by retention time
    // ------------------------------------

    Collections.sort(newPeakListRows, new Comparator<FeatureListRow>() {
      @Override
      public int compare(FeatureListRow row1, FeatureListRow row2) {
        double retTime1 = row1.getAverageRT();
        double retTime2 = row2.getAverageRT();

        return Double.compare(retTime1, retTime2);
      }
    });

    for (FeatureListRow row : newPeakListRows) {
      resolvedPeakList.addRow(row);
    }

    return resolvedPeakList;
  }

  /**
   * Convert MZmine PeakList to a list of ADAP Peaks
   *
   * @param peakList               MZmine PeakList object
   * @param edgeToHeightThreshold  edge-to-height threshold to determine peaks that can be merged
   * @param deltaToHeightThreshold delta-to-height threshold to determine peaks that can be merged
   * @return list of ADAP Peaks
   */

  @NotNull
  public static List<Peak> getPeaks(final FeatureList peakList, final double edgeToHeightThreshold,
      final double deltaToHeightThreshold) {
    RawDataFile dataFile = peakList.getRawDataFile(0);

    List<Peak> peaks = new ArrayList<>();

    for (FeatureListRow row : peakList.getRows()) {
      Feature peak = row.getBestFeature();
      List<Scan> scanNumbers = peak.getScanNumbers();

      // Build chromatogram
      NavigableMap<Double, Double> chromatogram = new TreeMap<>();
      for (int i = 0; i < scanNumbers.size(); i++) {
        DataPoint dataPoint = peak.getDataPointAtIndex(i);
        if (dataPoint != null) {
          chromatogram.put(Double.valueOf(String.valueOf(peak.getRetentionTimeAtIndex(i))),
              dataPoint.getIntensity());
        }
      }

      if (chromatogram.size() <= 1) {
        continue;
      }

      // Fill out PeakInfo
      PeakInfo info = new PeakInfo();

      try {
        // Note: info.peakID is the index of PeakListRow in
        // PeakList.peakListRows (starts from 0)
        // row.getID is row.myID (starts from 1)
        info.peakID = row.getID() - 1;

        double height = -Double.MIN_VALUE;
        for (int i = 0; i < scanNumbers.size(); i++) {
          double intensity = peak.getDataPointAtIndex(i).getIntensity();

          if (intensity > height) {
            height = intensity;
            info.peakIndex = scanNumbers.get(i).getScanNumber();
          }
        }

        info.leftApexIndex = scanNumbers.get(0).getScanNumber();
        info.rightApexIndex = scanNumbers.get(scanNumbers.size() - 1).getScanNumber();
        info.retTime = peak.getRT();
        info.mzValue = peak.getMZ();
        info.intensity = peak.getHeight();
        info.leftPeakIndex = info.leftApexIndex;
        info.rightPeakIndex = info.rightApexIndex;

      } catch (Exception e) {
        logger.info("Skipping " + row + ": " + e.getMessage());
        continue;
      }

      peaks.add(new Peak(chromatogram, info));
    }

    FeatureTools.correctPeakBoundaries(peaks, edgeToHeightThreshold, deltaToHeightThreshold);

    return peaks;
  }

  /**
   * Performs ADAP Peak Decomposition
   *
   * @param peaks list of Peaks
   * @return Collection of dulab.adap.Component objects
   */

  private List<Component> getComponents(List<Peak> peaks) {
    // -----------------------------
    // ADAP Decomposition Parameters
    // -----------------------------

    TwoStepDecompositionParameters params = new TwoStepDecompositionParameters();

    params.minClusterDistance = this.parameters
        .getParameter(ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_DISTANCE).getValue();
    params.minClusterSize =
        this.parameters.getParameter(ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_SIZE).getValue();
    params.minClusterIntensity = this.parameters
        .getParameter(ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_INTENSITY).getValue();
    params.useIsShared =
        this.parameters.getParameter(ADAP3DecompositionV1_5Parameters.USE_ISSHARED).getValue();
    params.edgeToHeightRatio = this.parameters
        .getParameter(ADAP3DecompositionV1_5Parameters.EDGE_TO_HEIGHT_RATIO).getValue();
    params.deltaToHeightRatio = this.parameters
        .getParameter(ADAP3DecompositionV1_5Parameters.DELTA_TO_HEIGHT_RATIO).getValue();
    params.shapeSimThreshold = this.parameters
        .getParameter(ADAP3DecompositionV1_5Parameters.SHAPE_SIM_THRESHOLD).getValue();
    params.minModelPeakSharpness = this.parameters
        .getParameter(ADAP3DecompositionV1_5Parameters.MIN_MODEL_SHARPNESS).getValue();
    params.modelPeakChoice =
        this.parameters.getParameter(ADAP3DecompositionV1_5Parameters.MODEL_PEAK_CHOICE).getValue();
    params.deprecatedMZValues =
        this.parameters.getParameter(ADAP3DecompositionV1_5Parameters.MZ_VALUES).getValue();

    return decomposition.run(params, peaks);
  }
}
