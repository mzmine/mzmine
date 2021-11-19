/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
