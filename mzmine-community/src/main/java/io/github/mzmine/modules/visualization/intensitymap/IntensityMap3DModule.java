/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.intensitymap;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapPerspective;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapProjection;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapTopView;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapLayer;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapSampler;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interactive 3D surface for LC-MS, mobility frames, and imaging data.
 */
public class IntensityMap3DModule implements MZmineRunnableModule {

  private static final Logger logger = Logger.getLogger(IntensityMap3DModule.class.getName());

  @Override
  public @NotNull String getName() {
    return "3D visualizer";
  }

  @Override
  public @NotNull String getDescription() {
    return "Explore LC-MS, ion mobility frames, and imaging intensities as a 3D surface.";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return IntensityMapParameters.class;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    return open(parameters, List.of(), IntensityMapProjection.PERSPECTIVE, null);
  }

  /**
   * Opens the visualizer after checking 3D support and matching data dimensions.
   *
   * @param mzRanges   initial m/z overlays, empty for the complete m/z range of the parameters
   * @param projection the 3D view, or the 2D view of {@link IntensityMap2DModule}
   * @param labelList  feature list of the initial peak labels, null for none
   */
  static @NotNull ExitCode open(@NotNull final ParameterSet parameters,
      @NotNull final List<Range<Double>> mzRanges, @NotNull final IntensityMapProjection projection,
      @Nullable final FeatureList labelList) {
    if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
      MZmineCore.getDesktop().displayErrorMessage("The platform does not provide 3D support.");
      return ExitCode.ERROR;
    }
    final RawDataFile[] files = parameters.getValue(IntensityMapParameters.dataFile)
        .getMatchingRawDataFiles();
    if (files.length == 0) {
      MZmineCore.getDesktop().displayErrorMessage("Select one or more raw data files.");
      return ExitCode.ERROR;
    }
    final var selection = parameters.getValue(IntensityMapParameters.dataFile);
    logger.fine(
        () -> "3D visualizer opens " + files.length + " files (" + selection.getSelectionType()
            + "): " + Arrays.stream(files).map(RawDataFile::getName)
            .collect(Collectors.joining(", ")));
    final var requested = parameters.getValue(IntensityMapParameters.mode);
    final Map<IntensityMapDimensions, List<String>> byMode = new EnumMap<>(
        IntensityMapDimensions.class);
    for (final RawDataFile file : files) {
      byMode.computeIfAbsent(IntensityMapSampler.resolveMode(file, requested),
          _ -> new ArrayList<>()).add(file.getName());
    }
    if (byMode.size() > 1) {
      // name the files, the selection of the dialog may differ from the project selection
      final String groups = byMode.entrySet().stream()
          .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
          .collect(Collectors.joining("\n"));
      MZmineCore.getDesktop().displayErrorMessage(
          "Overlaid samples must have matching coordinate dimensions. The raw data file selection contains:\n"
              + groups + "\nSelect LC-MS, mobility frames, or imaging files together.");
      return ExitCode.ERROR;
    }
    MZmineCore.getDesktop()
        .addTab(new IntensityMapTab(files, parameters, mzRanges, projection, labelList));
    return ExitCode.OK;
  }

  /**
   * Opens the visualizer for the raw data files selected in the main window. The run gets the files
   * selected at this moment, not those of an earlier evaluation of the stored selection; the stored
   * parameters keep "As selected in main window" for later runs.
   *
   * @param module the 2D or the 3D module
   * @param dialog show the module dialog first
   */
  public static void showSelectedFiles(@NotNull final Class<? extends MZmineRunnableModule> module,
      final boolean dialog) {
    final RawDataFile[] selected = MZmineGUI.getSelectedRawDataFiles().toArray(new RawDataFile[0]);
    logger.fine(() -> "Visualizer for the selected files: " + Arrays.stream(selected)
        .map(RawDataFile::getName).collect(Collectors.joining(", ")));
    final ParameterSet parameters = ConfigService.getConfiguration().getModuleParameters(module);
    // a new selection, because the stored one keeps the files of its last evaluation
    parameters.getParameter(IntensityMapParameters.dataFile)
        .setValue(new RawDataFilesSelection(RawDataFilesSelectionType.GUI_SELECTED_FILES));
    if (dialog && parameters.showSetupDialog(true) != ExitCode.OK) {
      return;
    }
    final ParameterSet run = parameters.cloneParameterSet();
    final RawDataFilesParameter files = run.getParameter(IntensityMapParameters.dataFile);
    // decision: files chosen differently in the dialog win over the selection in the main window
    if (files.getValue().getSelectionType() == RawDataFilesSelectionType.GUI_SELECTED_FILES) {
      files.setValue(new RawDataFilesSelection(selected));
    }
    MZmineCore.runMZmineModule(module, run);
  }

  /**
   * Opens the images of a feature and of its co-located features side by side in the 2D visualizer,
   * without the module dialog. The spectrum marks the m/z ranges of all images.
   *
   * @param selected  feature of an imaging file
   * @param colocated co-located features of the same file, most similar first; the images keep this
   *                  order
   */
  public static void showColocatedImages(@NotNull final Feature selected,
      @NotNull final List<? extends Feature> colocated) {
    final List<Range<Double>> mzRanges = new ArrayList<>();
    Stream.concat(Stream.of(selected), colocated.stream()).map(Feature::getRawDataPointsMZRange)
        .filter(Objects::nonNull).forEach(range -> addMerged(mzRanges, range));
    final ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(IntensityMap2DModule.class).cloneParameterSet();
    parameters.getParameter(IntensityMapParameters.dataFile)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES,
            new RawDataFile[]{selected.getRawDataFile()});
    parameters.getParameter(IntensityMapParameters.mode).setValue(IntensityMapDimensions.AUTOMATIC);
    parameters.getParameter(IntensityMapParameters.scanSelection).setValue(
        new ScanSelection(1, null,
            Objects.requireNonNullElse(selected.getRepresentativePolarity(), PolarityType.ANY)));
    mzRanges.stream().reduce(Range::span)
        .ifPresent(span -> parameters.getParameter(IntensityMapParameters.mzRange).setValue(span));
    open(parameters, mzRanges, IntensityMapProjection.TOP_VIEW, selected.getFeatureList());
  }

  /**
   * Adds a range, or merges it into an overlapping one at its position, so that the order of the
   * first appearance is kept.
   */
  static void addMerged(@NotNull final List<Range<Double>> ranges,
      @NotNull final Range<Double> range) {
    for (int i = 0; i < ranges.size(); i++) {
      if (ranges.get(i).isConnected(range)) {
        ranges.set(i, ranges.get(i).span(range));
        return;
      }
    }
    ranges.add(range);
  }

  /**
   * Opens the visualizer for selected features: one overlay per feature m/z range for every raw
   * file of their feature list, side by side, restricted to their retention time range and
   * polarity. The ranges include the features of the same rows in all files, so shifts between
   * samples stay in view. Overlapping m/z ranges are merged; imaging data ignore the retention
   * time. The feature list of the features labels the peaks.
   *
   * @param projection the 3D view, or the 2D view of {@link IntensityMap2DModule}
   */
  public static void showFeatures(@NotNull final List<? extends Feature> features,
      @NotNull final IntensityMapProjection projection) {
    if (features.isEmpty()) {
      return;
    }
    final FeatureList featureList = features.getFirst().getFeatureList();
    // decision: include all samples of the feature list, their overlays show side by side
    final RawDataFile[] files = Stream.concat(featureList.getRawDataFiles().stream(),
        features.stream().map(Feature::getRawDataFile)).distinct().toArray(RawDataFile[]::new);
    // the same rows in the other samples, e.g. with slightly shifted retention times
    final List<Feature> rowFeatures = Stream.concat(features.stream(),
        features.stream().map(Feature::getRow).filter(Objects::nonNull)
            .flatMap(row -> row.getFeatures().stream())
            .filter(feature -> feature.getFeatureStatus() != FeatureStatus.UNKNOWN)).filter(
        feature -> feature.getRawDataPointsMZRange() != null
            && feature.getRawDataPointsRTRange() != null).distinct().toList();
    final List<Range<Double>> mzRanges = rowFeatures.stream().map(Feature::getRawDataPointsMZRange)
        .toList();
    final Range<Float> rtRange = rowFeatures.stream().map(Feature::getRawDataPointsRTRange)
        .reduce(Range::span).orElse(null);
    final List<PolarityType> polarities = rowFeatures.stream()
        .map(Feature::getRepresentativePolarity).distinct().toList();
    // decision: features of different polarities show all scans
    final PolarityType polarity = polarities.size() == 1 ? polarities.getFirst() : PolarityType.ANY;
    // decision: a copy of the settings of the module of this view, so feature ranges do not
    // become the defaults of the module dialog
    final Class<? extends MZmineRunnableModule> module = switch (projection) {
      case IntensityMapTopView _ -> IntensityMap2DModule.class;
      case IntensityMapPerspective _ -> IntensityMap3DModule.class;
    };
    final ParameterSet parameters = ConfigService.getConfiguration().getModuleParameters(module)
        .cloneParameterSet();
    parameters.getParameter(IntensityMapParameters.dataFile)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES, files);
    // imaging features show images, all others retention time
    parameters.getParameter(IntensityMapParameters.mode).setValue(IntensityMapDimensions.AUTOMATIC);
    final boolean imaging = Arrays.stream(files).anyMatch(ImagingRawDataFile.class::isInstance);
    parameters.getParameter(IntensityMapParameters.scanSelection).setValue(
        new ScanSelection(1, imaging ? null : rtRange,
            Objects.requireNonNullElse(polarity, PolarityType.ANY)));
    final List<Range<Double>> merged = IntensityMapLayer.mergeOverlapping(mzRanges);
    merged.stream().reduce(Range::span)
        .ifPresent(span -> parameters.getParameter(IntensityMapParameters.mzRange).setValue(span));
    if (parameters.showSetupDialog(true) == ExitCode.OK) {
      // decision: label peaks with the feature list the features were opened from
      open(parameters, merged, projection, featureList);
    }
  }
}
