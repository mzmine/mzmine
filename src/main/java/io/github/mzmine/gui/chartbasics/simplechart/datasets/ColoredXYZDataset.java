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
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizePaintScales;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PaintScaleProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYZValueProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javafx.application.Platform;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.renderer.LookupPaintScale;
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

  private static final String FALLBACK_PAINTSCALE_STYLE = "Rainbow";

  private final XYZValueProvider xyzValueProvider;
  protected Double minZValue;
  protected Double maxZValue;
  protected LookupPaintScale paintScale;
  protected String paintScaleString;
  protected Double boxWidth;
  protected Double boxHeight;
  protected AbstractXYItemRenderer renderer;
  protected boolean useAlphaInPaintscale;

  public ColoredXYZDataset(@Nonnull PlotXYZDataProvider dataProvider) {
    this(dataProvider, true);
  }

  public ColoredXYZDataset(@Nonnull PlotXYZDataProvider dataProvider,
      final boolean useAlphaInPaintscale) {
    super(dataProvider, false);
    this.xyzValueProvider = dataProvider;
    minZValue = Double.MAX_VALUE;
    maxZValue = Double.MIN_VALUE;
    renderer = new XYBlockPixelSizeRenderer();
    paintScale = null;
    this.useAlphaInPaintscale = useAlphaInPaintscale;
    MZmineCore.getTaskController().addTask(this);
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
    if (item > computedItemCount) {
      return 0;
    }
    return xyzValueProvider.getZValue(item);
  }

  @Override
  public double getZValue(int series, int item) {
    if (item > computedItemCount) {
      return 0;
    }
    return xyzValueProvider.getZValue(item);
  }

  public double getMinZValue() {
    return minZValue;
  }

  public double getMaxZValue() {
    return maxZValue;
  }

  /**
   * see {@link XYBlockPixelSizePaintScales}
   *
   * @param paintScaleString
   */
  public void setPaintScaleString(String paintScaleString) {
    this.paintScaleString = paintScaleString;
  }

  /**
   * The {@link PaintScale} this data will be drawn with.
   *
   * @return A paint scale. If null, a default paint scale will be used.
   */
  @Nullable
  @Override
  public LookupPaintScale getPaintScale() {
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

  public int getValueIndex(final double domainValue, final double rangeValue) {
    for (int i = 0; i < computedItemCount; i++) {
      if (Double.compare(domainValue, getX(0, i).doubleValue()) == 0
          && Double.compare(rangeValue, getY(0, i).doubleValue()) == 0) {
        return i;
      }
    }
    return -1;
  }

  private double calculateDefaultBoxDimensionForPlots(Function<Integer, Double> getter,
      int maxIndex) {
    double[] valuesSorted = new double[maxIndex];
    for (int i = 0; i < maxIndex; i++) {
      valuesSorted[i] = getter.apply(i).doubleValue();
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

  private LookupPaintScale computePaintScale(double min, double max) {
    if (paintScaleString == null || paintScaleString.isEmpty()) {
      paintScaleString = FALLBACK_PAINTSCALE_STYLE;
    }

    // get index in accordance to percentile windows
    Color[] contourColors = XYBlockPixelSizePaintScales
        .getPaintColors("", Range.closed(min, max), paintScaleString);
    if (contourColors == null) {
      contourColors = XYBlockPixelSizePaintScales
          .getPaintColors("", Range.closed(min, max), FALLBACK_PAINTSCALE_STYLE);
    }
    if (useAlphaInPaintscale) {
      contourColors = XYBlockPixelSizePaintScales.scaleAlphaForPaintScale(contourColors);
    }
    LookupPaintScale scale = new LookupPaintScale(min, max, Color.BLACK);

//    double[] scaleValues = new double[contourColors.length];
    double delta = (max - min) / (contourColors.length - 1);
    double value = min;
    for (int i = 0; i < contourColors.length; i++) {
//      scaleValues[i] = value;
      scale.add(value, contourColors[i]);
      value = value + delta;
    }

    return scale;
  }

  @Override
  public void run() {
    status.set(TaskStatus.PROCESSING);
    xyValueProvider.computeValues(status);

    if (status.get() != TaskStatus.PROCESSING) {
      return;
    }

    computedItemCount = xyValueProvider.getValueCount();
    for (int i = 0; i < computedItemCount; i++) {
      if (minRangeValue.doubleValue() < xyValueProvider.getRangeValue(i)) {
        minRangeValue = xyValueProvider.getRangeValue(i);
      }
      if (xyzValueProvider.getZValue(i) < minZValue) {
        minZValue = xyzValueProvider.getZValue(i);
      }
      if (xyzValueProvider.getZValue(i) > maxZValue) {
        maxZValue = xyzValueProvider.getZValue(i);
      }
    }

    boxHeight = xyzValueProvider.getBoxHeight();
    boxWidth = xyzValueProvider.getBoxWidth();
    if (boxHeight == null) {
      boxHeight = calculateDefaultBoxDimensionForPlots(i -> getYValue(0, i), computedItemCount);
    }
    if (boxWidth == null) {
      boxWidth = calculateDefaultBoxDimensionForPlots(i -> getXValue(0, i), computedItemCount);
    }

    this.paintScale = computePaintScale(minZValue, maxZValue);

    computed = true;
    status.set(TaskStatus.FINISHED);

    Platform.runLater(this::fireDatasetChanged);
  }
}
