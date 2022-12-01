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
