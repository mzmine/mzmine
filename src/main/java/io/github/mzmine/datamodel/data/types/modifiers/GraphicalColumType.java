/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data.types.modifiers;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;

/**
 * This DataType creates a graphical cell content
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public interface GraphicalColumType<T> {
  /**
   * 
   * @param cell
   * @param coll
   * @param cellData same as cell.getItem
   * @param raw only provided for sample specific DataTypes
   * @return
   */
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, T> cell,
      TreeTableColumn<ModularFeatureListRow, T> coll, T cellData, RawDataFile raw);
}
