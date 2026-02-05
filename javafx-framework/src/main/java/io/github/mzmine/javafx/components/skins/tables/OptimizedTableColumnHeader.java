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

package io.github.mzmine.javafx.components.skins.tables;

import javafx.scene.control.TableColumnBase;
import javafx.scene.control.skin.TableColumnHeader;

public class OptimizedTableColumnHeader extends TableColumnHeader {

  private static final String SKIP_MEASUREMENT = "skipMeasurement";

  public OptimizedTableColumnHeader(TableColumnBase<?, ?> column) {
    super(column);
  }

  @Override
  protected void resizeColumnToFitContent(int maxRows) {
    return;
//    TableColumnBase<?, ?> col = getTableColumn();
//    if (col != null && Boolean.TRUE.equals(col.getProperties().get(SKIP_MEASUREMENT))) {
//      return; // Skip measurement
//    }
//    super.resizeColumnToFitContent(maxRows);
  }
}