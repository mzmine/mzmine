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

package io.github.mzmine.gui.chartbasics.simplechart.providers;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Example implementation of a PlotXYDatasetProvider. This can be your usual data-class, you just
 * need to implement the methods described in {@link PlotXYDataProvider}.
 *
 * @author https://github.com/SteffenHeu
 */
public class ExampleXYProvider implements PlotXYDataProvider {

  private final String seriesKey;
  private final Color awt;

  /**
   * These will be passed to the plot.
   */
  private final List<Double> xValues;
  private final List<Double> yValues;


  private final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
  private final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

  /**
   * in case our values need to be resorted, we need to store the resorted values.
   */
  private List<DataPoint> sortedDps;
  private List<DataPoint> originalDatapoints;

  private double finishedPercentage;


  public ExampleXYProvider(List<DataPoint> originalDataPoints) {
    this.seriesKey = "Some series key";
    this.awt = MZmineCore.getConfiguration().getDefaultColorPalette().getNextColorAWT();

    // note we just create the lists there, but the values are calculated in the computeValues()
    // method below.
    yValues = new ArrayList<>();
    xValues = new ArrayList<>();
    this.originalDatapoints = originalDataPoints;

    finishedPercentage = 0d;
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
    return mzFormat.format(sortedDps.get(index).getMZ());
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return "\nm/z " + mzFormat.format(sortedDps.get(itemIndex).getMZ())
        + "\nIntensity: " + intensityFormat.format(yValues.get(itemIndex));
  }

  @Override
  public double getDomainValue(int index) {
    return xValues.get(index);
  }

  @Override
  public double getRangeValue(int index) {
    return yValues.get(index);
  }

  @Override
  public int getValueCount() {
    return xValues.size();
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    // here we can load and sort our data. remember to store the sorted data, so we can generate
    // labels based on the indices.
    sortedDps = originalDatapoints.stream().sorted(Comparator.comparingDouble(DataPoint::getMZ))
        .collect(Collectors.toList());
    finishedPercentage = 0.5;

    // add the sorted values to our domain & range value list
    for (DataPoint dp : sortedDps) {
      xValues.add(dp.getMZ());
      yValues.add(dp.getIntensity());
    }

    finishedPercentage = 1.d;
  }

  @Override
  public double getComputationFinishedPercentage() {
    return finishedPercentage;
  }
}
