package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.datatype.DataTypeCheckListParameter;

public class FeatureTableFXParameters extends SimpleParameterSet {

  public static final DataTypeCheckListParameter showRowTypeColumns = new DataTypeCheckListParameter(
      "Row type columns",
      "Specify which data type columns shall be displayed in the feature list table");

  public static final DataTypeCheckListParameter showFeatureTypeColumns = new DataTypeCheckListParameter(
      "FeatureOld type columns",
      "Specify which data type columns shall be displayed in the feature list table");

  public FeatureTableFXParameters() {
    super(new Parameter[]{showRowTypeColumns, showFeatureTypeColumns});
  }

}
