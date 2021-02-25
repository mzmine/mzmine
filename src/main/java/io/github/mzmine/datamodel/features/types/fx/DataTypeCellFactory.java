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

package io.github.mzmine.datamodel.features.types.fx;

import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;

/**
 * Default cell factory for a DataType
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class DataTypeCellFactory implements
    Callback<TreeTableColumn<ModularFeatureListRow, Object>, TreeTableCell<ModularFeatureListRow, Object>> {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private RawDataFile raw;
  private DataType<?> type;
  private int subcolumn = -1;


  public DataTypeCellFactory(RawDataFile raw, DataType<?> type) {
    this(raw, type, -1);
  }

  public DataTypeCellFactory(RawDataFile raw, DataType<?> type, int subcolumn) {
    this.type = type;
    this.raw = raw;
    this.subcolumn = subcolumn;
  }

  /**
   * Creates cells of the modular feature table. Sets text, graphics and tool tips.
   *
   * @param param
   * @return The cell.
   */
  @Override
  public TreeTableCell<ModularFeatureListRow, Object> call(
      TreeTableColumn<ModularFeatureListRow, Object> param) {
//    logger.log(Level.INFO, "Creating cell in DataTypeCellFactory");
    return new TreeTableCell<>() {

      @Override
      protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);
//        logger.log(Level.INFO, "updateItem in Cell (DataTypeCellFactory)");
        if (item == null || empty) {
          setGraphic(null);
          setText(null);
        } else {
          // sub columns provide values
          if (type instanceof SubColumnsFactory) {
            // get sub column value
            SubColumnsFactory sub = (SubColumnsFactory) type;
            Node n = sub.getSubColNode(subcolumn, this, param, item, raw);
            setGraphic(n);
            setText(
                n != null ? null
                    : sub.getFormattedSubColValue(subcolumn, this, param, item, raw));
            setTooltip(
                new Tooltip(sub.getFormattedSubColValue(subcolumn, this, param, item, raw)));
          } else if (type instanceof GraphicalColumType) {
            Node node = ((GraphicalColumType) type).getCellNode(this, param, item, raw);
            getTableColumn().setMinWidth(((GraphicalColumType<?>) type).getColumnWidth());
            setGraphic(node);
            setText(null);
            setTooltip(new Tooltip(type.getFormattedString(item)));
          } else {
            setTooltip(new Tooltip(type.getFormattedString(item)));
            setText(type.getFormattedString(item));
            setGraphic(null);
          }
        }
        if(type instanceof NumberType)
          setAlignment(Pos.CENTER_RIGHT);
        else
          setAlignment(Pos.CENTER);
      }
    };
  }


}
