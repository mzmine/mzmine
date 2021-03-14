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
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.imageplot.ImageHeatMapPlot;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.imageplot.ImageXYZDataset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.layout.StackPane;
import javax.annotation.Nonnull;
import org.jfree.data.xy.XYZDataset;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ImageChart extends StackPane {

  private Double dataPointWidth;
  private Double dataPointHeight;
  private PaintScale paintScaleParameter;

  private static Logger logger = Logger.getLogger(ImageChart.class.getName());

  public ImageChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {
    try {
      Double[] xValues = null;
      Double[] yValues = null;
      Double[] zValues = null;

      int size = row.getFilesFeatures().size();
      int fi = 0;
      for (Feature f : row.getFeatures()) {
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
          if (progress != null)
            progress.addAndGet(1.0 / size / dataPoints.size());
        }
        xValues = new Double[xValuesSet.size()];
        xValues = xValuesSet.toArray(xValues);
        yValues = new Double[yValuesSet.size()];
        yValues = yValuesSet.toArray(yValues);
        zValues = new Double[zValuesSet.size()];
        zValues = zValuesSet.toArray(zValues);

        if (progress != null)
          progress.set((double) fi / size);
        XYZDataset dataset = new ImageXYZDataset(xValues, yValues, zValues, "");
        ImageHeatMapPlot retentionTimeMobilityHeatMapPlot = new ImageHeatMapPlot(dataset,
            createPaintScale(zValues), dataPointWidth, dataPointHeight);
        this.getChildren().add(retentionTimeMobilityHeatMapPlot);
      }
    } catch (Exception ex) {
      logger.log(Level.WARNING, "error in DP", ex);
    }
  }

  private PaintScale createPaintScale(Double[] zValues) {
    Double[] zValuesCopy = Arrays.copyOf(zValues, zValues.length);
    Arrays.sort(zValuesCopy);
    Range<Double> zValueRange = Range.closed(zValuesCopy[0], zValuesCopy[zValues.length - 1]);
    return new PaintScale(paintScaleParameter.getPaintScaleColorStyle(),
        paintScaleParameter.getPaintScaleBoundStyle(), zValueRange);
  }

}
