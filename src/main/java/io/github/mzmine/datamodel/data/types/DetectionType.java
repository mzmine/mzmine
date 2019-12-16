/*
 * Copyright 2006-2020 The MZmine Development Team
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
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.modifiers.GraphicalColumType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.shape.Circle;

public class DetectionType extends DataType<ObjectProperty<FeatureStatus>>
    implements GraphicalColumType<FeatureStatus> {

  @Override
  @Nonnull
  public String getHeaderString() {
    return "State";
  }

  @Override
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, FeatureStatus> cell,
      TreeTableColumn<ModularFeatureListRow, FeatureStatus> coll, FeatureStatus cellData,
      RawDataFile raw) {
    if (cellData == null)
      return null;

    Circle circle = new Circle();
    circle.setRadius(10);
    circle.setFill(cellData.getColorFX());
    return circle;
  }

  @Override
  public ObjectProperty<FeatureStatus> createProperty() {
    return new SimpleObjectProperty<FeatureStatus>();
  }

}
