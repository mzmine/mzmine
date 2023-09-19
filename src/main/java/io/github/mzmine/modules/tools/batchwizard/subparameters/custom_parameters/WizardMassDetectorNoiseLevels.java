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

package io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters;

import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;

/**
 * Value for noise levels and mass detector in wizard
 */
public class WizardMassDetectorNoiseLevels extends CustomComboValue<MassDetectorWizardOptions> {

  /**
   * MS1
   */
  private final double ms1NoiseLevel;

  /**
   * MS2 and MSn
   */
  private final double msnNoiseLevel;

  public WizardMassDetectorNoiseLevels(final MassDetectorWizardOptions value, final double ms1,
      final double msn) {
    super(value);
    ms1NoiseLevel = ms1;
    msnNoiseLevel = msn;
  }

  /**
   * @return MS1 noise level
   */
  public double getMs1NoiseLevel() {
    return ms1NoiseLevel;
  }

  /**
   * @return MS2 and MSn noise level
   */
  public double getMsnNoiseLevel() {
    return msnNoiseLevel;
  }

  @Override
  public WizardMassDetectorNoiseLevels copy() {
    return new WizardMassDetectorNoiseLevels(valueType, ms1NoiseLevel, msnNoiseLevel);
  }
}
