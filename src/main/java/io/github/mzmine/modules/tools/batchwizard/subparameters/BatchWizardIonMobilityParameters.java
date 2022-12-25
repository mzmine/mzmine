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
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.ImsDefaults;
import io.github.mzmine.parameters.HiddenParameter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import java.text.DecimalFormat;

public class BatchWizardIonMobilityParameters extends SimpleParameterSet {

  public static final DoubleParameter approximateImsFWHM = new DoubleParameter(
      "Approximate feature FWHM",
      "The approximate feature width (IMS peak width) in ion mobility (full-width-at-half-maximum, FWHM).",
      new DecimalFormat("0.0000"), 0.04d);

  public static final IntegerParameter minNumberOfDataPoints = new IntegerParameter(
      "Min # of data points",
      "Minimum number of data points as used in ion mobility feature resolving.", 5, 1,
      Integer.MAX_VALUE);

  public static final HiddenParameter<Boolean> imsActive = new HiddenParameter<>(
      new BooleanParameter("IMS active", "Flag if IMS is active", false));

  public static final ComboParameter<MobilityType> instrumentType = new ComboParameter<>("IMS type",
      "", MobilityType.values(), MobilityType.TIMS);


  public BatchWizardIonMobilityParameters() {
    super(new Parameter[]{imsActive, instrumentType, minNumberOfDataPoints, approximateImsFWHM});
  }

  /**
   * Create parameters from defaults
   *
   * @param defaults defines default values
   */
  public BatchWizardIonMobilityParameters(final ImsDefaults defaults) {
    this();
    setParameter(minNumberOfDataPoints, 5);
    setParameter(approximateImsFWHM, 0.04);
    // override defaults
    switch (defaults) {
      case NO_IMS -> {
        setParameter(imsActive, false);
        setParameter(instrumentType, MobilityType.NONE);
      }
      case tims -> {
        setParameter(imsActive, true);
        setParameter(instrumentType, MobilityType.TIMS);
      }
      case IMS -> {
        setParameter(imsActive, true);
        setParameter(instrumentType, MobilityType.OTHER);
      }
    }
  }
}
