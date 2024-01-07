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
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PaintScaleProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PieXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYZValueProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntToDoubleFunction;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.data.xy.XYZDataset;

/**
 * Used to plot XYZ datasets in a scatterplot-type of plot. Used to display spatial distribution in
 * imaging and ion mobility heatmaps.
 *
 * @author https://github.com/SteffenHeu
 */
public class ColoredXYZDataset extends ColoredXYDataset implements XYZDataset, PaintScaleProvider {

  private static final Logger logger = Logger.getLogger(ColoredXYZDataset.class.getName());

  private final XYZValueProvider xyzValueProvider;
  private final RunOption runOption;
  protected PaintScale paintScale;
  protected Double boxWidth;
  protected Double boxHeight;
  protected AbstractXYItemRenderer renderer;
  protected boolean useAlphaInPaintscale;
  protected Range<Double> zRange;

  public ColoredXYZDataset(@NotNull PlotXYZDataProvider dataProvider) {
    this(dataProvider, true);
  }

  public ColoredXYZDataset(@NotNull PlotXYZDataProvider dataProvider,
      @NotNull final RunOption runOption) {
    this(dataProvider, true, runOption);
  }

  public ColoredXYZDataset(@NotNull PlotXYZDataProvider dataProvider,
      final boolean useAlphaInPaintscale) {
    this(dataProvider, useAlphaInPaintscale, RunOption.NEW_THREAD);
  }

  ColoredXYZDataset(@NotNull PlotXYZDataProvider dataProvider, final boolean useAlphaInPaintscale,
      @NotNull final RunOption runOption) {
    // do not run from super constructor! we need to do some other stuff first
    super(dataProvider, RunOption.DO_NOT_RUN);

    if (dataProvider instanceof PieXYZDataProvider) {
      throw new IllegalArgumentException(
          "PieXYZDataProviders can only be used with ColoredXYPieDatasets");
    }

    this.xyzValueProvider = dataProvider;
    this.useAlphaInPaintscale = useAlphaInPaintscale;
    renderer = new XYBlockPixelSizeRenderer();
    paintScale = null;

    this.runOption = checkRunOption(runOption);
    handleRunOption(runOption);
  }

  public boolean isUseAlphaInPaintscale() {
    return useAlphaInPaintscale;
  }

  public void setUseAlphaInPaintscale(boolean useAlphaInPaintscale) {
    this.useAlphaInPaintscale = useAlphaInPaintscale;
  }

  public XYZValueProvider getXyzValueProvider() {
    return xyzValueProvider;
  }

  @Override
  public Number getZ(int series, int item) {
    return getZValue(series, item);
  }

  @Override
  public double getZValue(int series, int item) {
    if (!valuesComputed) {
      return 0.0;
    }
    return xyzValueProvider.getZValue(item);
  }

  public Range<Double> getZValueRange() {
    return zRange;
  }

  /**
   * The {@link PaintScale} this data will be drawn with.
   *
   * @return A paint scale. If null, a default paint scale will be used.
   */
  @Nullable
  @Override
  public PaintScale getPaintScale() {
    return paintScale;
  }

  public Double getBoxWidth() {
    return boxWidth;
  }

  public void setBoxWidth(double boxWidth) {
    this.boxWidth = boxWidth;
  }

  public Double getBoxHeight() {
    return boxHeight;
  }

  public void setBoxHeight(double boxHeight) {
    this.boxHeight = boxHeight;
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

  private double calculateDefaultBoxDimensionForPlots(IntToDoubleFunction getter, int maxIndex) {
    double[] valuesSorted = new double[maxIndex];
    for (int i = 0; i < maxIndex; i++) {
      valuesSorted[i] = getter.applyAsDouble(i);
    }
    Arrays.sort(valuesSorted);

    List<Double> deltas = new ArrayList<>();
    Double yA = null;

    for (Double y : valuesSorted) {
      if (yA == null) {
        yA = y;
      } else if (!(yA).equals(y)) {
        deltas.add(yA - y);
        yA = y;
      }
    }
    Collections.sort(deltas);
    Double median = 0.d;
    if (deltas.size() >= 2) {
      if (deltas.size() % 2 == 0) {
        int indexA = deltas.size() / 2;
        int indexB = deltas.size() / 2 - 1;
        median = (deltas.get(indexA) + deltas.get(indexB)) / 2;
      } else {
        int index = deltas.size() / 2;
        median = deltas.get(index);
      }
    }
    return median;
  }

  private PaintScale createDefaultPaintScale(double min, double max) {
    if (min >= max) {
      min = 0;
      max = 1;
    }
    Range<Double> zValueRange = Range.closed(min, max);

    paintScale = MZmineCore.getConfiguration().getDefaultPaintScalePalette()
        .toPaintScale(PaintScaleTransform.LINEAR, zValueRange);
    return paintScale;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    xyValueProvider.computeValues(status);

    if (status.getValue() == TaskStatus.CANCELED || status.getValue() == TaskStatus.ERROR) {
      return;
    }

    computedItemCount = xyValueProvider.getValueCount();
    valuesComputed = true;

    double minDomain = Double.POSITIVE_INFINITY;
    double maxDomain = Double.NEGATIVE_INFINITY;
    double minRange = Double.POSITIVE_INFINITY;
    double maxRange = Double.NEGATIVE_INFINITY;
    double minZ = Double.POSITIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < computedItemCount; i++) {
      final double rangeValue = xyValueProvider.getRangeValue(i);
      final double domainValue = xyValueProvider.getDomainValue(i);
      final double zValue = xyzValueProvider.getZValue(i);

      minDomain = Math.min(domainValue, minDomain);
      maxDomain = Math.max(domainValue, maxDomain);
      minRange = Math.min(rangeValue, minRange);
      maxRange = Math.max(rangeValue, maxRange);
      minZ = Math.min(zValue, minZ);
      maxZ = Math.max(zValue, maxZ);
    }

    domainRange = computedItemCount > 0 ? Range.closed(minDomain, maxDomain) : Range.closed(0d, 1d);
    rangeRange = computedItemCount > 0 ? Range.closed(minRange, maxRange) : Range.closed(0d, 1d);
    zRange = computedItemCount > 0 ? Range.closed(minZ, maxZ) : Range.closed(0d, 1d);

    boxHeight = xyzValueProvider.getBoxHeight();
    boxWidth = xyzValueProvider.getBoxWidth();
    if (boxHeight == null) {
      boxHeight = calculateDefaultBoxDimensionForPlots(i -> getYValue(0, i), computedItemCount);
    }
    if (boxWidth == null) {
      boxWidth = calculateDefaultBoxDimensionForPlots(i -> getXValue(0, i), computedItemCount);
    }

    if (xyzValueProvider instanceof PaintScaleProvider) {
      paintScale = ((PaintScaleProvider) xyzValueProvider).getPaintScale();
    }
    paintScale = (paintScale != null) ? paintScale
        : createDefaultPaintScale(zRange.lowerEndpoint(), zRange.upperEndpoint());

    onCalculationsFinished();
  }

  // Makes protected method public // TODO: possible alternatives?
  @Override
  public void fireDatasetChanged() {
    super.fireDatasetChanged();
  }

  @Override
  protected RunOption getRunOption() {
    return runOption;
  }
}
