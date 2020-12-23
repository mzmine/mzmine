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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.data.xy.XYZDataset;

public class ColoredXYZDataset extends ColoredXYDataset implements XYZDataset, PaintScaleProvider {

  private static final String FALLBACK_PAINTSCALE_STYLE = "Rainbow";

  private final XYZValueProvider xyzValueProvider;
  protected List<Double> zValues;
  protected Double minZValue;
  protected Double maxZValue;
  protected LookupPaintScale paintScale;

  protected Double boxWidth;
  protected Double boxHeight;
  protected AbstractXYItemRenderer renderer;

  public void setBoxWidth(double boxWidth) {
    this.boxWidth = boxWidth;
  }

  public void setBoxHeight(double boxHeight) {
    this.boxHeight = boxHeight;
  }

  public boolean isUseAlphaInPaintscale() {
    return useAlphaInPaintscale;
  }

  public void setUseAlphaInPaintscale(boolean useAlphaInPaintscale) {
    this.useAlphaInPaintscale = useAlphaInPaintscale;
  }

  protected boolean useAlphaInPaintscale;

  public ColoredXYZDataset(@Nonnull PlotXYZDataProvider dataProvider) {
    this(dataProvider, true);
  }

  public ColoredXYZDataset(@Nonnull PlotXYZDataProvider dataProvider,
      final boolean useAlphaInPaintscale) {
    super(dataProvider, false);
    this.xyzValueProvider = dataProvider;
    zValues = Collections.emptyList();
    minZValue = Double.MAX_VALUE;
    maxZValue = Double.MIN_VALUE;
    renderer = new XYBlockPixelSizeRenderer();
    paintScale = null;
    this.useAlphaInPaintscale = useAlphaInPaintscale;
    MZmineCore.getTaskController().addTask(this);
  }

  public XYZValueProvider getXyzValueProvider() {
    return xyzValueProvider;
  }

  @Override
  public Number getZ(int series, int item) {
    if (item > computedItemCount) {
      return 0;
    }
    return zValues.get(item);
  }

  @Override
  public double getZValue(int series, int item) {
    if (item > computedItemCount) {
      return 0;
    }
    return zValues.get(item);
  }

  public double getMinZValue() {
    return minZValue;
  }

  public double getMaxZValue() {
    return maxZValue;
  }

  private LookupPaintScale computePaintScale(double min, double max) {
    // get index in accordance to percentile windows
    Color[] contourColors = XYBlockPixelSizePaintScales
        .getPaintColors("", Range.closed(min, max), FALLBACK_PAINTSCALE_STYLE);
    contourColors = XYBlockPixelSizePaintScales.scaleAlphaForPaintScale(contourColors);
    LookupPaintScale scale = new LookupPaintScale(min, max, Color.BLACK);

    double[] scaleValues = new double[contourColors.length];
    double delta = (max - min) / (contourColors.length - 1);
    double value = min;
    for (int i = 0; i < contourColors.length; i++) {
      scaleValues[i] = value;
      scale.add(value, contourColors[i]);
      value = value + delta;
    }

    return scale;
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

  public Double getBoxHeight() {
    return boxHeight;
  }

  public int getValueIndex(final double domainValue, final double rangeValue) {
    // todo binary search somehow here
    for (int i = 0; i < computedItemCount; i++) {
      if (Double.compare(domainValue, getX(0, i).doubleValue()) == 0
          && Double.compare(rangeValue, getY(0, i).doubleValue()) == 0) {
        return i;
      }
    }
    return -1;
  }

  private void calculateDefaultBoxHeightForPlots() {
    List<Double> yValuesSorted = getYValues().stream().sorted(Double::compareTo)
        .collect(Collectors.toList());

    List<Double> ydeltas = new ArrayList<>();
    Double yA = null;

    for (Double y : yValuesSorted) {
      if (yA == null) {
        yA = y;
      } else if (!(yA).equals(y)) {
        ydeltas.add(yA - y);
        yA = y;
      }
    }
    Collections.sort(ydeltas);
    Double medianY = 0.d;
    if (ydeltas.size() >= 2) {
      if (ydeltas.size() % 2 == 0) {
        int indexA = ydeltas.size() / 2;
        int indexB = ydeltas.size() / 2 - 1;
        medianY = (ydeltas.get(indexA) + ydeltas.get(indexB)) / 2;
      } else {
        int index = ydeltas.size() / 2;
        medianY = ydeltas.get(index);
      }
    }
    boxHeight = medianY;
  }


  private void calculateDefaultBoxWidthForPlots() {
    List<Double> xValuesSorted = getXValues().stream().sorted(Double::compareTo)
        .collect(Collectors.toList());
    List<Double> xdeltas = new ArrayList<>();
    Double xA = null;
    Collections.sort(xdeltas);
    Double medianX = 0.d;
    for (Double x : xValuesSorted) {
      if (xA == null) {
        xA = x;
      } else if (!(xA).equals(x)) {
        xdeltas.add(xA - x);
        xA = x;
      }
    }
    if (xdeltas.size() >= 2) {
      if (xdeltas.size() % 2 == 0) {
        int indexA = xdeltas.size() / 2;
        int indexB = xdeltas.size() / 2 - 1;
        medianX = (xdeltas.get(indexA) + xdeltas.get(indexB)) / 2;
      } else {
        int index = xdeltas.size() / 2;
        medianX = xdeltas.get(index);
      }
    }

    boxWidth = medianX;
  }


  @Override
  public void run() {
    status = TaskStatus.PROCESSING;
    xyValueProvider.computeValues();

    if (status == TaskStatus.CANCELED) {
      return;
    }
    if (xyzValueProvider.getDomainValues().size() != xyzValueProvider.getRangeValues().size()
        || xyzValueProvider.getZValues().size() != xyzValueProvider.getRangeValues().size()) {
      throw new IllegalArgumentException("Number of domain, range or z values does not match.");
    }

    rangeValues = xyzValueProvider.getRangeValues();
    domainValues = xyzValueProvider.getDomainValues();
    zValues = xyzValueProvider.getZValues();

    for (Double rangeValue : rangeValues) {
      if (rangeValue < minRangeValue) {
        minRangeValue = rangeValue;
      }
    }
    for (Double zValue : zValues) {
      if (zValue < minZValue) {
        minZValue = zValue;
      }
      if (zValue > maxZValue) {
        maxZValue = zValue;
      }
    }

    boxHeight = xyzValueProvider.getBoxHeight();
    boxWidth = xyzValueProvider.getBoxWidth();
    if (boxHeight == null) {
      calculateDefaultBoxHeightForPlots();
    }
    if (boxWidth == null) {
      calculateDefaultBoxWidthForPlots();
    }

    this.paintScale = computePaintScale(minZValue, maxZValue);

    computedItemCount = domainValues.size();
    computed = true;
    status = TaskStatus.FINISHED;

    Platform.runLater(this::fireDatasetChanged);
  }
}
