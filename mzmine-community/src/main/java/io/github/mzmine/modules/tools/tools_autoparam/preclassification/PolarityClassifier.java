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

package io.github.mzmine.modules.tools.tools_autoparam.preclassification;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMsPolarity;
import io.github.mzmine.parameters.UserParameter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

/**
 * Decides the single polarity a wizard run processes. Statistics and batches of polarity switching
 * data are only meaningful for one polarity at a time.
 */
public final class PolarityClassifier implements RawDataClassifier<WizardMsPolarity> {

  /**
   * @param filePolarities polarity of each file by name. {@link PolarityType#ANY} for files with
   *                       both polarities, {@link PolarityType#UNKNOWN} for files without
   * @param wizardPolarity the ion mode currently selected in the wizard
   */
  static @NotNull ClassifierDecision<WizardMsPolarity> decide(
      @NotNull Map<String, PolarityType> filePolarities, @NotNull WizardMsPolarity wizardPolarity) {
    final List<String> positiveOnly = names(filePolarities, PolarityType.POSITIVE::equals);
    final List<String> negativeOnly = names(filePolarities, PolarityType.NEGATIVE::equals);
    if (!positiveOnly.isEmpty() && !negativeOnly.isEmpty()) {
      return ClassifierDecision.conflict("""
          The selected files contain positive only data (%s) and negative only data (%s). No \
          single ion mode matches all files.
          Each wizard run should process a single polarity, use separate wizard runs for the \
          positive and negative files.""".formatted(String.join(", ", positiveOnly),
          String.join(", ", negativeOnly)));
    }

    return switch (wizardPolarity) {
      case Positive, Negative -> {
        final PolarityType required = wizardPolarity.toScanPolaritySelection();
        // files with both polarities also have scans in the required one
        final List<String> missing = names(filePolarities,
            polarity -> polarity != required && polarity != PolarityType.ANY);
        if (!missing.isEmpty()) {
          yield ClassifierDecision.conflict("""
              The ion mode in the wizard is set to %s, but these files have no %s scans: %s
              Change the ion mode or the selected files.""".formatted(wizardPolarity,
              wizardPolarity.toString().toLowerCase(), String.join(", ", missing)));
        }
        yield ClassifierDecision.fixed(wizardPolarity);
      }
      case No_filter -> decideWithoutFilter(filePolarities, positiveOnly, negativeOnly);
    };
  }

  private static @NotNull ClassifierDecision<WizardMsPolarity> decideWithoutFilter(
      @NotNull Map<String, PolarityType> filePolarities, @NotNull List<String> positiveOnly,
      @NotNull List<String> negativeOnly) {
    final List<String> unknown = names(filePolarities, PolarityType.UNKNOWN::equals);
    if (unknown.size() == filePolarities.size()) {
      // no polarity information at all, nothing to filter
      return ClassifierDecision.fixed(WizardMsPolarity.No_filter);
    }
    // decision: filtering by a polarity would remove all scans of files without polarity
    // information, so they cannot be combined with files that have one
    if (!unknown.isEmpty()) {
      return ClassifierDecision.conflict("""
          These files have no polarity information, while the other selected files have: %s
          Filtering by a polarity would remove all their scans. Use separate wizard runs for \
          these files.""".formatted(String.join(", ", unknown)));
    }
    // decision: switch without asking if only one polarity matches all files. Also safer for
    // polarity switching files that were not part of the sampled files
    if (!positiveOnly.isEmpty()) {
      return ClassifierDecision.fixed(WizardMsPolarity.Positive);
    }
    if (!negativeOnly.isEmpty()) {
      return ClassifierDecision.fixed(WizardMsPolarity.Negative);
    }
    // the first option, positive, is preselected
    return ClassifierDecision.choice(
        List.of(WizardMsPolarity.Positive, WizardMsPolarity.Negative), """
        The selected files contain positive and negative scans (polarity switching). Each wizard \
        run processes a single polarity. Select the ion mode to estimate and optimize the \
        parameters for.""");
  }

  private static @NotNull List<String> names(@NotNull Map<String, PolarityType> filePolarities,
      @NotNull Predicate<PolarityType> filter) {
    return filePolarities.entrySet().stream().filter(entry -> filter.test(entry.getValue()))
        .map(Entry::getKey).toList();
  }

  @Override
  public @NotNull UserParameter<WizardMsPolarity, ?> parameter() {
    return PreclassificationParameters.polarity;
  }

  @Override
  public @NotNull ClassifierDecision<WizardMsPolarity> decide(
      @NotNull List<@NotNull RawDataFile> files, @NotNull WizardSequence wizard) {
    final Map<String, PolarityType> filePolarities = new LinkedHashMap<>();
    for (final RawDataFile file : files) {
      filePolarities.put(file.getName(), PolarityType.fromPolarities(file.getDataPolarity()));
    }
    final WizardMsPolarity wizardPolarity = wizard.get(WizardPart.MS)
        .filter(ms -> ms.hasParameter(MassSpectrometerWizardParameters.polarity))
        .map(ms -> ms.getValue(MassSpectrometerWizardParameters.polarity))
        .orElse(WizardMsPolarity.No_filter);
    return decide(filePolarities, wizardPolarity);
  }
}
