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
          + GraphicalColumType.MAXIMUM_GRAPHICAL_CELL_WIDTH + " pixels.", false);

  public static final BooleanParameter hideImageAxes = new BooleanParameter("Hide image axes",
      "If ticked, the axes of image plots will be hidden.", false);

  public FeatureTableFXParameters() {
    super(new Parameter[]{showRowTypeColumns, showFeatureTypeColumns, lockImagesToAspectRatio,
        hideImageAxes});
  }

}
