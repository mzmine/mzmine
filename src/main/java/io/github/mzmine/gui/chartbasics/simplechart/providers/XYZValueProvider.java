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
