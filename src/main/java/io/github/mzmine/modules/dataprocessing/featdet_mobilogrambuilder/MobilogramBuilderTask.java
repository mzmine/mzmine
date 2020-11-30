package io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder;

import cern.colt.function.DoubleComparator;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.StorableFrame;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Worker task of the mobilogram builder
 */
public class MobilogramBuilderTask extends AbstractTask {

  private final List<Frame> frames;
  private final MZTolerance mzTolerance;
  private final String massList;
  private int processedFrames;
  private int totalFrames;


  public MobilogramBuilderTask(List<Frame> frames, ParameterSet parameters) {
    this.frames = frames;
    this.mzTolerance = parameters.getParameter(MobilogramBuilderParameters.mzTolerance).getValue();
    this.massList = parameters.getParameter(MobilogramBuilderParameters.massList).getValue();
    totalFrames = (frames.size() != 0) ? frames.size() : 1;
    setStatus(TaskStatus.WAITING);
  }

  @Override
  public String getTaskDescription() {
    return "Detecting mobilograms for frames. " + processedFrames + "/" + totalFrames;
  }

  @Override
  public double getFinishedPercentage() {
    return (int) (processedFrames / totalFrames);
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    for (Frame frame : frames) {

      if(isCanceled()) {
        return;
      }

      if (!(frame instanceof StorableFrame)) {
        continue;
      }

      List<Mobilogram> mobilograms = calculateMobilogramsForScans(frame.getMobilityScans());
      processedFrames++;
      frame.getMobilograms().addAll(mobilograms);
    }

    setStatus(TaskStatus.FINISHED);
  }

  protected List<Mobilogram> calculateMobilogramsForScans(List<Scan> scans) {
    if (scans.size() == 0 || scans.get(0).getMassList(massList) == null) {
      return Collections.emptyList();
    }

    final MobilityType mobilityType = scans.get(0).getMobilityType();
    int numDp = 0;

    for (Scan scan : scans) {
      numDp += scan.getMassList(massList).getDataPoints().length;
    }
    final List<MobilityDataPoint> allDps = new ArrayList<>(numDp);

    for (Scan scan : scans) {
      Arrays.stream(scan.getMassList(massList).getDataPoints()).forEach(
          dp -> allDps
              .add(new MobilityDataPoint(dp.getMZ(), dp.getIntensity(), scan.getMobility(),
                  scan.getScanNumber())));
    }

    // sort by highest dp, we assume that that measurement was the most accurate
    allDps.sort(Comparator.comparingDouble(MobilityDataPoint::getIntensity));

    List<Mobilogram> mobilograms = new ArrayList<>();
    List<MobilityDataPoint> itemsToRemove = new ArrayList<>();

    for (int i = 0; i < allDps.size(); i++) {
      final MobilityDataPoint baseDp = allDps.get(i);
      final double baseMz = baseDp.getMZ();
      allDps.remove(baseDp);
      i--; // item removed

      final SimpleMobilogram mobilogram = new SimpleMobilogram(mobilityType);
      mobilogram.addDataPoint(baseDp);


      for (MobilityDataPoint dp : allDps) {
        if (mzTolerance.checkWithinTolerance(baseMz, dp.getMZ())
            && !mobilogram.containsDpForScan(dp.getScanNum())) {
          mobilogram.addDataPoint(dp);
          itemsToRemove.add(dp);
        }
      }

      allDps.removeAll(itemsToRemove);
      itemsToRemove.clear();

      if (mobilogram.getDataPoints().size() > 1) {
        mobilogram.calc();
        mobilograms.add(mobilogram);
      }

    }

    mobilograms.sort(Comparator.comparingDouble(Mobilogram::getMZ));
    return mobilograms;
  }
}
