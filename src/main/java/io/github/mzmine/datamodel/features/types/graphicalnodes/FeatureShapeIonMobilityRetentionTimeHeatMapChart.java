package io.github.mzmine.datamodel.features.types.graphicalnodes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jfree.data.xy.XYZDataset;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.ionmobilitytraceplot.RetentionTimeMobilityHeatMapPlot;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.ionmobilitytraceplot.RetentionTimeMobilityXYZDataset;
import javafx.scene.layout.StackPane;

public class FeatureShapeIonMobilityRetentionTimeHeatMapChart extends StackPane {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public FeatureShapeIonMobilityRetentionTimeHeatMapChart(@Nonnull ModularFeatureListRow row,
      AtomicDouble progress) {
    try {
      Float[] xValues = null;
      Double[] yValues = null;
      Double[] zValues = null;

      int size = row.getFilesFeatures().size();
      int fi = 0;
      Double dataPointWidth = null;
      Double dataPointHeight = null;
      double minMobility = Double.MAX_VALUE, maxMobility = 0;
      double minRt = Double.MAX_VALUE, maxRt = 0;
      for (Feature f : row.getFeatures()) {
        List<DataPoint> dataPoints = f.getDataPoints();
        // add data points retention time -> intensity
        List<Float> xValuesSet = new ArrayList<>();
        List<Double> yValuesSet = new ArrayList<>();
        List<Double> zValuesSet = new ArrayList<>();
        for (DataPoint dataPoint : dataPoints) {
          if (dataPoint instanceof RetentionTimeMobilityDataPoint) {
            RetentionTimeMobilityDataPoint dp = (RetentionTimeMobilityDataPoint) dataPoint;
            if (dataPointHeight == null && dataPointWidth == null) {
              dataPointHeight = dp.getDataPointHeight();
              dataPointWidth = dp.getDataPointWidth();
            }
            xValuesSet.add(dp.getRetentionTime());
            yValuesSet.add(dp.getMobility());
            zValuesSet.add(dp.getIntensity());
            if (dp.getMobility() > maxMobility) {
              maxMobility = dp.getMobility();
            }
            if (dp.getMobility() < minMobility) {
              minMobility = dp.getMobility();
            }
            if (dp.getRetentionTime() > maxRt) {
              maxRt = dp.getRetentionTime();
            }
            if (dp.getRetentionTime() < minRt) {
              minRt = dp.getRetentionTime();
            }
            if (progress != null)
              progress.addAndGet(1.0 / size / dataPoints.size());
          }
        }
        xValues = new Float[xValuesSet.size()];
        xValues = xValuesSet.toArray(xValues);
        yValues = new Double[yValuesSet.size()];
        yValues = yValuesSet.toArray(yValues);
        zValues = new Double[zValuesSet.size()];
        zValues = zValuesSet.toArray(zValues);

        if (progress != null)
          progress.set((double) fi / size);
      }
      XYZDataset dataset = new RetentionTimeMobilityXYZDataset(xValues, yValues, zValues, "Test");
      RetentionTimeMobilityHeatMapPlot retentionTimeMobilityHeatMapPlot =
          new RetentionTimeMobilityHeatMapPlot(dataset, "Rainbow", dataPointWidth, dataPointHeight);
      this.getChildren().add(retentionTimeMobilityHeatMapPlot);
    } catch (Exception ex) {
      logger.log(Level.WARNING, "error in DP", ex);
    }
  }
}

