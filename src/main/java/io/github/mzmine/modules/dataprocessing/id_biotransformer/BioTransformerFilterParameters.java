package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

public class BioTransformerFilterParameters extends SimpleParameterSet {

  public static BooleanParameter eductMustHaveMsMs = new BooleanParameter("Educt must have MS/MS",
      "Transformation products will only be predicted for educts with MS/MS.", false);

  public static OptionalParameter<DoubleParameter> minEductHeight = new OptionalParameter<>(
      new DoubleParameter("Minimum Educt intensity",
          "Products will only be predicted for educts above this intensity.",
          MZmineCore.getConfiguration().getIntensityFormat(), 1E4));

  public static BooleanParameter productMustHaveMsMs = new BooleanParameter(
      "Product must have MS/MS",
      "Transformation products will only be assigned to products with an MS/MS spectrum.", false);

  public static OptionalParameter<DoubleParameter> minProductHeight = new OptionalParameter<>(
      new DoubleParameter("Minimum Product intensity",
          "Products will only be assigned to products above this intensity.",
          MZmineCore.getConfiguration().getIntensityFormat(), 1E4));


  public BioTransformerFilterParameters() {
    super(new Parameter[] {eductMustHaveMsMs, minEductHeight, productMustHaveMsMs, minProductHeight});
  }
}
