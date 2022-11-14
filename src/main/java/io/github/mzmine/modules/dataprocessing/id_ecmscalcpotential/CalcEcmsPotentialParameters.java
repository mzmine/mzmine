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

package io.github.mzmine.modules.dataprocessing.id_ecmscalcpotential;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.text.DecimalFormat;

public class CalcEcmsPotentialParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final DoubleParameter tubingLengthMM = new DoubleParameter("Tubing length / mm",
      "Tubing length between EC-Cell and ESI-Needle.", new DecimalFormat("0.0"), 750d);

  public static final DoubleParameter tubingIdMM = new DoubleParameter("Tubing inner diameter / mm",
      "Inner diameter of the tubing.", new DecimalFormat("0.000"), 0.127d);

  public static final DoubleParameter flowRateMicroLiterPerMin = new DoubleParameter(
      "Flow rate / μL/min", "Tubing length between EC-Cell and ESI-Needle.", new DecimalFormat("0.0"));

  public static final DoubleParameter potentialRampSpeed = new DoubleParameter(
      "Potential ramp / mV/s", "Potential ramp speed in mV/s.");

  public static final PercentParameter potentialAssignmentIntensityPercentage = new PercentParameter(
      "Potential assingment intensity",
      "Percentage of the maximum metabolite intensity that will be used to assign the formation potential to a metabolite.");

  public CalcEcmsPotentialParameters() {
    super(new Parameter[]{flists, tubingLengthMM, tubingIdMM, flowRateMicroLiterPerMin, potentialRampSpeed,
        potentialAssignmentIntensityPercentage});
  }
}
