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

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.XYDataset;

/**
 * Contains information about the currently selected point. The point is determined by the {@link
 * org.jfree.chart.plot.XYPlot#getDomainCrosshairValue()} and {@link
 * org.jfree.chart.plot.XYPlot#getRangeCrosshairValue()} methods. A listener can be added via the
 * {@link SimpleXYChart#cursorPositionProperty()}.
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
    return Double.compare(that.getRangeValue(), getRangeValue()) == 0 &&
        Double.compare(that.getDomainValue(), getDomainValue()) == 0 &&
        getValueIndex() == that.getValueIndex() &&
        Objects.equals(getDataset(), that.getDataset());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRangeValue(), getDomainValue(), getDataset(), getValueIndex());
  }
}
