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

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.List;
import javafx.beans.property.Property;

/**
 * This interface is used to provide a dataset with x and y values. The amount of x and y values has
 * to be equal and is checked via the {@link List#size()} method.
 * <p></p>
 * The values are not grabbed during the creation of the dataset. After initialising the dataset
 * (e.g. {@link ColoredXYDataset}) a thread is started where the values of the dataset can be
 * calculated or loaded from disk. For that operation, the
 * {@link XYValueProvider#computeValues(Property)} method is used. The implementing class can supply
 * information on the progress of the operation via the method
 * {@link XYValueProvider#getComputationFinishedPercentage()}, which will be represented in the task
 * bar.
 * <p></p>
 * When the computation ({@link XYValueProvider#computeValues} has finished, the values are loaded
 * into the dataset via the {@link XYValueProvider#getDomainValue(int)} and
 * {@link XYValueProvider#getRangeValue(int)} methods.
 * <p></p>
 * After the dataset has been loaded successfully, the chart is automatically updated via a
 * {@link org.jfree.chart.JFreeChart#fireChartChanged()} event.
 *
 * @author https://github.com/SteffenHeu
 */
public interface XYValueProvider {

  /**
   * Called in a seperate thread to compute values or load them from disk after the dataset has been
   * created.
   *
   * @param status The task status of the task executing this calculation. Long calculations should
   *               repeatedly check this value and cancel their computation if the status has
   *               changed to {@link TaskStatus#CANCELED}. Implementing classes can also make use of
   *               CANCELED or ERROR to stop the task from continuing, if an error occurred.
   */
  void computeValues(Property<TaskStatus> status);

  /**
   * @return A sorted list of domain values. Index has to match the range value indices.
   */
  double getDomainValue(int index);

  /**
   * @return A sorted (ascending) list of range values. Index has to match the domain value indices.
   */
  double getRangeValue(int index);

  /**
   * Called after {@link XYValueProvider#computeValues}.
   *
   * @return The number of values in this data set.
   */
  int getValueCount();

  /**
   * Helper method to provide the user with progress information during
   * {@link XYValueProvider#computeValues(Property)}.
   *
   * @return a finished percentage. (0.0-1.0)
   */
  double getComputationFinishedPercentage();
}
