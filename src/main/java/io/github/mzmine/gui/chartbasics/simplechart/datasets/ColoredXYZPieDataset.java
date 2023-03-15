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

package io.github.mzmine.gui.chartbasics.simplechart.datasets;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PieXYZDataProvider;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.data.xy.XYZDataset;

/**
 * Used to plot XYZ datasets in a scatterplot-type of plot. Used to display spatial distribution in
 * imaging and ion mobility heatmaps.
 *
 * @author https://github.com/SteffenHeu
 */
public class ColoredXYZPieDataset<T> extends ColoredXYDataset implements XYZDataset {

  private final PieXYZDataProvider<T> pieDataProvider;
  private final RunOption runOption;
  protected AbstractXYItemRenderer renderer;
  protected double[] summedZValues;
  protected T[] sliceIdentifiers;
  private Range<Double> zRange;

  public ColoredXYZPieDataset(@NotNull PieXYZDataProvider<T> dataProvider) {
    this(dataProvider, RunOption.NEW_THREAD);
  }

  public ColoredXYZPieDataset(@NotNull PieXYZDataProvider<T> dataProvider,
      @NotNull final RunOption runOption) {
    super(dataProvider, RunOption.DO_NOT_RUN);
    this.pieDataProvider = dataProvider;
    renderer = new XYBlockPixelSizeRenderer();

    this.runOption = checkRunOption(runOption);
    handleRunOption(runOption);
  }

  public PieXYZDataProvider<T> getPieDataProvider() {
    return pieDataProvider;
  }

  @Override
  public Number getZ(int series, int item) {
    if (!valuesComputed) {
      return 0.0;
    }
    return pieDataProvider.getZValue(item);
  }

  public double getZValue(int item) {
    if (!valuesComputed) {
      return 0.0;
    }
    return pieDataProvider.getZValue(item);
  }

  @Override
  public double getZValue(int series, int item) {
    return pieDataProvider.getZValue(series, item);
  }

  @Override
  public int getSeriesCount() {
    return sliceIdentifiers != null ? sliceIdentifiers.length : 0;
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return "Pie data set";
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return pieDataProvider.getLabelForSeries(series);
  }

  public double getPieDiameter(int index) {
    return pieDataProvider.getPieDiameter(index);
  }

  @Override
  public int getValueIndex(final double domainValue, final double rangeValue) {
    for (int i = 0; i < computedItemCount; i++) {
      if (Double.compare(domainValue, getX(0, i).doubleValue()) == 0
          && Double.compare(rangeValue, getY(0, i).doubleValue()) == 0) {
        return i;
      }
    }
    return -1;
  }

  public Color getSliceColor(int series) {
    return pieDataProvider.getSliceColor(series);
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    xyValueProvider.computeValues(status);

    if (status.getValue() != TaskStatus.PROCESSING) {
      return;
    }

    computedItemCount = xyValueProvider.getValueCount();
    sliceIdentifiers = pieDataProvider.getSliceIdentifiers();
    valuesComputed = true;
    summedZValues = new double[computedItemCount];

    double minDomain = Double.POSITIVE_INFINITY;
    double maxDomain = Double.NEGATIVE_INFINITY;
    double minRange = Double.POSITIVE_INFINITY;
    double maxRange = Double.NEGATIVE_INFINITY;
    double minZ = Double.POSITIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < computedItemCount; i++) {
      final double rangeValue = xyValueProvider.getRangeValue(i);
      final double domainValue = xyValueProvider.getDomainValue(i);
      final double zValue = pieDataProvider.getZValue(i);

      minDomain = Math.min(domainValue, minDomain);
      maxDomain = Math.max(domainValue, maxDomain);
      minRange = Math.min(rangeValue, minRange);
      maxRange = Math.max(rangeValue, maxRange);
      minZ = Math.min(zValue, minZ);
      maxZ = Math.max(zValue, maxZ);

      for (int j = 0; j < sliceIdentifiers.length; j++) {
        summedZValues[i] += getZValue(j, i);
      }
    }

    domainRange = computedItemCount > 0 ? Range.closed(minDomain, maxDomain) : Range.closed(0d, 1d);
    rangeRange = computedItemCount > 0 ? Range.closed(minRange, maxRange) : Range.closed(0d, 1d);
    zRange = computedItemCount > 0 ? Range.closed(minZ, maxZ) : Range.closed(0d, 1d);

    computed = true;
    onCalculationsFinished();
  }

  public Range<Double> getZValueRange() {
    return zRange;
  }

  @Override
  protected void fireDatasetChanged() {
    super.fireDatasetChanged();
  }

  @Override
  protected RunOption getRunOption() {
    return runOption;
  }
}
