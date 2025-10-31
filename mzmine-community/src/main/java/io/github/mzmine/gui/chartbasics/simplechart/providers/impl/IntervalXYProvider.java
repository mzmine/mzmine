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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl;

import io.github.mzmine.gui.chartbasics.simplechart.providers.IntervalWidthProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntervalXYProvider implements PlotXYDataProvider, IntervalWidthProvider {

  private final Color awtColor;
  private final String seriesKey;

  private final double[] domainValues;
  private final double[] rangeValues;
  private final double intervalWidth;

  public IntervalXYProvider(double[] domainValues, double[] rangeValues, double intervalWidth,
      Color awtColor, String seriesKey) {
    this.awtColor = awtColor;
    this.seriesKey = seriesKey;
    this.domainValues = domainValues;
    this.rangeValues = rangeValues;
    this.intervalWidth = intervalWidth;
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return awtColor;
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return FxColorUtil.awtColorToFX(awtColor);
  }

  @Override
  public double getIntervalWidth() {
    return intervalWidth;
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    return null;
  }

  @NotNull
  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    // nothing to do
  }

  @Override
  public double getDomainValue(int index) {
    return domainValues[index];
  }

  @Override
  public double getRangeValue(int index) {
    return rangeValues[index];
  }

  @Override
  public int getValueCount() {
    return domainValues.length;
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 0;
  }

  /**
   * @return true if computed. Providers that are precomputed may use true always
   */
  public boolean isComputed() {
    return true;
  }
}
