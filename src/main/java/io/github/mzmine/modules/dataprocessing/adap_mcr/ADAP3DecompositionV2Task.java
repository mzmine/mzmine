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
package io.github.mzmine.modules.dataprocessing.adap_mcr;

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
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureShapeType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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
  private final Decomposition decomposition;
  private final RawDataFile dataFile;
  private final String suffix;
  // User parameters
  private final ParameterSet parameters;

  ADAP3DecompositionV2Task(final MZmineProject project, final ChromatogramPeakPair lists,
      RawDataFile dataFile, final ParameterSet parameterSet, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    // Initialize.
    this.project = project;
    parameters = parameterSet;
    originalLists = lists;
    decomposition = new Decomposition();
    this.dataFile = dataFile;
    this.suffix = parameters.getParameter(ADAP3DecompositionV2Parameters.SUFFIX).getValue();
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

          ModularFeatureList newPeakList = decomposePeaks(originalLists);

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
    ModularFeatureList resolvedPeakList = new ModularFeatureList(dataFile + " " + suffix,
        getMemoryMapStorage(), dataFile);
    DataTypeUtils.addDefaultChromatographicTypeColumns(resolvedPeakList);

    resolvedPeakList.setSelectedScans(dataFile, lists.chromatograms.getSeletedScans(dataFile));

    // Load previous applied methods.
    for (final FeatureList.FeatureListAppliedMethod method : lists.peaks.getAppliedMethods()) {
      resolvedPeakList.addDescriptionOfAppliedTask(method);
    }

    // Add task description to feature list.
    resolvedPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Peak deconvolution by ADAP-3",
            ADAPMultivariateCurveResolutionModule.class, parameters, getModuleCallDate()));

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
      Feature refPeak = getFeature(dataFile, component, resolvedPeakList);

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

      // todo: replace this with it's own data type?
      refPeak.setIsotopePattern(
          new SimpleIsotopePattern(dataPoints.toArray(new DataPoint[dataPoints.size()]), -1,
              IsotopePattern.IsotopePatternStatus.PREDICTED, "Spectrum"));

      final ModularFeatureListRow row = new ModularFeatureListRow(resolvedPeakList, ++rowID);

      row.addFeature(dataFile, refPeak);
      row.set(FeatureShapeType.class, true);

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

    params.prefWindowWidth = parameters.getParameter(
        ADAP3DecompositionV2Parameters.PREF_WINDOW_WIDTH).getValue();
    params.retTimeTolerance = parameters.getParameter(
        ADAP3DecompositionV2Parameters.RET_TIME_TOLERANCE).getValue();
    params.minClusterSize = parameters.getParameter(ADAP3DecompositionV2Parameters.MIN_CLUSTER_SIZE)
        .getValue();
    params.adjustApexRetTimes = parameters.getParameter(
        ADAP3DecompositionV2Parameters.ADJUST_APEX_RET_TIME).getValue();

    return decomposition.run(params, chromatograms, peaks);
  }

  @NotNull
  private Feature getFeature(@NotNull RawDataFile file, @NotNull BetterPeak peak,
      ModularFeatureList resolvedFeatureList) {
    Chromatogram chromatogram = peak.chromatogram;

    // todo: can the scans be passed along from the original features so we dont have to go through all scans here?
    // Retrieve scan numbers
    List<Scan> scans = new ArrayList<>(chromatogram.length);
    for (Scan num : file.getScans()) {
      double retTime = num.getRetentionTime();
      Double intensity = chromatogram.getIntensity(retTime, false);
      if (intensity != null) { // warning: now it's not guaranteed that the mzs, intensities and scans have the same number of values (required)
        scans.add(num);
      }
    }

    double[] mzs = new double[chromatogram.length];
    Arrays.fill(mzs, peak.getMZ()); // todo: use mzs from the actual scans (get from original feature?)

    IonTimeSeries<Scan> series = new SimpleIonTimeSeries(resolvedFeatureList.getMemoryMapStorage(),
        mzs, chromatogram.ys, scans);

    // calculations done in the constructor by FeatureDataUtils
    return new ModularFeature(resolvedFeatureList, dataFile, series, FeatureStatus.MANUAL);
  }

  @Override
  public void cancel() {
    decomposition.cancel();
    super.cancel();
  }
}
