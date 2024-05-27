package io.github.mzmine.modules.visualization.kendrickmassplot.regionextraction;

import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotParameters;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.RegionsParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;

public class KendrickRegionExtractionParameters extends SimpleParameterSet {

  public static final ParameterSetParameter<KendrickMassPlotParameters> kendrickParam = new ParameterSetParameter<>(
      "Configure (kendrick) plot creation",
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

  public KendrickRegionExtractionParameters() {
    super(kendrickParam, xAxisCharge, yAxisCharge, xAxisDivisor, yAxisDivisor, regions, suffix);
  }
}
