package io.github.mzmine.datamodel.features.types.graphicalnodes;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import io.github.mzmine.util.color.ColorsFX;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class FeatureShapeIonMobilityRetentionTimeChart extends StackPane {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public FeatureShapeIonMobilityRetentionTimeChart(@Nonnull ModularFeatureListRow row,
      AtomicDouble progress) {
    try {
      final NumberAxis xAxis = new NumberAxis();
      final NumberAxis yAxis = new NumberAxis();
      final LineChart<Number, Number> bc = new LineChart<>(xAxis, yAxis);

      double minRT = Double.MAX_VALUE;
      double maxRT = 0;
      int size = row.getFilesFeatures().size();
      int fi = 0;
      for (Feature f : row.getFeatures()) {
        XYChart.Series<Number, Number> data = new XYChart.Series<>();
        List<DataPoint> dataPoints = f.getDataPoints();
        RawDataFile raw = f.getRawDataFile();
        // add data points retention time -> intensity
        for (DataPoint dataPoint : dataPoints) {
          if (dataPoint instanceof RetentionTimeMobilityDataPoint) {
            RetentionTimeMobilityDataPoint dp = (RetentionTimeMobilityDataPoint) dataPoint;
            double retentionTime = dp.getRetentionTime();
            double intensity = dp == null ? 0 : dp.getIntensity();
            for (DataPoint dataPointCompare : dataPoints) {
              RetentionTimeMobilityDataPoint dpComapre =
                  (RetentionTimeMobilityDataPoint) dataPointCompare;
              if (dp != dpComapre && dpComapre.getRetentionTime() == retentionTime) {
                intensity = intensity + (dpComapre == null ? 0 : dpComapre.getIntensity());
              }
            }
            data.getData().add(new XYChart.Data<>(retentionTime, intensity));
            /*
             * if (dp != null && (max == null || max.getIntensity() < dp.getIntensity())) { max =
             * dp; }
             */
            if (retentionTime > maxRT) {
              maxRT = retentionTime;
            }
            if (retentionTime < minRT) {
              minRT = retentionTime;
            }
            if (progress != null)
              progress.addAndGet(1.0 / size / dataPoints.size());
          }
        }
        fi++;
        bc.getData().add(data);

        // set series color according to the rawDataFile color
        Node line = data.getNode().lookup(".chart-series-line");
        Color fileColor = raw.getColor();
        if (fileColor == null) {
          fileColor = Color.DARKORANGE;
        }
        line.setStyle("-fx-stroke: " + ColorsFX.toHexString(fileColor) + ";");

        if (progress != null)
          progress.set((double) fi / size);
      }

      bc.setLegendVisible(false);
      bc.setMinHeight(100);
      bc.setPrefHeight(100);
      bc.setMaxHeight(100);
      bc.setPrefWidth(150);
      bc.setCreateSymbols(false);

      // do not add data to chart
      xAxis.setAutoRanging(false);
      xAxis.setUpperBound(maxRT + 2);
      xAxis.setLowerBound(((minRT - 2) > 0) ? (minRT - 2) : 0);

      bc.setOnScroll(new EventHandler<>() {
        @Override
        public void handle(ScrollEvent event) {
          NumberAxis axis = xAxis;
          final double minX = xAxis.getLowerBound();
          final double maxX = xAxis.getUpperBound();
          double d = maxX - minX;
          double x = event.getX();
          double direction = event.getDeltaY();
          if (direction > 0) {
            if (d > 0.3) {
              axis.setLowerBound(minX + 0.1);
              axis.setUpperBound(maxX - 0.1);
            }
          } else {
            axis.setLowerBound(minX - 0.1);
            axis.setUpperBound(maxX + 0.1);
          }
          event.consume();
        }
      });

      this.getChildren().add(bc);
    } catch (Exception ex) {
      logger.log(Level.WARNING, "error in DP", ex);
    }
  }

}
