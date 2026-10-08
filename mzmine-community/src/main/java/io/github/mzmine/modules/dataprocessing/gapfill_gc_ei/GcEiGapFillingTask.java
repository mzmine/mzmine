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

package io.github.mzmine.modules.dataprocessing.gapfill_gc_ei;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Gap filling for GC-EI feature lists. Re-detects the top signals of the pseudo spectrum of each
 * row in samples without a feature, using the original chromatogram builder and resolver settings.
 * See {@link GcEiRawFileGapFiller}.
 */
public class GcEiGapFillingTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(GcEiGapFillingTask.class.getName());

  private final @NotNull MZmineProject project;
  private final @NotNull ModularFeatureList originalList;
  private final @NotNull OriginalFeatureListOption handleOriginal;
  private final @NotNull String suffix;
  private final int numTopSignals;
  private final @NotNull RTTolerance rtTolerance;
  private final @NotNull RTTolerance coelutionTolerance;
  private @Nullable ModularFeatureList processedList;

  public GcEiGapFillingTask(@NotNull final MZmineProject project,
      @NotNull final FeatureList featureList, @Nullable final MemoryMapStorage storage,
      @NotNull final Instant moduleCallDate, @NotNull final ParameterSet parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.project = project;
    originalList = (ModularFeatureList) featureList;
    handleOriginal = parameters.getValue(GcEiGapFillingParameters.HANDLE_ORIGINAL);
    suffix = parameters.getValue(GcEiGapFillingParameters.SUFFIX);
    numTopSignals = parameters.getValue(GcEiGapFillingParameters.NUMBER_OF_TOP_SIGNALS);
    rtTolerance = parameters.getValue(GcEiGapFillingParameters.RT_TOLERANCE);
    coelutionTolerance = parameters.getValue(GcEiGapFillingParameters.COELUTION_RT_TOLERANCE);
  }

  @Override
  protected void process() {
    final GcEiFeatureFindingSettings settings;
    try {
      settings = GcEiFeatureFindingSettings.fromAppliedMethods(originalList.getAppliedMethods());
    } catch (IllegalStateException e) {
      error(e.getMessage());
      return;
    }

    final int numRows = originalList.getNumberOfRows();
    final int numRaws = originalList.getNumberOfRawDataFiles();
    final ModularFeatureList targetList = switch (handleOriginal) {
      case PROCESS_IN_PLACE -> originalList;
      // estimate the number of features to avoid resizing
      case KEEP, REMOVE ->
          FeatureListUtils.createCopy(originalList, null, suffix, getMemoryMapStorage(), true,
              originalList.getRawDataFiles(), false, numRows,
              FeatureListUtils.estimateFeatures(numRows, numRaws));
    };
    processedList = targetList;

    // collect all gaps before rows are modified in parallel
    final Map<RawDataFile, List<GcEiGapFillTarget>> gapsPerFile = collectGaps(targetList,
        settings.mzTolerance());
    totalItems = gapsPerFile.size();
    final long totalGaps = gapsPerFile.values().stream().mapToLong(List::size).sum();
    logger.info(() -> "Started GC-EI gap filling of %d gaps in %d raw data files of %s".formatted(
        totalGaps, gapsPerFile.size(), originalList.getName()));

    final long filled = gapsPerFile.entrySet().parallelStream().mapToLong(entry -> {
      if (isCanceled()) {
        return 0;
      }
      final GcEiRawFileGapFiller filler = new GcEiRawFileGapFiller(targetList, entry.getKey(),
          settings, rtTolerance, coelutionTolerance);
      final int filledInFile = filler.fillGaps(entry.getValue(), this);
      incrementFinishedItems();
      return filledInFile;
    }).sum();

    if (isCanceled()) {
      return;
    }

    // features were added without updating the row bindings
    targetList.applyRowBindings();
    handleOriginal.reflectNewFeatureListToProject(suffix, project, targetList, originalList);

    logger.info(
        () -> "Finished GC-EI gap filling of %s: filled %d of %d gaps".formatted(
            originalList.getName(), filled, totalGaps));
  }

  /**
   * @return the gaps (rows without feature) for each raw data file. Rows without pseudo spectrum
   * are skipped.
   */
  private @NotNull Map<RawDataFile, List<GcEiGapFillTarget>> collectGaps(
      @NotNull final ModularFeatureList flist, @NotNull final MZTolerance mzTolerance) {
    final Map<RawDataFile, List<GcEiGapFillTarget>> gapsPerFile = new LinkedHashMap<>();
    int rowsWithoutPseudoSpectrum = 0;
    for (final FeatureListRow row : flist.getRows()) {
      final GcEiGapFillTarget target = GcEiGapFillTarget.fromRow(row, numTopSignals, mzTolerance);
      if (target == null) {
        rowsWithoutPseudoSpectrum++;
        continue;
      }
      for (final RawDataFile file : flist.getRawDataFiles()) {
        final Feature feature = row.getFeature(file);
        if (feature == null || feature.getFeatureStatus() == FeatureStatus.UNKNOWN) {
          gapsPerFile.computeIfAbsent(file, _ -> new ArrayList<>()).add(target);
        }
      }
    }

    if (rowsWithoutPseudoSpectrum > 0) {
      final int skipped = rowsWithoutPseudoSpectrum;
      logger.warning(() -> """
          %d rows of %s have no pseudo spectrum and were not gap-filled. \
          Run the GC-EI spectral deconvolution before alignment and gap filling.""".formatted(
          skipped, flist.getName()));
    }
    return gapsPerFile;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return processedList == null ? List.of() : List.of(processedList);
  }

  @Override
  public String getTaskDescription() {
    return "GC-EI gap filling of " + originalList.getName();
  }
}
