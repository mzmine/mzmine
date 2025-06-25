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

package io.github.mzmine.gui.chartbasics.simplechart;

import io.github.mzmine.gui.chartbasics.gui.wrapper.MouseEventWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.XYDataset;

/**
 * Contains information about the currently selected point. The point is determined by the
 * {@link org.jfree.chart.plot.XYPlot#getDomainCrosshairValue()} and
 * {@link org.jfree.chart.plot.XYPlot#getRangeCrosshairValue()} methods. A listener can be added via
 * the {@link SimpleXYChart#cursorPositionProperty()}.
 *
 * @author https://github.com/SteffenHeu
 */
public class PlotCursorPosition {

  public static final double DEFAULT_Z_VALUE = -1.0d;

  final double rangeValue;
  final double domainValue;
  final double zValue;
  final XYDataset dataset;
  final int valueIndex;
  private MouseEventWrapper mouseEvent;

  public PlotCursorPosition(final double domainVal, final double rangeVal, final double zValue,
      final int valueIndex, final XYDataset dataset) {
    this.rangeValue = rangeVal;
    this.domainValue = domainVal;
    this.valueIndex = valueIndex;
    this.zValue = zValue;
    this.dataset = dataset;
  }

  public PlotCursorPosition(final double domainVal, final double rangeVal, final int valueIndex,
      final XYDataset dataset) {
    this(domainVal, rangeVal, DEFAULT_Z_VALUE, valueIndex, dataset);
  }

  public double getRangeValue() {
    return rangeValue;
  }

  public double getDomainValue() {
    return domainValue;
  }

  /**
   * @return The z value or {@link PlotCursorPosition#DEFAULT_Z_VALUE} if no z value was set.
   */
  public double getzValue() {
    return zValue;
  }

  /**
   * @return -1 if values were not in any {@link ColoredXYDataset}
   */
  public int getValueIndex() {
    return valueIndex;
  }

  /**
   * @return The dataset or null
   */
  @Nullable
  public XYDataset getDataset() {
    return dataset;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PlotCursorPosition)) {
      return false;
    }
    PlotCursorPosition that = (PlotCursorPosition) o;
    return Double.compare(that.getRangeValue(), getRangeValue()) == 0
        && Double.compare(that.getDomainValue(), getDomainValue()) == 0
        && getValueIndex() == that.getValueIndex() && Objects.equals(getDataset(),
        that.getDataset());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRangeValue(), getDomainValue(), getDataset(), getValueIndex());
  }

  public void setMouseEvent(MouseEventWrapper e) {
    this.mouseEvent = e;
  }

  @Nullable
  public MouseEventWrapper getMouseEvent() {
    return mouseEvent;
  }
}
