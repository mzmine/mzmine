package io.github.mzmine.datamodel.features.types.graphicalnodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.jfree.data.xy.XYZDataset;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.imageplot.ImageHeatMapPlot;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.imageplot.ImageXYZDataset;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.Coordinates;
import javafx.scene.layout.StackPane;

public class ImageChart extends StackPane {

  private Double dataPointWidth;
  private Double dataPointHeight;

  public ImageChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {
    try {
      Integer[] xValues = null;
      Integer[] yValues = null;
      Double[] zValues = null;

      int size = row.getFilesFeatures().size();
      int fi = 0;
      for (Feature f : row.getFeatures()) {
        List<DataPoint> dps = f.getDataPoints();
        List<ImageDataPoint> dataPoints = new ArrayList<>();
        dataPoints.addAll((Collection<? extends ImageDataPoint>) dps);
        // add data points retention time -> intensity
        List<Integer> xValuesSet = new ArrayList<>();
        List<Integer> yValuesSet = new ArrayList<>();
        List<Double> zValuesSet = new ArrayList<>();
        for (ImageDataPoint dp : dataPoints) {
          if (dataPointHeight == null) {
            dataPointHeight = dp.getDataPointHeigth();
            dataPointWidth = dp.getDataPointWidth();
          }
          Coordinates coordinates = dp.getCoordinates();
          xValuesSet.add(coordinates.getX());
          yValuesSet.add(coordinates.getY());
          zValuesSet.add(dp.getIntensity());
          if (progress != null)
            progress.addAndGet(1.0 / size / dataPoints.size());
        }
        xValues = new Integer[xValuesSet.size()];
        xValues = xValuesSet.toArray(xValues);
        yValues = new Integer[yValuesSet.size()];
        yValues = yValuesSet.toArray(yValues);
        zValues = new Double[zValuesSet.size()];
        zValues = zValuesSet.toArray(zValues);

        if (progress != null)
          progress.set((double) fi / size);
      }
      XYZDataset dataset = new ImageXYZDataset(xValues, yValues, zValues, "Test");
      ImageHeatMapPlot retentionTimeMobilityHeatMapPlot =
          new ImageHeatMapPlot(dataset, "Rainbow", dataPointWidth, dataPointHeight);
      this.getChildren().add(retentionTimeMobilityHeatMapPlot);
    } catch (Exception ex) {
      // logger.log(Level.WARNING, "error in DP", ex);
    }
  }

}
