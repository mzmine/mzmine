/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 *
 */

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import com.google.common.collect.Range;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.FeatureImageProvider;
import java.awt.Color;
import java.util.Arrays;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javax.annotation.Nonnull;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ImageChart extends StackPane {

  private static Logger logger = Logger.getLogger(ImageChart.class.getName());
  private Double dataPointWidth;
  private Double dataPointHeight;
  private PaintScale paintScaleParameter;

  public ImageChart(@Nonnull ModularFeature f, AtomicDouble progress) {
    /*try {
      Double[] xValues = null;
      Double[] yValues = null;
      Double[] zValues = null;

      int fi = 0;
      List<? extends DataPoint> dps = f.getDataPoints();
      List<ImageDataPoint> dataPoints = new ArrayList<>();
      dataPoints.addAll((Collection<? extends ImageDataPoint>) dps);
      // add data points retention time -> intensity
      List<Double> xValuesSet = new ArrayList<>();
      List<Double> yValuesSet = new ArrayList<>();
      List<Double> zValuesSet = new ArrayList<>();
      for (ImageDataPoint dp : dataPoints) {
        if (dataPointHeight == null) {
          dataPointHeight = dp.getDataPointHeigth();
          dataPointWidth = dp.getDataPointWidth();
          paintScaleParameter = dp.getPaintScale();
        }
        xValuesSet.add(dp.getxWorld());
        yValuesSet.add(dp.getyWorld());
        zValuesSet.add(dp.getIntensity());
        if (progress != null) {
          progress.addAndGet(1.0 / dataPoints.size());
        }
      }
      xValues = new Double[xValuesSet.size()];
      xValues = xValuesSet.toArray(xValues);
      yValues = new Double[yValuesSet.size()];
      yValues = yValuesSet.toArray(yValues);
      zValues = new Double[zValuesSet.size()];
      zValues = zValuesSet.toArray(zValues);

      XYZDataset dataset = new ImageXYZDataset(xValues, yValues, zValues, "");
      ImageHeatMapPlot retentionTimeMobilityHeatMapPlot = new ImageHeatMapPlot(dataset,
          createPaintScale(zValues), dataPointWidth, dataPointHeight);
      this.getChildren().add(retentionTimeMobilityHeatMapPlot);

    } catch (Exception ex) {
      logger.log(Level.WARNING, "error in DP", ex);
    }*/

    FeatureImageProvider prov = new FeatureImageProvider(f);
    SimpleXYZScatterPlot<FeatureImageProvider> chart = new SimpleXYZScatterPlot<>();
    FastColoredXYZDataset ds = new FastColoredXYZDataset(prov);
    chart.setRangeAxisLabel("µm");
    chart.setDomainAxisLabel("µm");
    setPrefHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
    setPrefWidth(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH);
    chart.getChart().setBackgroundPaint(new Color(0, 0, 0, 0));
    getChildren().add(chart);
    Platform.runLater(() -> chart.setDataset(ds));
  }

  private PaintScale createPaintScale(Double[] zValues) {
    Double[] zValuesCopy = Arrays.copyOf(zValues, zValues.length);
    Arrays.sort(zValuesCopy);
    Range<Double> zValueRange = Range.closed(zValuesCopy[0], zValuesCopy[zValues.length - 1]);
    return new PaintScale(paintScaleParameter.getPaintScaleColorStyle(),
        paintScaleParameter.getPaintScaleBoundStyle(), zValueRange);
  }

}
