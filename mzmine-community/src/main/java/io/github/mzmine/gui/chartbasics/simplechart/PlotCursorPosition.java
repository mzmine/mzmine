/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.gui.chartbasics.gestures.ChartGestureEvent;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.PlotCursorConfigModel;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.PlotCursorUtils;
import io.github.mzmine.gui.chartbasics.gui.wrapper.MouseEventWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

/**
 * Contains information about the currently selected point. The point is determined by the
 * {@link PlotCursorUtils#findSetCursorPosition(ChartGestureEvent, ChartRenderingInfo, XYPlot,
 * ObjectProperty)} usually to the data point closest in on screen measures.
 * <p>
 * {@link PlotCursorConfigModel#getDomainCursorMarker()} and its range equivalent are auto updated
 * to the coordinates described by this {@link PlotCursorPosition}. The domain cursor marker
 * property may be a better subscription target if only the value is important, while this plot
 * cursor position may change because of any internal value change like another click event.
 * <p>
 * A listener can be added via the {@link SimpleXYChart#cursorPositionProperty()}.
 *
 * @author https://github.com/SteffenHeu
 */
public class PlotCursorPosition {

  public static final double DEFAULT_Z_VALUE = -1.0d;

  private final double rangeValue;
  private final double domainValue;
  private final double zValue;
  private final @Nullable XYDataset dataset;
  private final int valueIndex;
  private final @Nullable MouseEventWrapper mouseEvent;

  public PlotCursorPosition(final double domainVal, final double rangeVal, final double zValue,
      final int valueIndex, final XYDataset dataset) {
    this(domainVal, rangeVal, zValue, valueIndex, dataset, null);
  }

  public PlotCursorPosition(final double domainVal, final double rangeVal, final double zValue,
      final int valueIndex, final XYDataset dataset, @Nullable MouseEventWrapper mouseEvent) {
    this.rangeValue = rangeVal;
    this.domainValue = domainVal;
    this.valueIndex = valueIndex;
    this.zValue = zValue;
    this.dataset = dataset;
    this.mouseEvent = mouseEvent;
  }

  public PlotCursorPosition(final double domainVal, final double rangeVal, final int valueIndex,
      final XYDataset dataset) {
    this(domainVal, rangeVal, valueIndex, dataset, null);
  }

  public PlotCursorPosition(final double domainVal, final double rangeVal, final int valueIndex,
      final XYDataset dataset, @Nullable MouseEventWrapper mouseEvent) {
    this(domainVal, rangeVal, DEFAULT_Z_VALUE, valueIndex, dataset, mouseEvent);
  }

  public PlotCursorPosition(PlotCursorPosition source) {
    this(source.getDomainValue(), source.getRangeValue(), source.getzValue(),
        source.getValueIndex(), source.getDataset(), source.getMouseEvent());
  }

  public PlotCursorPosition(@Nullable Double domain, @Nullable Double range) {
    this(domain == null ? 0 : domain, range == null ? 0 : range, DEFAULT_Z_VALUE, -1, null, null);
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

  @Nullable
  public MouseEventWrapper getMouseEvent() {
    return mouseEvent;
  }

}
