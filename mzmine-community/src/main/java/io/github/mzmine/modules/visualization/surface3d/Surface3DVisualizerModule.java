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

package io.github.mzmine.modules.visualization.surface3d;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.visualization.surface3d.sampling.Surface3DLayer;
import io.github.mzmine.modules.visualization.surface3d.sampling.Surface3DSampler;
import io.github.mzmine.parameters.ParameterSet;
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
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * Interactive 3D surface for LC-MS, mobility frames, and imaging data.
 */
public class Surface3DVisualizerModule implements MZmineRunnableModule {

  private static final Logger logger = Logger.getLogger(
      Surface3DVisualizerModule.class.getName());

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
    return Surface3DVisualizerParameters.class;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    return open(parameters, List.of());
  }

  /**
   * Opens the visualizer after checking 3D support and matching data dimensions.
   *
   * @param mzRanges initial m/z overlays, empty for the complete m/z range of the parameters
   */
  public static @NotNull ExitCode open(@NotNull final ParameterSet parameters,
      @NotNull final List<Range<Double>> mzRanges) {
    if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
      MZmineCore.getDesktop().displayErrorMessage("The platform does not provide 3D support.");
      return ExitCode.ERROR;
    }
    final RawDataFile[] files = parameters.getValue(Surface3DVisualizerParameters.dataFile)
        .getMatchingRawDataFiles();
    if (files.length == 0) {
      MZmineCore.getDesktop().displayErrorMessage("Select one or more raw data files.");
      return ExitCode.ERROR;
    }
    final var selection = parameters.getValue(Surface3DVisualizerParameters.dataFile);
    logger.fine(() -> "3D visualizer opens " + files.length + " files ("
        + selection.getSelectionType() + "): " + Arrays.stream(files).map(RawDataFile::getName)
        .collect(Collectors.joining(", ")));
    final var requested = parameters.getValue(Surface3DVisualizerParameters.mode);
    final Map<Surface3DDataMode, List<String>> byMode = new EnumMap<>(Surface3DDataMode.class);
    for (final RawDataFile file : files) {
      byMode.computeIfAbsent(Surface3DSampler.resolveMode(file, requested),
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
        .addTab(new Surface3DVisualizerTab(files, parameters.cloneParameterSet(), mzRanges));
    return ExitCode.OK;
  }

  /**
   * Opens the visualizer for selected features: one overlay per feature m/z range for every raw
   * file of the features, restricted to their retention time range and polarity. Overlapping m/z
   * ranges are merged; imaging data ignore the retention time.
   */
  public static void showFeatures(@NotNull final List<? extends Feature> features) {
    if (features.isEmpty()) {
      return;
    }
    final RawDataFile[] files = features.stream().map(Feature::getRawDataFile).distinct()
        .toArray(RawDataFile[]::new);
    final List<Range<Double>> mzRanges = features.stream().map(Feature::getRawDataPointsMZRange)
        .toList();
    final Range<Float> rtRange = features.stream().map(Feature::getRawDataPointsRTRange)
        .reduce(Range::span).orElse(null);
    final List<PolarityType> polarities = features.stream()
        .map(Feature::getRepresentativePolarity).distinct().toList();
    // decision: features of different polarities show all scans
    final PolarityType polarity = polarities.size() == 1 ? polarities.getFirst() : PolarityType.ANY;
    // decision: a copy, feature ranges must not become the defaults of the module dialog
    final ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(Surface3DVisualizerModule.class).cloneParameterSet();
    parameters.getParameter(Surface3DVisualizerParameters.dataFile)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES, files);
    // imaging features show images, all others retention time
    parameters.getParameter(Surface3DVisualizerParameters.mode)
        .setValue(Surface3DDataMode.AUTOMATIC);
    final boolean imaging = Arrays.stream(files).anyMatch(ImagingRawDataFile.class::isInstance);
    parameters.getParameter(Surface3DVisualizerParameters.scanSelection).setValue(
        new ScanSelection(1, imaging ? null : rtRange,
            Objects.requireNonNullElse(polarity, PolarityType.ANY)));
    final List<Range<Double>> merged = Surface3DLayer.mergeOverlapping(mzRanges);
    merged.stream().reduce(Range::span).ifPresent(
        span -> parameters.getParameter(Surface3DVisualizerParameters.mzRange).setValue(span));
    if (parameters.showSetupDialog(true) == ExitCode.OK) {
      open(parameters, merged);
    }
  }
}
