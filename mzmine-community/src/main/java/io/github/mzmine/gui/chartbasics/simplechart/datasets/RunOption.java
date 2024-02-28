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

package io.github.mzmine.gui.chartbasics.simplechart.datasets;

import io.github.mzmine.gui.chartbasics.simplechart.providers.XYValueProvider;

public enum RunOption {
  /**
   * Directly executes this dataset's calculations on the current thread. This may not be the FX
   * application thread.
   *
   * @see ColoredXYDataset#run()
   * @see io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider#computeValues
   */
  THIS_THREAD,
  /**
   * Directly executes this dataset calculations on a new thread.
   *
   * @see ColoredXYDataset#run()
   * @see XYValueProvider#computeValues(javafx.beans.property.Property)
   */
  NEW_THREAD,
  /**
   * Does not run the dataset calculations. To be started individually by the programmer.
   *
   * @see ColoredXYDataset#run()
   * @see io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider#computeValues
   */
  DO_NOT_RUN
}
