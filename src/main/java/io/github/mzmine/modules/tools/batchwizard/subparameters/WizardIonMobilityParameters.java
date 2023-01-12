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
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.ImsDefaults;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import java.text.DecimalFormat;

public class WizardIonMobilityParameters extends SimpleParameterSet {

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

  public static final BooleanParameter smoothing = new BooleanParameter("Smoothing",
      "Apply smoothing in the mobility dimension, usually only needed if the peak shapes are spiky.",
      true);
  /**
   * the UI element shown on top to signal the workflow used. Presets May be changed and then saved
   * to user presets as parameter files.
   */
  public static final HiddenParameter<ImsDefaults> wizardPart = new HiddenParameter<>(
      new ComboParameter<>("Wizard part", "Defines the wizard part used", ImsDefaults.values(),
          ImsDefaults.NO_IMS));

  /**
   * the part category of presets - is used in all wizard parameter classes
   */
  public static final WizardPartParameter wizardPartCategory = new WizardPartParameter(
      WizardPart.IMS);

  public WizardIonMobilityParameters() {
    super(new Parameter[]{
        // hidden
        wizardPart, wizardPartCategory,
        // shown
        imsActive, smoothing, instrumentType, minNumberOfDataPoints, approximateImsFWHM});
  }

  public WizardIonMobilityParameters(final int minDataPoints, final double fwhm,
      final boolean smooth, final boolean active, final MobilityType instrument) {
    this();
    setParameter(minNumberOfDataPoints, minDataPoints);
    setParameter(approximateImsFWHM, fwhm);
    setParameter(smoothing, smooth);
    setParameter(imsActive, active);
    setParameter(instrumentType, instrument);
  }


  /**
   * Create parameters from defaults
   *
   * @param defaults defines default values
   */
  public static WizardIonMobilityParameters create(final ImsDefaults defaults) {
    WizardIonMobilityParameters params = switch (defaults) {
      case NO_IMS -> new WizardIonMobilityParameters(5, 0.01, true, false, MobilityType.NONE);
      case TIMS -> new WizardIonMobilityParameters(5, 0.01, true, true, MobilityType.TIMS);
      case IMS -> new WizardIonMobilityParameters(5, 0.01, true, true, MobilityType.OTHER);
      case DTIMS -> new WizardIonMobilityParameters(4, 0.7, true, true, MobilityType.DRIFT_TUBE);
      case TWIMS ->
          new WizardIonMobilityParameters(4, 0.4, true, true, MobilityType.TRAVELING_WAVE);
    };
    params.setParameter(wizardPart, defaults);
    params.setParameter(wizardPartCategory, WizardPart.IMS);
    return params;
  }
}
