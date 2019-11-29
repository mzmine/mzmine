/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data.types;

import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.modifiers.GraphicalColumType;
import io.github.mzmine.util.color.ColorsFX;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * This FeaturesType contains features for each RawDataFile. Sub columns for samples and charts are
 * created.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class RawColorType extends DataType<Color> implements GraphicalColumType {

  public RawColorType(Color color) {
    super(color);
  }

  @Override
  @Nonnull
  public String getHeaderString() {
    return "Color";
  }

  @Override
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, ? extends DataType> cell,
      TreeTableColumn<ModularFeatureListRow, ? extends DataType> coll, DataType<?> cellData,
      RawDataFile raw) {

    Pane pane = new Pane();
    pane.setStyle(
        "-fx-background-color: #" + ColorsFX.toHexString(value == null ? Color.BLACK : value));
    return pane;
  }

}
