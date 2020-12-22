package io.github.mzmine.datamodel.features.types.graphicalnodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jfree.data.xy.XYZDataset;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.imageplot.ImageHeatMapPlot;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.imageplot.ImageXYZDataset;
import javafx.scene.layout.StackPane;

public class ImageChart extends StackPane {

  private Double dataPointWidth;
  private Double dataPointHeight;

  private static Logger logger = Logger.getLogger(ImageChart.class.getName());

  public ImageChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {
    try {
      Double[] xValues = null;
      Double[] yValues = null;
      Double[] zValues = null;

      int size = row.getFilesFeatures().size();
      int fi = 0;
      for (Feature f : row.getFeatures()) {
        ImagingRawDataFile rawDataFile = (ImagingRawDataFile) f.getRawDataFile();
        List<DataPoint> dps = f.getDataPoints();
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
        XYZDataset dataset = new ImageXYZDataset(xValues, yValues, zValues, "Test");
        ImageHeatMapPlot retentionTimeMobilityHeatMapPlot =
            new ImageHeatMapPlot(dataset, "Rainbow", dataPointWidth, dataPointHeight, rawDataFile);
        this.getChildren().add(retentionTimeMobilityHeatMapPlot);
      }
    } catch (Exception ex) {
      logger.log(Level.WARNING, "error in DP", ex);
    }
  }

}
