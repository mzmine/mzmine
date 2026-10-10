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

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationParameters;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.DataFileStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.FeatureRecord;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.InterSampleRtStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataPreparation;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The raw data estimates for a wizard sequence, shared by the wizard's estimate action and the
 * optimizer, see {@link #estimate}.
 */
public record WizardParameterEstimationResult(@NotNull ParameterEstimationContext context,
                                              @NotNull PreparedParameterSet estimates) {

  /**
   * Computes the per-file statistics and the cross-file analysis of the imported files, offers the
   * presets that fit the raw data, and prepares all estimates. No batch is run.
   *
   * @param benchmarkFeatures  additional target features, or null
   * @param sequence           the wizard sequence, is not modified
   * @param preclassification  the settings fixed by the pre-classification, see
   *                           {@link PreclassificationParameters}
   * @param presetConfirmation called with the fitting presets if they differ from the sequence,
   *                           returns true to estimate for them
   * @param canceled           checked between the steps
   * @return the estimates, null if canceled
   * @throws IllegalStateException if no file was imported or no file contains isotope signals
   */
  public static @Nullable WizardParameterEstimationResult estimate(
      @NotNull List<RawDataFile> importedFiles, @Nullable List<FeatureRecord> benchmarkFeatures,
      @NotNull WizardSequence sequence, @NotNull ParameterSet preclassification,
      @NotNull Predicate<@NotNull PresetSelection> presetConfirmation,
      @Nullable MemoryMapStorage storage, @NotNull BooleanSupplier canceled) {
    if (importedFiles.isEmpty()) {
      throw new IllegalStateException("None of the selected raw data files could be imported.");
    }
    final PolarityType polarity = preclassification.getValue(PreclassificationParameters.polarity)
        .toScanPolaritySelection();
    final List<DataFileStatistics> statistics = RawDataPreparation.computeFileStatistics(
        importedFiles, benchmarkFeatures, storage, polarity);
    RawDataPreparation.requireIsotopeSignals(statistics);
    if (canceled.getAsBoolean()) {
      return null;
    }

    final RawDataAnalysis analysis = RawDataAnalysis.analyze(statistics);
    if (canceled.getAsBoolean()) {
      return null;
    }
    final ParameterEstimationContext context = ParameterEstimationContext.withFittingPresets(
        analysis, sequence, preclassification, presetConfirmation);
    if (canceled.getAsBoolean()) {
      return null;
    }
    return new WizardParameterEstimationResult(context, PreparedParameterSet.prepare(context));
  }

  public @NotNull List<DataFileStatistics> statistics() {
    return context.analysis().files();
  }

  public @NotNull InterSampleRtStatistics interSampleRtStatistics() {
    return ParameterEstimators.interSampleRtStatistics(context.analysis());
  }
}
