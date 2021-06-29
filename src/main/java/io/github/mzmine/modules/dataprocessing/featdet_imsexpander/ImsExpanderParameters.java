package io.github.mzmine.modules.dataprocessing.featdet_imsexpander;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class ImsExpanderParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final OptionalParameter<MZToleranceParameter> mzTolerance = new OptionalParameter<>(
      new MZToleranceParameter("m/z tolerance",
          "m/z tolerance for peaks in the mobility dimension. If enabled, the given "
              + "tolerance will be applied to the feature m/z. If disabled, the m/z range of the "
              + "feature's data points will be used as a tolerance range."));

  public static final IntegerParameter mobilogramBinWidth = new IntegerParameter(
      "Mobility bin witdh (scans)",
      "The mobility binning width in scans. (default = 1, high mobility resolutions "
          + "in TIMS might require a higher bin width to achieve a constant ion current for a "
          + "mobilogram.", 1, true);

  public ImsExpanderParameters() {
    super(new Parameter[]{featureLists, mzTolerance, mobilogramBinWidth});
  }
}
