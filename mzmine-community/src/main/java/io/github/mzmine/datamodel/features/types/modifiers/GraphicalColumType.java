/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.modifiers;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This DataType creates a graphical cell content
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public interface GraphicalColumType<T> {

  public static final int DEFAULT_GRAPHICAL_CELL_HEIGHT = 100;
  public static final int DEFAULT_IMAGE_CELL_HEIGHT = 150;
  public static final int DEFAULT_GRAPHICAL_CELL_WIDTH = 250;
  public static final int LARGE_GRAPHICAL_CELL_WIDTH = 300;
  public static final int MAXIMUM_GRAPHICAL_CELL_WIDTH = 800;


  /**
   * Generates a default graphical cell node. A placeholder text is added to this node. The actual
   * content, e.g. chart, will be created in the
   * {@link #createCellContent(ModularFeatureListRow, Object, RawDataFile, AtomicDouble)} method,
   * which is executed outside the JavaFX thread for a lag-free experience.
   *
   * @param cellData same as cell.getItem
   * @param raw      only provided for sample specific DataTypes
   * @return
   */
  public default Node getCellNode(TreeTableCell<ModularFeatureListRow, T> cell,
      TreeTableColumn<ModularFeatureListRow, T> coll, DataType type, T cellData, RawDataFile raw) {
    final ModularFeatureListRow row = cell.getTableRow().getItem();

    if (row.getFeatureList() != null) {
      return row.getFeatureList().getChartForRow(row, type, raw);
    }
    throw new IllegalStateException("No feature list associated with row.");
  }

  default double getCellHeight() {
    return DEFAULT_GRAPHICAL_CELL_HEIGHT;
  }

  /**
   * Create the actual graphical content for this data type. This method is executed outside the
   * JavaFX thread and is only meant to return the graphical content. All calculations should be
   * executed here for a lag-free feature table. The node is then added to the actual cell and also
   * buffered in the {@link io.github.mzmine.datamodel.features.ModularFeatureList#bufferedCharts}
   * field.
   *
   */
  @Nullable Node createCellContent(@NotNull ModularFeatureListRow row, T cellData,
      @Nullable RawDataFile raw, AtomicDouble progress);
}
