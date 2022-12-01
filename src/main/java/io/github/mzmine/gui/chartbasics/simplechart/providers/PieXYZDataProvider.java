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

package io.github.mzmine.gui.chartbasics.simplechart.providers;

import java.awt.Color;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

public interface PieXYZDataProvider<T> extends PlotXYZDataProvider {

  /**
   * Returns the slice identifiers. In order to support JFreechart's legend generation, the array
   * has to be in the same order, every time this method is called. Therefore, streaming the entries
   * from an undordered collection to an array should not be executed every time this method is
   * called, but during calculation via {@link #computeValues(SimpleObjectProperty)} and stored
   * thereafter.
   *
   * @return The slice identifiers.
   */
  T[] getSliceIdentifiers();

  /**
   * @param item The item index.
   * @return The summed value for the given index.
   */
  @Override
  double getZValue(int item);

  double getZValue(int series, int item);

  /**
   * @param series The index of the given slice.
   * @return The color.
   */
  @NotNull
  Color getSliceColor(int series);

  double getPieDiameter(int index);

  @Nullable
  @Override
  default Double getBoxHeight() {
    return null;
  }

  @Nullable
  @Override
  default Double getBoxWidth() {
    return null;
  }

   String getLabelForSeries(int series);

  @Nullable
  @Override
  default PaintScale getPaintScale() {
    return null;
  }
}
