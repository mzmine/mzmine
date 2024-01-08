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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

public final class IonInterfaceImagingWizardParameters extends IonInterfaceWizardParameters {

  public static final IntegerParameter minNumberOfDataPoints = new IntegerParameter(
      "Minimum number of pixels",
      "Minimum number of pixels a m/z must be detected in. Consider the number of pixels in your imaging analysis.",
      25, 1, Integer.MAX_VALUE);

  public static final BooleanParameter enableDeisotoping = new BooleanParameter(
      "Enable deisotoping", "Enables feature list deisotoping during the workflow.\n"
      + "If enabled, this might falsely remove unique features from isobaric overlaps.", false);

  public IonInterfaceImagingWizardParameters(final IonInterfaceWizardParameterFactory preset) {
    super(WizardPart.ION_INTERFACE, preset,
        // actual parameters
        minNumberOfDataPoints, enableDeisotoping);
  }

  public IonInterfaceImagingWizardParameters(final IonInterfaceWizardParameterFactory preset,
      final int minDataPoints, final boolean enableDeisotoping) {
    this(preset);
    setParameter(minNumberOfDataPoints, minDataPoints);
    setParameter(this.enableDeisotoping, enableDeisotoping);
  }

}
