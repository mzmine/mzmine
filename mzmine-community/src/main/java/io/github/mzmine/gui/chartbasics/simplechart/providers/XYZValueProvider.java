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

import org.jetbrains.annotations.Nullable;

public interface XYZValueProvider extends XYValueProvider {

  public double getZValue(int index);

  /**
   * Supplies the dataset with box width information. Is called after {@link
   * PlotXYZDataProvider#computeValues} when values have been loaded, so member variables of the
   * provider can be used. Can be calculated when this method is called or during {@link
   * PlotXYZDataProvider#computeValues}
   *
   * @return The box width or null if a default shall be calculated based on median y values.
   */
  @Nullable
  public Double getBoxHeight();

  /**
   * Supplies the dataset with box width information. Is called after {@link
   * PlotXYZDataProvider#computeValues} when values have been loaded, so member variables of the
   * provider can be used. Can be calculated when this method is called or during {@link
   * PlotXYZDataProvider#computeValues}
   *
   * @return The box height or null if a default shall be calculated based on median x values.
   */
  @Nullable
  public Double getBoxWidth();
}
