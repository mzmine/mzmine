/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;

public class CountingRowCellFactory<T, V> implements
    Callback<TreeTableColumn<T, V>, TreeTableCell<T, V>> {

  // uses a counter to label the first cell that seems to be the measurement cell
  public final AtomicInteger counter = new AtomicInteger(0);
  private final Function<Integer, TreeTableCell<T, V>> createCell;

  public CountingRowCellFactory(
      Function<Integer, TreeTableCell<T, V>> createCell) {
    this.createCell = createCell;
  }

  @Override
  public TreeTableCell<T, V> call(
      TreeTableColumn<T, V> column) {
    return createCell.apply(counter.getAndIncrement());
  }
}
