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

package io.github.mzmine.datamodel.features.types;

import org.jetbrains.annotations.NotNull;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.util.color.ColorsFX;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
public class RawColorType extends DataType<ObjectProperty<Color>>
    implements GraphicalColumType<ObjectProperty<Color>> {

  @Override
  @NotNull
  public String getHeaderString() {
    return "Color";
  }

  @Override
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, ObjectProperty<Color>> cell,
      TreeTableColumn<ModularFeatureListRow, ObjectProperty<Color>> coll,
      ObjectProperty<Color> value, RawDataFile raw) {

    Pane pane = new Pane();
    pane.setStyle("-fx-background-color: "
        + ColorsFX.toHexString(value.getValue() == null ? Color.BLACK : value.getValue()));
    return pane;
  }

  @Override
  public ObjectProperty<Color> createProperty() {
    return new SimpleObjectProperty<Color>();
  }

  @Override
  public double getColumnWidth() {
    return 25;
  }

}
