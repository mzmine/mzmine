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

/**
 * This interface is used to generate tool-tips based on the index of a value. A tooltip is only
 * shown when the mouse is hovered over a datapoint within the chart, so it can be longer and more
 * specific than a label.
 *
 * @author https://github.com/SteffenHeu
 * @see ExampleXYProvider
 */
public interface ToolTipTextProvider {

  /**
   * @param itemIndex The index of the value to provide a tool tip for.
   * @return A tool tip for the value at the given index. E.g., in a TIC plot (Intensity vs. time)
   * you might want to provide the m/z value, the exact retention time, the intensity of the highest
   * peak and maybe even the scan number as a tooltip.
   */
  @Nullable
  public String getToolTipText(int itemIndex);
}
