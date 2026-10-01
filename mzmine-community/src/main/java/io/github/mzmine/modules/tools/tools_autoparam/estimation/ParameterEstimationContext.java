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

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMassDetectorNoiseLevels;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.RawDataParameterEstimation;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Objects;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wizard context for interpreting measurements; construction performs no processing.
 */
public final class ParameterEstimationContext {

  private final @NotNull RawDataAnalysis analysis;
  private final @NotNull WizardSequence sequence;
  private final @Nullable MZTolerance sampleToSampleMzTolerance;
  private final @NotNull PresetSelection presetSelection;

  private ParameterEstimationContext(@NotNull RawDataAnalysis analysis,
      @NotNull WizardSequence sequence, @Nullable MZTolerance sampleToSampleMzTolerance,
      @NotNull PresetSelection presetSelection) {
    this.analysis = analysis;
    this.sequence = sequence;
    this.sampleToSampleMzTolerance = sampleToSampleMzTolerance;
    this.presetSelection = presetSelection;
  }

  /**
   * Estimates for the presets selected in the sequence.
   */
  public ParameterEstimationContext(@NotNull RawDataAnalysis analysis,
      @NotNull WizardSequence sequence) {
    this(analysis, sequence, estimateSampleToSampleMzTolerance(analysis), PresetSelection.NONE);
  }

  /**
   * Offers the ion interface and mass spectrometer presets that fit the raw data, see
   * {@link PresetSelection}, and estimates for them if confirmed. The context sequence is then a
   * copy with the default parameters of the new presets.
   *
   * @param sequence     the wizard sequence, is not modified
   * @param confirmation called with the fitting presets if they differ from the sequence, returns
   *                     true to estimate for them. Usually asks the user and switches the wizard.
   */
  public static @NotNull ParameterEstimationContext withFittingPresets(
      @NotNull RawDataAnalysis analysis, @NotNull WizardSequence sequence,
      @NotNull Predicate<@NotNull PresetSelection> confirmation) {
    final PresetSelection fitting = PresetSelection.select(analysis, sequence);
    if (fitting.isEmpty() || !confirmation.test(fitting)) {
      return new ParameterEstimationContext(analysis, sequence);
    }
    final WizardSequence estimationSequence = sequence.copy();
    fitting.applyDefaultPresets(estimationSequence);
    return new ParameterEstimationContext(analysis, estimationSequence,
        estimateSampleToSampleMzTolerance(analysis), fitting);
  }

  private static @Nullable MZTolerance estimateSampleToSampleMzTolerance(
      @NotNull RawDataAnalysis analysis) {
    return ParameterEstimators.estimateSampleToSampleMzTolerance(analysis.sampleMzToleranceCounts(),
        0.8f);
  }

  /**
   * @return the presets that were switched to in {@link #sequence()} to fit the raw data
   */
  public @NotNull PresetSelection presetSelection() {
    return presetSelection;
  }

  public @NotNull MassDetectorWizardOptions massDetectorType() {
    return sequence.get(WizardPart.MS)
        .map(step -> step.getValue(MassSpectrometerWizardParameters.massDetectorOption))
        .map(WizardMassDetectorNoiseLevels::getValueType)
        .orElseGet(() -> RawDataParameterEstimation.inferMassDetectorType(analysis.files()));
  }

  public boolean lowResolution() {
    return sequence.get(WizardPart.MS).map(WizardStepParameters::getFactory)
        .map(MassSpectrometerWizardParameterFactory.LOW_RES::equals).orElse(false);
  }

  public <T> @NotNull T preset(@NotNull WizardPart part, @NotNull UserParameter<T, ?> parameter) {
    return sequence.get(part).map(WizardStepParameters::createDefaultParameterPreset)
        .map(step -> step.getValue(parameter)).orElseThrow(() -> new IllegalArgumentException(
            "No preset default for " + part + ": " + parameter.getName()));
  }

  public @NotNull RawDataAnalysis analysis() {
    return analysis;
  }

  public @NotNull WizardSequence sequence() {
    return sequence;
  }

  public @Nullable MZTolerance sampleMzTolerance() {
    return sampleToSampleMzTolerance;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (ParameterEstimationContext) obj;
    return Objects.equals(this.analysis, that.analysis) && Objects.equals(this.sequence,
        that.sequence) && Objects.equals(this.sampleToSampleMzTolerance,
        that.sampleToSampleMzTolerance)
        && Objects.equals(this.presetSelection, that.presetSelection);
  }

  @Override
  public int hashCode() {
    return Objects.hash(analysis, sequence, sampleToSampleMzTolerance, presetSelection);
  }

  @Override
  public String toString() {
    return "ParameterEstimationContext[" + "analysis=" + analysis + ", " + "sequence=" + sequence
        + ", " + "sampleToSampleMzTolerance=" + sampleToSampleMzTolerance + ", "
        + "presetSelection="
        + presetSelection + ']';
  }

}
