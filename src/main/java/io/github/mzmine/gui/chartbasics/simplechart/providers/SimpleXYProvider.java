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

import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Example implementation of a PlotXYDatasetProvider. This can be your usual data-class, you just
 * need to implement the methods described in {@link PlotXYDataProvider}.
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleXYProvider implements PlotXYDataProvider {

  private final String seriesKey;
  private final Color awt;
  private final NumberFormat rangeFormat;
  private final NumberFormat domainFormat;
  /**
   * These will be passed to the plot.
   */
  private double[] xValues;
  private double[] yValues;
  private double finishedPercentage;

  public SimpleXYProvider(String seriesKey, Color awt) {
    this(seriesKey, awt, null, null);
  }

  public SimpleXYProvider(String seriesKey, Color awt, double[] xValues, double[] yValues) {
    this(seriesKey, awt, xValues, yValues, new DecimalFormat(), new DecimalFormat());
  }

  public SimpleXYProvider(String seriesKey, Color awt, double[] xValues, double[] yValues,
      NumberFormat domainFormat, NumberFormat rangeFormat) {
    this.seriesKey = seriesKey;
    this.awt = awt;
    this.xValues = xValues;
    this.yValues = yValues;
    this.rangeFormat = rangeFormat;
    this.domainFormat = domainFormat;
  }

  protected void setxValues(double[] xValues) {
    this.xValues = xValues;
  }

  protected void setyValues(double[] yValues) {
    this.yValues = yValues;
  }

  @Override
  public Color getAWTColor() {
    return awt;
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return FxColorUtil.awtColorToFX(getAWTColor());
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Override
  public String getLabel(int index) {
    return domainFormat.format(getDomainValue(index));
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return "\nx: " + domainFormat.format(getDomainValue(itemIndex)) + "\ny: " + rangeFormat.format(
        getRangeValue(itemIndex));
  }

  @Override
  public double getDomainValue(int index) {
    return xValues[index];
  }

  @Override
  public double getRangeValue(int index) {
    return yValues[index];
  }

  @Override
  public int getValueCount() {
    return xValues.length;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    finishedPercentage = 1.d;
  }

  @Override
  public double getComputationFinishedPercentage() {
    return finishedPercentage;
  }
}
