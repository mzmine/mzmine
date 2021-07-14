package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.datatype.DataTypeCheckListParameter;

public class FeatureTableFXParameters extends SimpleParameterSet {

  public static final DataTypeCheckListParameter showRowTypeColumns = new DataTypeCheckListParameter(
      "Row type columns",
      "Specify which data type columns shall be displayed in the feature list table");

  public static final DataTypeCheckListParameter showFeatureTypeColumns = new DataTypeCheckListParameter(
      "Feature type columns",
      "Specify which data type columns shall be displayed in the feature list table");

  public static final BooleanParameter lockImagesToAspectRatio = new BooleanParameter(
      "Lock images to aspect ratio",
      "If enabled, the width of the column will be determined by the lateral width of "
          + "the image.\nNevertheless, this options is capped to a maximum width of "
          + GraphicalColumType.MAXIMUM_GRAPHICAL_CELL_WIDTH + " pixels.",
      false);

  public static final BooleanParameter hideImageAxes = new BooleanParameter(
      "Hide image axes",
      "If ticked, the axes of image plots will be hidden.",
      false);

  public FeatureTableFXParameters() {
    super(new Parameter[]{showRowTypeColumns, showFeatureTypeColumns, lockImagesToAspectRatio,
        hideImageAxes});
  }

}
