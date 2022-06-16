/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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

  public static final BooleanParameter normalizeImagesTIC = new BooleanParameter(
      "Normalize images to TIC",
      "Specifies if images displayed in the feature table shall be normalized to the average TIC or shown according to the raw data.",
      true);

  public FeatureTableFXParameters() {
    super(new Parameter[]{showRowTypeColumns, showFeatureTypeColumns, lockImagesToAspectRatio,
        hideImageAxes, normalizeImagesTIC});
  }

}
