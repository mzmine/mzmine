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
import io.github.mzmine.modules.tools.batchwizard.subparameters.CustomizationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public final class ParameterEstimationTestData {

  public static final WizardParameterDefinition<Double> MINIMUM_FEATURE_HEIGHT = OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT;
  public static final WizardParameterDefinition<Integer> MINIMUM_CONSECUTIVE_SCANS = OptimizationParameterRegistry.MINIMUM_CONSECUTIVE_SCANS;
  public static final WizardParameterDefinition<RTTolerance> INTER_SAMPLE_RT = OptimizationParameterRegistry.INTER_SAMPLE_RT;
  public static final WizardParameterDefinition<MZTolerance> MZ_TOLERANCE = OptimizationParameterRegistry.MZ_TOLERANCE;

  private ParameterEstimationTestData() {
  }

  public static @NotNull ParameterEstimationContext context(@NotNull WizardSequence sequence) {
    return context(sequence, new PreclassificationParameters().cloneParameterSet());
  }

  /**
   * @param preclassification the settings fixed by the pre-classification
   */
  public static @NotNull ParameterEstimationContext context(@NotNull WizardSequence sequence,
      @NotNull ParameterSet preclassification) {
    return new ParameterEstimationContext(
        new RawDataAnalysis(List.of(), new double[]{0.01, 0.04, 0.08, 0.1, 0.2},
            new double[]{6, 10, 12, 16, 20}, new double[]{10, 100, 500, 1000, 10000},
            new double[]{800, 1000, 12000, 25000, 250000}, new double[]{0.01, 0.02, 0.03, 0.05},
            new double[0], java.util.Map.of()), sequence, preclassification);
  }

  public static @NotNull WizardSequence sequence() {
    final WizardSequence sequence = new WizardSequence();
    sequence.set(WizardPart.ION_INTERFACE, IonInterfaceWizardParameterFactory.HPLC.create());
    sequence.set(WizardPart.MS, MassSpectrometerWizardParameterFactory.QTOF.create());
    sequence.set(WizardPart.IMS, IonMobilityWizardParameterFactory.TIMS.create());
    sequence.set(WizardPart.CUSTOMIZATION, CustomizationWizardParameters.createDefault());
    return sequence;
  }
}
