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

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import javafx.scene.control.TreeTableCell;

/**
 * This cell skips measurement cell. Becuase measurements are applied to many values of the column.
 * But if the column has a defined prefered width this is not needed and may be slow for expensive
 * cells.
 * <p>
 * In our experience id==0 is measurement cell
 *
 * @param <S>
 * @param <V>
 */
public abstract class SkipMeasurementTreeCell<S, V> extends TreeTableCell<S, V> {

  protected final int id;

  public SkipMeasurementTreeCell(int id) {
    this.id = id;
  }

  /**
   * Measurement cell is always generated first. Also later the cell has index -1
   */
  public boolean isMeasurementCell() {
    return id == 0;
  }

  @Override
  protected void updateItem(V value, boolean visible) {
    super.updateItem(value, visible);
    if (isMeasurementCell()) {
      // measurement cell is called many times
      // this is not needed for expensive columns with fixed width
      return;
    }
    updateContent(value, visible);
  }

  /**
   * update the content, called by update item. skipped for measurement cell
   */
  protected abstract void updateContent(V value, boolean empty);
}
