/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPreset;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardWorkflowParameters.WorkflowDefaults;
import io.github.mzmine.parameters.Parameter;

public sealed class WizardWorkflowParameters extends
    AbstractWizardParameters<WorkflowDefaults> permits WizardWorkflowDdaParameters,
    WizardWorkflowGcElectronImpactParameters {

  public WizardWorkflowParameters(final WorkflowDefaults preset, final Parameter<?>... parameters) {
    super(WizardPart.WORKFLOW, preset, parameters);
  }

  @Override
  public WorkflowDefaults[] getPresetChoices() {
    return WorkflowDefaults.values();
  }

  /**
   * the defaults should not change the name of enum values. if strings are needed, override the
   * toString method
   */
  public enum WorkflowDefaults implements WizardParameterFactory {
    DDA, GC_EI_DECONVOLUTION, LIBRARY_GENERATION, MS1_ONLY;

    @Override
    public String toString() {
      return switch (this) {
        case DDA -> super.toString();
        case MS1_ONLY -> "MS1 only";
        case GC_EI_DECONVOLUTION -> "GC-EI deconvolution";
        case LIBRARY_GENERATION -> "Library generation";
      };
    }

    @Override
    public String getUniqueId() {
      return name();
    }

    @Override
    public WizardPreset create() {
      var params = switch (this) {
        // EMPTY parameter set
        case MS1_ONLY, LIBRARY_GENERATION -> new WizardWorkflowParameters(this);
        // specialized parameters
        case DDA -> new WizardWorkflowDdaParameters(true, null, true, true);
        case GC_EI_DECONVOLUTION -> new WizardWorkflowGcElectronImpactParameters(true, null, true);
      };
      return new WizardPreset(toString(), getUniqueId(), params);
    }

  }
}
