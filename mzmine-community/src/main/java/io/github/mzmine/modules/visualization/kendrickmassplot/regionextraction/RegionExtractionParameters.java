/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.kendrickmassplot.regionextraction;

import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotParameters;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.RegionsParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import org.jetbrains.annotations.NotNull;

public class RegionExtractionParameters extends SimpleParameterSet {

  public static final ParameterSetParameter<KendrickMassPlotParameters> kendrickParam = new ParameterSetParameter<>(
      "Configure plot creation (Kendrick)",
      "Configure the generation of the dataset (e.g., the x and y values for the region extraction",
      (KendrickMassPlotParameters) new KendrickMassPlotParameters().cloneParameterSet());

  public static final IntegerParameter xAxisCharge = new IntegerParameter("x axis charge",
      "Charge for calculation of the x axis value (if the value requires a charge for calculation)",
      1);
  public static final IntegerParameter yAxisCharge = new IntegerParameter("y axis charge",
      "Charge for calculation of the y axis value (if the value requires a charge for calculation)",
      1);

  public static final IntegerParameter xAxisDivisor = new IntegerParameter("x axis divisior",
      "Divisior for calculation of the x axis value (if the value requires a divisor for calculation).",
      1);
  public static final IntegerParameter yAxisDivisor = new IntegerParameter("y axis divisior",
      "Divisior for calculation of the y axis value (if the value requires a divisor for calculation).",
      1);

  public static final RegionsParameter regions = new RegionsParameter("Regions",
      "Define the regions for the region extraction.");

  public static final StringParameter suffix = new StringParameter("Feature list suffix",
      "The suffix for the filtered feature list.", "region");

  public RegionExtractionParameters() {
    super(kendrickParam, xAxisCharge, yAxisCharge, xAxisDivisor, yAxisDivisor, regions, suffix);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
