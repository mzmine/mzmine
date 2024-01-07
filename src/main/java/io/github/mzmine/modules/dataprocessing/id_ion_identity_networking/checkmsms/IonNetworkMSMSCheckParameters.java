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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.checkmsms;


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;

/**
 * Refinement to MS annotation
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class IonNetworkMSMSCheckParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  // INCLUDED in sub
  // MZ-tolerance: deisotoping, adducts
  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter(
      "m/z tolerance (MS2)",
      "Tolerance value of the m/z difference between MS2 signals (and the precursor, if selected)");

  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height (in MS2)",
      "Minimum height of signal", MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public static final BooleanParameter CHECK_MULTIMERS = new BooleanParameter("Check for multimers",
      "Checks the truth of the multimer identification by searching the MS/MS spectra for the connection yM -> xM (x<y)",
      true);

  public static final OptionalParameter<ComboParameter<NeutralLossCheck>> CHECK_NEUTRALLOSSES =
      new OptionalParameter<ComboParameter<NeutralLossCheck>>(new ComboParameter<NeutralLossCheck>(
          "Check neutral losses (MS1->MS2)",
          "If M-H2O was detected in MS1 this modification is searched for the precursor m/z or any signal (+precursor)",
          NeutralLossCheck.values(), NeutralLossCheck.PRECURSOR), true);

  // Constructor
  public IonNetworkMSMSCheckParameters() {
    this(false);
  }

  public IonNetworkMSMSCheckParameters(boolean isSub) {
    super(isSub ? // no peak list and rt tolerance
        new Parameter[]{MZ_TOLERANCE, MIN_HEIGHT, CHECK_MULTIMERS, CHECK_NEUTRALLOSSES}
        : new Parameter[]{PEAK_LISTS, MZ_TOLERANCE, MIN_HEIGHT, CHECK_MULTIMERS,
            CHECK_NEUTRALLOSSES});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
