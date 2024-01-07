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

import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import java.text.DecimalFormat;

public final class IonMobilityWizardParameters extends WizardStepParameters {

  public static final DoubleParameter approximateImsFWHM = new DoubleParameter(
      "Approximate feature FWHM",
      "The approximate feature width (IMS peak width) in ion mobility (full-width-at-half-maximum, FWHM).",
      new DecimalFormat("0.0000"), 0.04d);

  public static final IntegerParameter minNumberOfDataPoints = new IntegerParameter(
      "Minimum consecutive scans",
      "Minimum number of consecutive scans with detected data points as used in ion mobility feature resolving.", 5, 1,
      Integer.MAX_VALUE);

  public static final HiddenParameter<Boolean> imsActive = new HiddenParameter<>(
      new BooleanParameter("IMS active", "Flag if IMS is active", false));

  public static final ComboParameter<MobilityType> instrumentType = new ComboParameter<>("IMS type",
      "", MobilityType.values(), MobilityType.TIMS);

  public static final BooleanParameter smoothing = new BooleanParameter("Smoothing",
      "Apply smoothing in the mobility dimension, usually only needed if the peak shapes are spiky.",
      true);

  public IonMobilityWizardParameters(IonMobilityWizardParameterFactory preset) {
    super(WizardPart.IMS, preset,
        // parameters
        imsActive, smoothing, instrumentType, minNumberOfDataPoints, approximateImsFWHM);
  }

  public IonMobilityWizardParameters(final IonMobilityWizardParameterFactory preset,
      final int minDataPoints, final double fwhm, final boolean smooth, final boolean active,
      final MobilityType instrument) {
    this(preset);
    setParameter(minNumberOfDataPoints, minDataPoints);
    setParameter(approximateImsFWHM, fwhm);
    setParameter(smoothing, smooth);
    setParameter(imsActive, active);
    setParameter(instrumentType, instrument);
  }


}
