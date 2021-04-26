/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
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
import javafx.application.Platform;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

  private final XYZValueProvider xyzValueProvider;
  protected PaintScale paintScale;
  protected Double boxWidth;
  protected Double boxHeight;
  protected AbstractXYItemRenderer renderer;
  protected boolean useAlphaInPaintscale;
  protected Range<Double> zRange;

  public ColoredXYZDataset(@Nonnull PlotXYZDataProvider dataProvider) {
    this(dataProvider, true);
  }

  public ColoredXYZDataset(@Nonnull PlotXYZDataProvider dataProvider,
      final boolean useAlphaInPaintscale) {
    this(dataProvider, useAlphaInPaintscale, true);
  }


  ColoredXYZDataset(@Nonnull PlotXYZDataProvider dataProvider,
      final boolean useAlphaInPaintscale, boolean autocompute) {
    super(dataProvider, false);

    if (dataProvider instanceof PieXYZDataProvider) {
      throw new IllegalArgumentException(
          "PieXYZDataProviders can only be used with ColoredXYPieDatasets");
    }

    this.xyzValueProvider = dataProvider;
    this.useAlphaInPaintscale = useAlphaInPaintscale;
    renderer = new XYBlockPixelSizeRenderer();
    paintScale = null;
    if (autocompute) {
      MZmineCore.getTaskController().addTask(this);
    }
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

//  public void setPaintScale(PaintScale paintScale) {
//    this.paintScale = paintScale;
//  }

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

  private double calculateDefaultBoxDimensionForPlots(IntToDoubleFunction getter,
      int maxIndex) {
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
    /*var paintScale =
        new io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale(
            defaultPaintScaleColorStyle, defaultPaintScaleBoundStyle, zValueRange, Color.WHITE);
    PaintScaleFactory psf = new PaintScaleFactory();
    paintScale = psf.createColorsForPaintScale(paintScale, true);*/

    paintScale = MZmineCore.getConfiguration().getDefaultPaintScalePalette().toPaintScale(
        PaintScaleTransform.LINEAR, zValueRange);
    return paintScale;
  }

  @Override
  public void run() {
    status.set(TaskStatus.PROCESSING);
    xyValueProvider.computeValues(status);

    if (status.get() == TaskStatus.CANCELED || status.get() == TaskStatus.ERROR) {
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

    domainRange = Range.closed(minDomain, maxDomain);
    rangeRange = Range.closed(minRange, maxRange);
    zRange = Range.closed(minZ, maxZ);

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

    computed = true;
    status.set(TaskStatus.FINISHED);

    if (!(this instanceof FastColoredXYZDataset)) { // no need to notify then, dataset will be up to date
      if (Platform.isFxApplicationThread()) {
        fireDatasetChanged();
      } else {
        Platform.runLater(this::fireDatasetChanged);
      }
    }
  }

  // Makes protected method public // TODO: possible alternatives?
  @Override
  public void fireDatasetChanged() {
    super.fireDatasetChanged();
  }

}
