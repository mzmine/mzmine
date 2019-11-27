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

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.WrongTypeException;
import io.github.mzmine.datamodel.data.types.modifiers.GraphicalCellData;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.shape.Circle;

public class DetectionType extends DataType<FeatureStatus> implements GraphicalCellData {

  public DetectionType(FeatureStatus value) {
    super(value);
  }

  @Override
  public String getFormattedString() {
    return value.toString();
  }

  @Override
  public String getHeaderString() {
    return "Detection status";
  }

  @Override
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, DataType<?>> cell,
      TreeTableColumn<ModularFeatureListRow, ? extends DataType> coll) {
    if (!(cell.getItem() instanceof DetectionType))
      throw new WrongTypeException(this.getClass(), cell.getItem());

    DetectionType type = (DetectionType) cell.getItem();
    Circle circle = new Circle();
    circle.setRadius(10);
    circle.setFill(type.getValue().getColorFX());
    return circle;
  }

}
