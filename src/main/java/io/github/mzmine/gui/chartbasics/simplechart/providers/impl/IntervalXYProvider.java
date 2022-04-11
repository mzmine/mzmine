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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl;

import io.github.mzmine.gui.chartbasics.simplechart.providers.IntervalWidthProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import javafx.beans.property.SimpleObjectProperty;
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
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {

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
}
