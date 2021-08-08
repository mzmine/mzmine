/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package io.github.mzmine.modules.dataprocessing.adap_mcr;

import com.google.common.collect.Range;
import dulab.adap.datamodel.BetterComponent;
import dulab.adap.datamodel.BetterPeak;
import dulab.adap.datamodel.Chromatogram;
import dulab.adap.workflow.decomposition.Decomposition;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
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
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionV2Task extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(ADAP3DecompositionV2Task.class.getName());

  private final ADAP3DecompositionV2Utils utils = new ADAP3DecompositionV2Utils();

  // Feature lists.
  private final MZmineProject project;
  private final ChromatogramPeakPair originalLists;
  private ModularFeatureList newPeakList;
  private final Decomposition decomposition;

  // User parameters
  private final ParameterSet parameters;

  ADAP3DecompositionV2Task(final MZmineProject project, final ChromatogramPeakPair lists,
      final ParameterSet parameterSet, @Nullable MemoryMapStorage storage) {
    super(storage);
    // Initialize.
    this.project = project;
    parameters = parameterSet;
    originalLists = lists;
    newPeakList = null;
    decomposition = new Decomposition();
  }

  @Override
  public String getTaskDescription() {
    return "ADAP Peak decomposition on " + originalLists;
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
      logger.info("Started ADAP Peak Decomposition on " + originalLists);

      // Check raw data files.
      if (originalLists.chromatograms.getNumberOfRawDataFiles() > 1
          && originalLists.peaks.getNumberOfRawDataFiles() > 1) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            "Peak Decomposition can only be performed on feature lists with a single raw data file");
      } else {

        try {

          newPeakList = decomposePeaks(originalLists);

          if (!isCanceled()) {

            // Add new peaklist to the project.
            project.addFeatureList(newPeakList);

            // Remove the original peaklist if requested.
            if (parameters.getParameter(ADAP3DecompositionV2Parameters.AUTO_REMOVE).getValue()) {
              project.removeFeatureList(originalLists.chromatograms);
              project.removeFeatureList(originalLists.peaks);
            }

            setStatus(TaskStatus.FINISHED);
            logger.info("Finished peak decomposition on " + originalLists);
          }

        } catch (IllegalArgumentException e) {
          errorMsg = "Incorrect Feature List selected:\n" + e.getMessage();
          e.printStackTrace();
        } catch (IllegalStateException e) {
          errorMsg = "Peak decompostion error:\n" + e.getMessage();
          e.printStackTrace();
        } catch (Exception e) {
          errorMsg = "'Unknown error' during peak decomposition. \n" + e.getMessage();
          e.printStackTrace();
        } catch (Throwable t) {

          setStatus(TaskStatus.ERROR);
          setErrorMessage(t.getMessage());
          t.printStackTrace();
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

  private ModularFeatureList decomposePeaks(@NotNull ChromatogramPeakPair lists) {
    RawDataFile dataFile = lists.chromatograms.getRawDataFile(0);

    // Create new feature list.
    final ModularFeatureList resolvedPeakList = new ModularFeatureList(lists.peaks + " "
        + parameters.getParameter(ADAP3DecompositionV2Parameters.SUFFIX).getValue(), getMemoryMapStorage(), dataFile);

    // Load previous applied methods.
    for (final FeatureList.FeatureListAppliedMethod method : lists.peaks.getAppliedMethods()) {
      resolvedPeakList.addDescriptionOfAppliedTask(method);
    }

    // Add task description to feature list.
    resolvedPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Peak deconvolution by ADAP-3",
            ADAPMultivariateCurveResolutionModule.class, parameters));

    // Collect peak information
    List<BetterPeak> chromatograms = utils.getPeaks(lists.chromatograms);
    List<BetterPeak> peaks = utils.getPeaks(lists.peaks);

    // Find components (a.k.a. clusters of peaks with fragmentation spectra)
    List<BetterComponent> components = getComponents(chromatograms, peaks);

    // Create PeakListRow for each components
    List<FeatureListRow> newPeakListRows = new ArrayList<>();

    int rowID = 0;

    for (final BetterComponent component : components) {
      if (component.spectrum.length == 0 || component.getIntensity() < 1e-12) {
        continue;
      }

      // Create a reference peak
      Feature refPeak = getFeature(dataFile, component);

      // Add spectrum
      List<DataPoint> dataPoints = new ArrayList<>();
      for (int i = 0; i < component.spectrum.length; ++i) {
        double mz = component.spectrum.getMZ(i);
        double intensity = component.spectrum.getIntensity(i);
        if (intensity > 1e-3 * component.getIntensity()) {
          dataPoints.add(new SimpleDataPoint(mz, intensity));
        }
      }

      if (dataPoints.size() < 5) {
        continue;
      }

      refPeak.setIsotopePattern(
          new SimpleIsotopePattern(dataPoints.toArray(new DataPoint[dataPoints.size()]),
              IsotopePattern.IsotopePatternStatus.PREDICTED, "Spectrum"));

      ModularFeatureListRow row = new ModularFeatureListRow((ModularFeatureList) resolvedPeakList,
          ++rowID);

      row.addFeature(dataFile, refPeak);

      // Set row properties
      row.setAverageMZ(refPeak.getMZ());
      row.setAverageRT(refPeak.getRT());

      // resolvedPeakList.addRow(row);
      newPeakListRows.add(row);
    }

    // ------------------------------------
    // Sort new peak rows by retention time
    // ------------------------------------

    newPeakListRows.sort(Comparator.comparingDouble(FeatureListRow::getAverageRT));

    for (FeatureListRow row : newPeakListRows) {
      resolvedPeakList.addRow(row);
    }

    return resolvedPeakList;
  }

  /**
   * Performs ADAP Peak Decomposition
   *
   * @param chromatograms list of {@link BetterPeak} representing chromatograms
   * @param peaks         containing ranges of detected peaks
   * @return Collection of dulab.adap.Component objects
   */

  private List<BetterComponent> getComponents(List<BetterPeak> chromatograms,
      List<BetterPeak> peaks) {
    // -----------------------------
    // ADAP Decomposition Parameters
    // -----------------------------

    Decomposition.Parameters params = new Decomposition.Parameters();

    params.prefWindowWidth =
        parameters.getParameter(ADAP3DecompositionV2Parameters.PREF_WINDOW_WIDTH).getValue();
    params.retTimeTolerance =
        parameters.getParameter(ADAP3DecompositionV2Parameters.RET_TIME_TOLERANCE).getValue();
    params.minClusterSize =
        parameters.getParameter(ADAP3DecompositionV2Parameters.MIN_CLUSTER_SIZE).getValue();
    params.adjustApexRetTimes =
        parameters.getParameter(ADAP3DecompositionV2Parameters.ADJUST_APEX_RET_TIME).getValue();

    return decomposition.run(params, chromatograms, peaks);
  }

  @NotNull
  private Feature getFeature(@NotNull RawDataFile file, @NotNull BetterPeak peak) {
    Chromatogram chromatogram = peak.chromatogram;

    // Retrieve scan numbers
    Scan representativeScan = null;
    Scan[] scanNumbers = new Scan[chromatogram.length];
    int count = 0;
    for (Scan num : file.getScans()) {
      double retTime = num.getRetentionTime();
      Double intensity = chromatogram.getIntensity(retTime, false);
      if (intensity != null) {
        scanNumbers[count++] = num;
      }
      if (retTime == peak.getRetTime()) {
        representativeScan = num;
      }
    }

    // Calculate peak area
    double area = 0.0;
    for (int i = 1; i < chromatogram.length; ++i) {
      double base = (chromatogram.xs[i] - chromatogram.xs[i - 1]) * 60d;
      double height = 0.5 * (chromatogram.ys[i] + chromatogram.ys[i - 1]);
      area += base * height;
    }

    // Create array of DataPoints
    DataPoint[] dataPoints = new DataPoint[chromatogram.length];
    count = 0;
    for (double intensity : chromatogram.ys) {
      dataPoints[count++] = new SimpleDataPoint(peak.getMZ(), intensity);
    }

    ModularFeature newFeature = new ModularFeature(newPeakList, file, peak.getMZ(),
        (float) peak.getRetTime(),
        (float) peak.getIntensity(), (float) area, scanNumbers, dataPoints, FeatureStatus.MANUAL,
        representativeScan, representativeScan, new Scan[]{},
        Range.closed((float) peak.getFirstRetTime(),
            (float) peak.getLastRetTime()), Range.closed(peak.getMZ() - 0.01, peak.getMZ() + 0.01),
        Range.closed(0f, (float) peak.getIntensity()));
    return newFeature;
  }

  @Override
  public void cancel() {
    decomposition.cancel();
    super.cancel();
  }
}
