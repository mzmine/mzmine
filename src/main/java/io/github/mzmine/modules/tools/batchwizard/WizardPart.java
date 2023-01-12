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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.tools.batchwizard.WizardPreset.ChromatographyDefaults;
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.DefaultOptions;
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.ImsDefaults;
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.MsInstrumentDefaults;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;

/**
 * Describes the sequence of steps in the wizard. Elements should stay in correct order.
 */
public enum WizardPart {
  DATA_IMPORT, CHROMATOGRAPHY, IMS, MS, FILTER, EXPORT;

  public Enum<?>[] getDefaultsEnum() {
    return switch (this) {
      // only one option
      case DATA_IMPORT, FILTER, EXPORT -> DefaultOptions.values();
      // multiple options
      case CHROMATOGRAPHY -> ChromatographyDefaults.values();
      case IMS -> ImsDefaults.values();
      case MS -> MsInstrumentDefaults.values();
    };
  }

  /**
   * @return the corresponding ParameterSetParameter to this preset
   */
  public ParameterSetParameter getParameterSetParameter() {
    return switch (this) {
      case DATA_IMPORT -> BatchWizardParameters.dataInputParams;
      case CHROMATOGRAPHY -> BatchWizardParameters.hplcParams;
      case IMS -> BatchWizardParameters.imsParameters;
      case MS -> BatchWizardParameters.msParams;
      case FILTER -> BatchWizardParameters.filterParameters;
      case EXPORT -> BatchWizardParameters.exportParameters;
    };
  }
}
