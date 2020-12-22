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
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDatasetProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYZValueProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import javafx.application.Platform;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.data.xy.XYZDataset;

public class ColoredXYZDataset extends ColoredXYDataset implements XYZDataset, PaintScaleProvider {

  private static final String FALLBACK_PAITNSCALE_STYLE = "Rainbow";

  private final XYZValueProvider xyzValueProvider;
  protected List<Double> zValues;
  protected Double minZValue;
  protected Double maxZValue;
  protected LookupPaintScale paintScale;

  protected double boxWidth;

  protected double boxHeight;
  protected AbstractXYItemRenderer renderer;

  public ColoredXYZDataset(@Nonnull PlotXYZDatasetProvider datasetProvider) {
    super(datasetProvider, false);
    this.xyzValueProvider = datasetProvider;
    zValues = Collections.emptyList();
    minZValue = Double.MAX_VALUE;
    maxZValue = Double.MIN_VALUE;
    renderer = new XYBlockPixelSizeRenderer();
    paintScale = null;
    MZmineCore.getTaskController().addTask(this);
  }

  public XYZValueProvider getXyzValueProvider() {
    return xyzValueProvider;
  }

  @Override
  public Number getZ(int series, int item) {
    return zValues.get(item);
  }

  @Override
  public double getZValue(int series, int item) {
    return zValues.get(item);
  }

  public double getMinZValue() {
    return minZValue;
  }

  public double getMaxZValue() {
    return maxZValue;
  }

  private PaintScale computePaintScale(double min, double max) {
    // get index in accordance to percentile windows
    Color[] contourColors = XYBlockPixelSizePaintScales
        .getPaintColors("", Range.closed(min, max), FALLBACK_PAITNSCALE_STYLE);
    // contourColors = XYBlockPixelSizePaintScales.scaleAlphaForPaintScale(contourColors);
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

  public double getBoxWidth() {
    return boxWidth;
  }

  public double getBoxHeight() {
    return boxHeight;
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
    computePaintScale(minZValue, maxZValue);

    computedItemCount = domainValues.size();
    computed = true;
    status = TaskStatus.FINISHED;
    Platform.runLater(this::fireDatasetChanged);
  }
}
