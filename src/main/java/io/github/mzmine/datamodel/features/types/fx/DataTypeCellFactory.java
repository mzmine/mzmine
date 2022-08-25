/*
 *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.features.types.fx;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.LinkedGraphicalType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberRangeType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default cell factory for a DataType
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class DataTypeCellFactory implements
    Callback<TreeTableColumn<ModularFeatureListRow, Object>, TreeTableCell<ModularFeatureListRow, Object>> {

  private static final Logger logger = Logger.getLogger(DataTypeCellFactory.class.getName());
  @NotNull
  private final DataType type;
  @Nullable
  private final RawDataFile raw;
  @Nullable
  private final SubColumnsFactory parent;
  private final int subcolumn;


  public DataTypeCellFactory(@Nullable RawDataFile raw, @NotNull DataType type,
      @Nullable SubColumnsFactory parent, int subcolumn) {
    this.parent = parent;
    this.type = type;
    this.raw = raw;
    this.subcolumn = subcolumn;
  }

  /**
   * Creates cells of the modular feature table. Sets text, graphics and tool tips.
   *
   * @param param the column
   * @return The cell.
   */
  @Override
  public TreeTableCell<ModularFeatureListRow, Object> call(
      TreeTableColumn<ModularFeatureListRow, Object> param) {
//    logger.log(Level.INFO, "Creating cell in DataTypeCellFactory");
    return new TreeTableCell<>() {

      @Override
      protected void updateItem(Object item, boolean empty) {
        try {
          super.updateItem(item, empty);
//        logger.log(Level.INFO, "updateItem in Cell (DataTypeCellFactory)");
          if(!getTableRow().isVisible()) {
            // fix to make the cell factory not go crazy and create invisible nodes
            return;
          }
          if (type instanceof LinkedGraphicalType lgType) {
            // convert Boolean to boolean
            boolean cellActive = item != null && ((Boolean) item).booleanValue();
            // first handle linked graphical types (like charts) that are dependent on other data
            Node node = ((GraphicalColumType) lgType).getCellNode(this, param, cellActive, raw);
            getTableColumn().setMinWidth(lgType.getColumnWidth());
            setGraphic(node);
            setText(null);
          } else {
            if (item == null) {
              item = type.getDefaultValue();
            }
            if (item == null || empty) {
              setGraphic(null);
              setText(null);
            } else {
              if (type instanceof GraphicalColumType graphicalColumType) {
                Node node = graphicalColumType.getCellNode(this, param, item, raw);
                getTableColumn().setMinWidth(graphicalColumType.getColumnWidth());
                setGraphic(node);
                setText(null);
              } else {
                // dirty fix for NumberRangeType as those types do not return sub types for each
                // column, but rather use NumberRangeType.this as type
                if (type instanceof NumberRangeType rangeType) {
                  setText(rangeType.getFormattedString((Number) item));
                } else {
                  setText(type.getFormattedString(item));
                }
                setGraphic(null);
              }
            }
            if (type instanceof NumberType) {
              setAlignment(Pos.CENTER_RIGHT);
            } else {
              setAlignment(Pos.CENTER);
            }
          }
        } catch (Exception ex) {
          logger.log(Level.WARNING, "Error in cell factory", ex);
        }
      }
    };
  }


}
