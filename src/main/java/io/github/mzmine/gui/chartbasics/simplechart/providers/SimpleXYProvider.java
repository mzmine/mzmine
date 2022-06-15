/*
 * Copyright 2006-2022 The MZmine Development Team
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
