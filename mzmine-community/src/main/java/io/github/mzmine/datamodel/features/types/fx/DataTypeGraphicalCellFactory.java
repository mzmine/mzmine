/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.datamodel.features.types.fx;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class DataTypeGraphicalCellFactory<S, T extends DataType<S> & GraphicalColumType<S>> implements
    Callback<TreeTableColumn<ModularFeatureListRow, Object>, TreeTableCell<ModularFeatureListRow, Object>> {

  private static final Logger logger = Logger.getLogger(DataTypeGraphicalCellFactory.class.getName());
  @NotNull
  private final DataType type;
  @Nullable
  private final RawDataFile raw;
  @Nullable
  private final SubColumnsFactory parent;
  private final int subcolumn;


  public DataTypeGraphicalCellFactory(@Nullable RawDataFile raw, @NotNull DataType type,
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
          // needs to check for row visibility
          // this makes scrambles the column order - rows seem to be flagged invisible wrongly
          if (empty || item == null) {
            setGraphic(null);
            return;
          }

          if (type instanceof GraphicalColumType graphicalColumType) {
            Node node = graphicalColumType.getCellNode(this, param, type, item, raw);
            getTableColumn().setPrefWidth(graphicalColumType.getColumnWidth());
            setGraphic(node);
          }

        } catch (Exception ex) {
          logger.log(Level.WARNING, "Error in cell factory", ex);
        }
      }
    };
  }


}
