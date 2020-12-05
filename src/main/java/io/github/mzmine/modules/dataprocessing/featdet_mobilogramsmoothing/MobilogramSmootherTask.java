package io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.MobilityDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.SimpleMobilogram;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;

public class MobilogramSmootherTask extends AbstractTask {

  private final List<Frame> frames;
  private final LoessInterpolator loess;
  private final double bandwidth;
  private final int totalFrames;
  private int processedFrames;


  public MobilogramSmootherTask(List<Frame> frames, ParameterSet parameters) {
    bandwidth = 0.08;
//    this.loess = new LoessInterpolator(bandwidth, 0);
    this.loess = new LoessInterpolator();
    this.frames = frames;

    totalFrames = frames.size();
    processedFrames = 0;
    setStatus(TaskStatus.WAITING);
  }

  @Override
  public String getTaskDescription() {
    return "Smoothing mobilogram " + processedFrames + "/" + totalFrames;
  }

  @Override
  public double getFinishedPercentage() {
    return (double) processedFrames / totalFrames;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    for (Frame frame : frames) {
      List<Mobilogram> smoothedMobilograms = new ArrayList<>(frame.getMobilograms().size());

      for (Mobilogram mobilogram : frame.getMobilograms()) {
        if (isCanceled()) {
          return;
        }

        SimpleMobilogram smoothedMobilogram = smoothMobilogram(mobilogram, loess);
        smoothedMobilograms.add(smoothedMobilogram);

      }

      frame.clearMobilograms();
      smoothedMobilograms.forEach(frame::addMobilogram);

      processedFrames++;
    }

    setStatus(TaskStatus.FINISHED);
  }

  public static SimpleMobilogram smoothMobilogram(Mobilogram mobilogram, LoessInterpolator loess) {
    List<MobilityDataPoint> dataPoints = mobilogram.getDataPoints();
    // loess needs x val in ascending order, tims has descending mobility
    Collections.reverse(dataPoints);

    double[] smoothedIntensity = loess
        .smooth(dataPoints.stream().mapToDouble(MobilityDataPoint::getMobility).toArray(),
            dataPoints.stream().mapToDouble(MobilityDataPoint::getIntensity).toArray());

    SimpleMobilogram smoothedMobilogram = new SimpleMobilogram(mobilogram.getMobilityType(),
        mobilogram.getRawDataFile());
    for (int i = 0; i < dataPoints.size(); i++) {
      // keep in mind
      MobilityDataPoint dp = dataPoints.get(dataPoints.size() - 1 - i);

      smoothedMobilogram.addDataPoint(new MobilityDataPoint(dp.getMZ(),
          smoothedIntensity[i], dp.getMobility(), dp.getScanNum()));
    }
//        smoothedMobilogram.fillEdgesWithZeros(3);
    smoothedMobilogram.calc();
    return smoothedMobilogram;
  }
}
