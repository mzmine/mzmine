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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer;

import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.OptimizationParameterRegistry;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterDefinition;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.CheckListParameter;
import io.github.mzmine.util.ExitCode;
import java.util.ArrayList;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple optimizer setup that only selects the parameters to optimize. All other settings use the
 * defaults of {@link OptimizerParameters}, see
 * {@link OptimizerParameters#create(SimpleOptimizerParameters)}.
 */
public class SimpleOptimizerParameters extends SimpleParameterSet {

  public static final CheckListParameter<ParameterDefinition<?>> paramToOptimize = new CheckListParameter<>(
      "Parameters to optimize", "Select which parameters should be optimized.",
      OptimizationParameterRegistry.allSolutions(),
      new ArrayList<>(OptimizationParameterRegistry.defaultSolutions()));

  public SimpleOptimizerParameters() {
    super(paramToOptimize);
  }

  @Override
  public @Nullable Region getMessage() {
    return FxLayout.newAccordion(true, OptimizerParameters.createOverrideMessage());
  }

  /**
   * @param sequence the wizard sequence, limits the parameter checklist to the parameters that
   *                 apply to its presets. null shows all parameters.
   */
  public @NotNull ExitCode showSetupDialog(boolean valueCheckRequired,
      @Nullable WizardSequence sequence) {
    return OptimizerParameters.showSetupDialogForSequence(this, paramToOptimize, valueCheckRequired,
        sequence);
  }
}
