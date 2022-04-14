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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.Task;
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
    setupNewTICVisualizer(MZmineCore.getProjectManager().getCurrentProject().getDataFiles(),
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

      myInstance.runModule(MZmineCore.getProjectManager().getCurrentProject(), p,
          new ArrayList<Task>(), Instant.now()); // date is irrelevant
    }

  }

  public static void showNewTICVisualizerWindow(final RawDataFile[] dataFiles,
      final Feature[] selectionPeaks, final Map<Feature, String> peakLabels,
      final ScanSelection scanSelection, final TICPlotType plotType, final Range<Double> mzRange) {

    TICVisualizerTab window = new TICVisualizerTab(dataFiles, plotType, scanSelection,
        mzRange, Arrays.asList(selectionPeaks), peakLabels);
    MZmineCore.getDesktop().addTab(window);
  }

  public static void showNewTICVisualizerWindow(final RawDataFile[] dataFiles,
      final List<Feature> selectionPeaks, final Map<Feature, String> peakLabels,
      final ScanSelection scanSelection, final TICPlotType plotType, final Range<Double> mzRange) {

    TICVisualizerTab window = new TICVisualizerTab(dataFiles, plotType, scanSelection,
        mzRange, selectionPeaks, peakLabels);
    MZmineCore.getDesktop().addTab(window);
  }

  public static void visualizeFeatureListRows(List<ModularFeatureListRow> rows, List<ModularFeature> selectedFeatures) {
    final Map<Feature, String> labelsMap = new HashMap<>();
    final Set<RawDataFile> files = new HashSet<>();

    Range<Double> mzRange = null;
    for (ModularFeatureListRow row : rows) {
      for (ModularFeature feature : selectedFeatures) {
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
        MZmineCore.getProjectManager().getCurrentProject().getDataFiles(),
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
      TICVisualizerTab window = new TICVisualizerTab(dataFiles, plotType, scanSelection,
          mzRange, selectionPeaks, ((TICVisualizerParameters) parameters).getPeakLabelMap());
      MZmineCore.getDesktop().addTab(window);

    } else {

      MZmineCore.getDesktop().displayErrorMessage("No scans found");
    }

    return ExitCode.OK;
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
