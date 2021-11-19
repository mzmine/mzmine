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

package io.github.mzmine.gui.chartbasics.simplechart;

import io.github.mzmine.gui.chartbasics.listener.RegionSelectionListener;

public interface AllowsRegionSelection {

  /**
   * Initializes a {@link RegionSelectionListener} and adds it to the plot. Following clicks will be
   * added to a region. Region selection can be finished by {@link SimpleXYZScatterPlot#finishPath()}.
   */
  public void startRegion();

  /**
   * The {@link RegionSelectionListener} of the current selection. The path/points can be retrieved
   * from the listener object.
   *
   * @return
   */
  public RegionSelectionListener finishPath();

}
