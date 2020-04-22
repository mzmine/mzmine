package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.datatype.DataTypeCheckListParameter;

public class FeatureTableFXParameters extends SimpleParameterSet {

  public static final BooleanParameter showXIC = new BooleanParameter("Show XIC",
      "Shows the XIC of the selected feature.", false);
  public static final BooleanParameter showSpectrum = new BooleanParameter("Show spectrum",
      "Shows the spectrum of the selected feature.", false);

  public static final DataTypeCheckListParameter showDataTypeColumns = new DataTypeCheckListParameter(
      "Show data type columns",
      "Specify which data type columns shall be displayed in the feature list table");

  public FeatureTableFXParameters() {
    super(new Parameter[]{showXIC, showSpectrum, showDataTypeColumns});
  }

}
