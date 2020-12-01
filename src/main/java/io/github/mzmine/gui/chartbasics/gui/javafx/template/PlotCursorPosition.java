/*
 *
 *  * Copyright 2006-2020 The MZmine Development Team
 *  *
 *  * This file is part of MZmine.
 *  *
 *  * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  * General Public License as published by the Free Software Foundation; either version 2 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  * Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  * USA
 *
 *
 */

package io.github.mzmine.gui.chartbasics.gui.javafx.template;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Contains information about the currently selected point
 */
public class PlotCursorPosition {

  final double rangeValue;
  final double domainValue;
  final ColoredXYDataset dataset;
  final int valueIndex;

  public PlotCursorPosition(double rangeVal, double domainVal,
      int valueIndex, ColoredXYDataset dataset) {
    this.rangeValue = rangeVal;
    this.domainValue = domainVal;
    this.valueIndex = valueIndex;
    this.dataset = dataset;
  }

  public double getRangeValue() {
    return rangeValue;
  }

  public double getDomainValue() {
    return domainValue;
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
  public ColoredXYDataset getDataset() {
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
