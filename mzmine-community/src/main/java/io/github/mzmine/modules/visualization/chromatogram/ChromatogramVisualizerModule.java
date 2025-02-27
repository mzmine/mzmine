/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.chromatogram;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.mvci.LatestTaskScheduler;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TIC/XIC visualizer using JFreeChart library
 */
public class ChromatogramVisualizerModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "TIC/XIC visualizer";
  private static final String MODULE_DESCRIPTION = "TIC/XIC visualizer."; // TODO

  public static void setupNewTICVisualizer(final RawDataFile dataFile) {

    setupNewTICVisualizer(new RawDataFile[]{dataFile});
  }

  public static void setupNewTICVisualizer(final RawDataFile[] dataFiles) {
    setupNewTICVisualizer(ProjectService.getProjectManager().getCurrentProject().getDataFiles(),
        dataFiles, new Feature[0], new Feature[0], null, null, null);
  }

  public static void setupNewTICVisualizer(final RawDataFile[] allFiles,
      final RawDataFile[] selectedFiles, final Feature[] allPeaks, final Feature[] selectedPeaks,
      final Map<Feature, String> peakLabels, ScanSelection scanSelection,
      final Range<Double> mzRange) {

    assert allFiles != null;

    final ChromatogramVisualizerModule myInstance =
        MZmineCore.getModuleInstance(ChromatogramVisualizerModule.class);
    final TICVisualizerParameters myParameters = (TICVisualizerParameters) MZmineCore
        .getConfiguration().getModuleParameters(ChromatogramVisualizerModule.class);
    myParameters.getParameter(TICVisualizerParameters.PLOT_TYPE).setValue(TICPlotType.BASEPEAK);

    if (scanSelection != null) {
      myParameters.getParameter(TICVisualizerParameters.scanSelection).setValue(scanSelection);
    }

    if (mzRange != null) {
      myParameters.getParameter(TICVisualizerParameters.MZ_RANGE).setValue(mzRange);
    }

    if (myParameters.showSetupDialog(true, allFiles, selectedFiles, allPeaks,
        selectedPeaks) == ExitCode.OK) {

      final TICVisualizerParameters p = (TICVisualizerParameters) myParameters.cloneParameterSet();

      if (peakLabels != null) {
        p.setPeakLabelMap(peakLabels);
      }

      myInstance.runModule(ProjectService.getProjectManager().getCurrentProject(), p,
          new ArrayList<Task>(), Instant.now()); // date is irrelevant
    }

  }

  public static void showNewTICVisualizerWindow(final RawDataFile[] dataFiles,
      final Feature[] selectionPeaks, final Map<Feature, String> peakLabels,
      final ScanSelection scanSelection, final TICPlotType plotType, final Range<Double> mzRange) {

    // this method is the quick visualization in table
    // maximum samples to show as line. greater than that will be displayed only as feature shape
    final int ticMaxSamples = 50; // don't show lines for so many samples
    createTICPlotInTask(dataFiles, plotType, scanSelection, mzRange,
        Arrays.asList(selectionPeaks), peakLabels, ticMaxSamples);
  }

  public static void showNewTICVisualizerWindow(final RawDataFile[] dataFiles,
      final List<Feature> selectionPeaks, final Map<Feature, String> peakLabels,
      final ScanSelection scanSelection, final TICPlotType plotType, final Range<Double> mzRange) {

    createTICPlotInTask(dataFiles, plotType, scanSelection, mzRange,
        selectionPeaks, peakLabels, null);
  }

  public static void visualizeFeatureListRows(Collection<ModularFeatureListRow> rows) {
    final Map<Feature, String> labelsMap = new HashMap<>();
    final Set<RawDataFile> files = new HashSet<>();

    Range<Double> mzRange = null;
    final List<Feature> selectedFeatures = new ArrayList<>();
    for (ModularFeatureListRow row : rows) {
      for (final Feature f : row.getFeatures()) {
        final ModularFeature feature = (ModularFeature) f;
        if(feature == null || feature.getFeatureStatus() == FeatureStatus.UNKNOWN) {
          continue;
        }
        if (mzRange == null) {
          mzRange = feature.getRawDataPointsMZRange();
          double upper = mzRange.upperEndpoint();
          double lower = mzRange.lowerEndpoint();
          if ((upper - lower) < 0.000001) {
            // Workaround to make ultra narrow mzRanges (e.g. from imported mzTab peaklist),
            // a more reasonable default for a HRAM instrument (~5ppm)
            double fiveppm = (upper * 5E-6);
            mzRange = Range.closed(lower - fiveppm, upper + fiveppm);
          }
        } else {
          mzRange = mzRange.span(feature.getRawDataPointsMZRange());
        }

        selectedFeatures.add(feature);

        // Label the peak with the row's preferred identity.
        final FeatureIdentity identity = row.getPreferredFeatureIdentity();
        if (identity != null) {
          labelsMap.put(feature, identity.getName());
        }
        files.add(feature.getRawDataFile());
      }
    }
    ScanSelection scanSelection = new ScanSelection(1);

    showNewTICVisualizerWindow(files.toArray(new RawDataFile[0]),
        selectedFeatures.toArray(new Feature[selectedFeatures.size()]), labelsMap, scanSelection,
        TICPlotType.BASEPEAK, mzRange);
  }

  public static void setUpVisualiserFromFeatures(Collection<ModularFeatureListRow> rows,
      @Nullable RawDataFile selectedFile) {
    // Map peaks to their identity labels.
    final Map<Feature, String> labelsMap = new HashMap<>();

    Range<Double> mzRange = null;
    final ArrayList<Feature> allFeatures = new ArrayList<>();
    final ArrayList<Feature> selectedFeatures = new ArrayList<>();
    final Set<RawDataFile> allFiles = new HashSet<>();
    allFiles.addAll(rows.stream().flatMap(row -> row.getRawDataFiles().stream()).
        collect(Collectors.toSet()));

    for (final ModularFeatureListRow row : rows) {

      // Label the peak with the row's preferred identity.
      final FeatureIdentity identity = row.getPreferredFeatureIdentity();

      for (final Feature feature : row.getFeatures()) {
        if(feature == null || feature.getFeatureStatus() == FeatureStatus.UNKNOWN) {
          continue;
        }

        allFeatures.add(feature);
        if (feature.getRawDataFile() == selectedFile) {
          selectedFeatures.add(feature);
        }

        if (mzRange == null) {
          mzRange = feature.getRawDataPointsMZRange();
        } else {
          mzRange = mzRange.span(feature.getRawDataPointsMZRange());
        }

        if (identity != null) {
          labelsMap.put(feature, identity.getName());
        }
//        allFiles.add(feature.getRawDataFile());
      }
    }

    ScanSelection scanSelection = new ScanSelection(1);

    setupNewTICVisualizer(
        ProjectService.getProjectManager().getCurrentProject().getDataFiles(),
        allFiles.toArray(new RawDataFile[0]), allFeatures.toArray(new Feature[allFeatures.size()]),
        selectedFeatures.toArray(new Feature[selectedFeatures.size()]), labelsMap,
        scanSelection, mzRange);
  }

  @Override
  public @NotNull
  String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull
  String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    final RawDataFile[] dataFiles = parameters.getParameter(TICVisualizerParameters.DATA_FILES)
        .getValue().getMatchingRawDataFiles();
    final Range<Double> mzRange =
        parameters.getParameter(TICVisualizerParameters.MZ_RANGE).getValue();
    final ScanSelection scanSelection =
        parameters.getParameter(TICVisualizerParameters.scanSelection).getValue();
    final TICPlotType plotType =
        parameters.getParameter(TICVisualizerParameters.PLOT_TYPE).getValue();
    final List<Feature> selectionPeaks =
        parameters.getParameter(TICVisualizerParameters.PEAKS).getValue();

    final Integer ticMaxSamples = parameters.getOptionalValue(
        TICVisualizerParameters.ticMaxSamples).orElse(null);
    // Add the window to the desktop only if we actually have any raw
    // data to show.
    boolean weHaveData = false;
    for (RawDataFile dataFile : dataFiles) {
      Scan selectedScans[] = scanSelection.getMatchingScans(dataFile);
      if (selectedScans.length > 0) {
        weHaveData = true;
      }
    }

    if (weHaveData) {
      createTICPlotInTask(dataFiles, plotType, scanSelection, mzRange,
          selectionPeaks, ((TICVisualizerParameters) parameters).getPeakLabelMap(), ticMaxSamples);

    } else {

      MZmineCore.getDesktop().displayErrorMessage("No scans found");
    }

    return ExitCode.OK;
  }

  private static void createTICPlotInTask(final RawDataFile[] dataFiles, final TICPlotType plotType, final ScanSelection scanSelection,
      final Range<Double> mzRange, final List<Feature> selectionPeaks,
      final Map<Feature, String> peakLabelMap, @Nullable final Integer ticMaxSamples) {
    final LatestTaskScheduler scheduler = new LatestTaskScheduler();
    scheduler.onTaskThread(new FxUpdateTask<>("Creating TIC view", null) {
      private TICVisualizerTab window;

      @Override
      protected void process() {
        window = new TICVisualizerTab(dataFiles, plotType, scanSelection, mzRange, selectionPeaks,
            peakLabelMap, ticMaxSamples);
      }

      @Override
      protected void updateGuiModel() {
        MZmineCore.getDesktop().addTab(window);
      }

      @Override
      public String getTaskDescription() {
        return "Creating TIC view";
      }

      @Override
      public double getFinishedPercentage() {
        return 0;
      }
    }, TaskPriority.HIGH);
  }

  @Override
  public @NotNull
  MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Override
  public @NotNull
  Class<? extends ParameterSet> getParameterSetClass() {
    return TICVisualizerParameters.class;
  }
}
