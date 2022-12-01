/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;

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
   *
   * @param cell
   * @param coll
   * @param cellData same as cell.getItem
   * @param raw only provided for sample specific DataTypes
   * @return
   */
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, T> cell,
      TreeTableColumn<ModularFeatureListRow, T> coll, T cellData, RawDataFile raw);

  /**
   * Returns width of the column.
   *
   * @return width of the column
   */
  public double getColumnWidth();
}
