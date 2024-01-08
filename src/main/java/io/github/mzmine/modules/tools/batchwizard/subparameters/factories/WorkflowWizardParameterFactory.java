/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.subparameters.factories;

import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDiaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowGcElectronImpactWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowLibraryGenerationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowWizardParameters;

/**
 * the defaults should not change the name of enum values. if strings are needed, override the
 * toString method
 */
public enum WorkflowWizardParameterFactory implements WizardParameterFactory {
  /**
   * Options for GNPS, molecular networking, SIRIUS,
   */
  DDA,
  DIA,
  /**
   * Currently only used in GC-EI; maybe in the future for all ion fragmentation (DIA)
   */
  DECONVOLUTION,
  /**
   * uses annotations to build spectral libraries
   */
  LIBRARY_GENERATION,
  /**
   * Nothing special just avoids all MS2 specific steps
   */
  MS1_ONLY,
  /**
   * imaging analysis
   */
  IMAGING;

  @Override
  public String toString() {
    return switch (this) {
      case DDA, DIA -> super.toString();
      case MS1_ONLY -> "MS1 only";
      case DECONVOLUTION -> "Spectral deconvolution";
      case LIBRARY_GENERATION -> "Library generation";
      case IMAGING -> "Imaging";
    };
  }

  @Override
  public String getUniqueId() {
    return name();
  }

  @Override
  public WizardStepParameters create() {
    return switch (this) {
      // EMPTY parameter set
      case MS1_ONLY, IMAGING -> new WorkflowWizardParameters(this);
      // specialized parameters
      case LIBRARY_GENERATION ->
          new WorkflowLibraryGenerationWizardParameters(null, true, true, false);
      case DDA -> new WorkflowDdaWizardParameters(true, true, null, true, true, false);
      case DECONVOLUTION ->
          new WorkflowGcElectronImpactWizardParameters(true, null, true, true, false);
      case DIA -> new WorkflowDiaWizardParameters(0.8, 5, true, null, true, true, false);
    };
  }
}
