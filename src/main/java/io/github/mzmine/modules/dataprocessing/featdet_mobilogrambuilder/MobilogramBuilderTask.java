package io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MobilogramBuilderTask extends AbstractTask {

  private final List<Scan> scans;
  private final MZTolerance mzTolerance;
  private final String massList;

  public MobilogramBuilderTask(List<Scan> scans, ParameterSet parameters) {
    this.scans = scans;
    this.mzTolerance = parameters.getParameter(MobilogramBuilderParameters.mzTolerance).getValue();
    this.massList = parameters.getParameter(MobilogramBuilderParameters.massList).getValue();
  }

  @Override
  public String getTaskDescription() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {

    if (scans.size() == 0 || scans.get(0).getMassList(massList) == null) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    final MobilityType mobilityType = scans.get(0).getMobilityType();
    int numDp = 0;

    for (Scan scan : scans) {
        numDp += scan.getMassList(massList).getDataPoints().length;
    }
    final List<MobilityDataPoint> allDps = new ArrayList<>(numDp);

    for (Scan scan : scans) {
      Arrays.stream(scan.getDataPoints()).forEach(
          dp -> allDps.add(new MobilityDataPoint(dp.getMZ(), dp.getIntensity(), scan.getMobility(),
              scan.getScanNumber())));
    }

    // sort by highest dp, we assume that that measurement was the most accurate
    allDps.sort(Comparator.comparingDouble(MobilityDataPoint::getIntensity));
    List<Mobilogram> mobilograms = new ArrayList<>();

    for (MobilityDataPoint baseDp : allDps) {
      final double baseMz = baseDp.getMZ();
      SimpleMobilogram mobilogram = new SimpleMobilogram(mobilityType);
      mobilogram.addDataPoint(baseDp);
      allDps.remove(baseDp);

      for (MobilityDataPoint dp : allDps) {
        if (mzTolerance.checkWithinTolerance(baseMz, dp.getMZ())
            && !mobilogram.containsDpForScan(dp.getScanNum())) {
          mobilogram.addDataPoint(dp);
          allDps.remove(dp);
        }
      }

      if(mobilogram.getDataPoints().size() > 1) {
        mobilogram.calc();
        mobilograms.add(mobilogram);
      }

    }

  }
}
