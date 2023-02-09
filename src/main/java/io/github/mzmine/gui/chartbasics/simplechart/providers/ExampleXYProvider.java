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
import javafx.beans.property.Property;

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
  private final List<DataPoint> originalDatapoints;

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
    return "\nm/z " + mzFormat.format(sortedDps.get(itemIndex).getMZ()) + "\nIntensity: "
        + intensityFormat.format(yValues.get(itemIndex));
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
  public void computeValues(Property<TaskStatus> status) {
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
