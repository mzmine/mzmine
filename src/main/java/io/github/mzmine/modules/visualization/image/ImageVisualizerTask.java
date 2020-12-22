package io.github.mzmine.modules.visualization.image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.jfree.data.xy.XYZDataset;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.imageplot.ImageHeatMapPlot;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.imageplot.ImageXYZDataset;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.ImagingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.impl.StorableImagingScan;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;

public class ImageVisualizerTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final ParameterSet parameters;
  private final ImagingRawDataFile rawDataFile;
  private final ImagingParameters imagingParameters;
  private final ScanSelection scanSelection;
  private final Range<Double> mzRange;

  private double pixelWidth;
  private double pixelHeight;
  private double progress = 0.0;
  private String taskDescription = "";


  public ImageVisualizerTask(RawDataFile rawDataFile, ParameterSet parameters) {
    this.parameters = parameters;
    this.rawDataFile = (ImagingRawDataFile) rawDataFile;
    this.imagingParameters = ((ImagingRawDataFile) rawDataFile).getImagingParam();
    this.scanSelection =
        parameters.getParameter(ImageVisualizerParameters.scanSelection).getValue();
    this.mzRange = parameters.getParameter(ImageVisualizerParameters.mzRange).getValue();
    setStatus(TaskStatus.WAITING);
  }

  @Override
  public String getTaskDescription() {
    return taskDescription;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (isCanceled()) {
      return;
    }
    progress = 0.0;
    calculatePixelSize();
    Set<ImageDataPoint> imageDataPoints = extractAllDataPointsFromScans();
    ImageHeatMapPlot imageHeatMapPlot = createImageHeatMapPlot(imageDataPoints);
    Platform.runLater(() -> {
      ImageVisualizerTab newTab =
          new ImageVisualizerTab(parameters, imageHeatMapPlot, rawDataFile, imagingParameters);
      MZmineCore.getDesktop().addTab(newTab);
    });
    progress = 1.0;
    setStatus(TaskStatus.FINISHED);
  }

  private void calculatePixelSize() {
    pixelWidth = imagingParameters.getLateralWidth() / imagingParameters.getMaxNumberOfPixelX();
    pixelHeight = imagingParameters.getLateralHeight() / imagingParameters.getMaxNumberOfPixelY();
  }

  // Extract all data point sorted by intensity
  private Set<ImageDataPoint> extractAllDataPointsFromScans() {
    logger.info("Start data point extraction");
    taskDescription = "Get data points from scans";
    int processedScans = 1;
    SortedSet<ImageDataPoint> allDataPoints = new TreeSet<>(new Comparator<ImageDataPoint>() {
      @Override
      public int compare(ImageDataPoint o1, ImageDataPoint o2) {
        if (o1.getxWorld() > o2.getxWorld()) {
          return 1;
        } else {
          return -1;
        }
      }
    });
    Scan[] scans = scanSelection.getMatchingScans(rawDataFile);
    for (Scan scan : scans) {
      if (!(scan instanceof StorableImagingScan) || !scanSelection.matches(scan)) {
        continue;
      }
      double intensitySum = Arrays.stream(scan.getDataPointsByMass(mzRange))
          .mapToDouble(DataPoint::getIntensity).sum();
      allDataPoints.add(new ImageDataPoint(0.0, intensitySum, scan.getScanNumber(),
          (((StorableImagingScan) scan).getCoordinates().getX() + 1) * pixelWidth,
          (((StorableImagingScan) scan).getCoordinates().getY() + 1) * pixelHeight, 1, pixelHeight,
          pixelWidth));
      progress = (processedScans / (double) scans.length);
      processedScans++;
    }
    logger.info("Extracted " + allDataPoints.size() + " ims data points");
    return allDataPoints;
  }

  private ImageHeatMapPlot createImageHeatMapPlot(Set<ImageDataPoint> imageDataPoints) {
    Double[] xValues = null;
    Double[] yValues = null;
    Double[] zValues = null;
    Double dataPointWidth = null;
    Double dataPointHeight = null;
    // add data points retention time -> intensity
    List<Double> xValuesSet = new ArrayList<>();
    List<Double> yValuesSet = new ArrayList<>();
    List<Double> zValuesSet = new ArrayList<>();
    for (ImageDataPoint dp : imageDataPoints) {
      if (dataPointWidth == null) {
        dataPointHeight = dp.getDataPointHeigth();
        dataPointWidth = dp.getDataPointWidth();
      }
      xValuesSet.add(dp.getxWorld());
      yValuesSet.add(dp.getyWorld());
      zValuesSet.add(dp.getIntensity());
    }
    xValues = new Double[xValuesSet.size()];
    xValues = xValuesSet.toArray(xValues);
    yValues = new Double[yValuesSet.size()];
    yValues = yValuesSet.toArray(yValues);
    zValues = new Double[zValuesSet.size()];
    zValues = zValuesSet.toArray(zValues);

    XYZDataset dataset = new ImageXYZDataset(xValues, yValues, zValues, "Test");
    return new ImageHeatMapPlot(dataset, "Rainbow", dataPointWidth, dataPointHeight);
  }
}
