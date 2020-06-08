package io.github.mzmine.modules.visualization.ims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import org.jfree.data.xy.AbstractXYDataset;

import java.util.ArrayList;

public class ImsVisualizerXYDataset extends AbstractXYDataset {
  private RawDataFile dataFiles[];
  private Scan scans[];
  private Range<Double> mzRange;
  ArrayList<Double> mobility;
  private double[] xValues;
  private double[] yValues;

  public ImsVisualizerXYDataset(ParameterSet parameters) {

    dataFiles =
        parameters
            .getParameter(ImsVisualizerParameters.dataFiles)
            .getValue()
            .getMatchingRawDataFiles();

    scans =
        parameters
            .getParameter(ImsVisualizerParameters.scanSelection)
            .getValue()
            .getMatchingScans(dataFiles[0]);

    mzRange = parameters.getParameter(ImsVisualizerParameters.mzRange).getValue();

    // Calc xValues retention time
    mobility = new ArrayList<Double>();

    for (int i = 0; i < scans.length; i++) {
      if (i == 0) {
        mobility.add(scans[i].getMobility());
      } else {
        if (scans[i].getMobility() != scans[i - 1].getMobility()) {
          mobility.add(scans[i].getMobility());
        }
      }
    }

    xValues = new double[mobility.size()];
    yValues = new double[mobility.size()];

    for (int i = 0; i < (int) mobility.size(); i++) {
      xValues[i] = mobility.get(i);
    }

    for (int i = 0; i < mobility.size(); i++) {
      for (int k = 0; k < scans.length; k++) {
        if (scans[k].getMobility() == mobility.get(i)) {
          // Take value in only selected mz range.
          DataPoint dataPoint[] = scans[k].getDataPointsByMass(mzRange);

          for (int j = 0; j < dataPoint.length; j++) {
            yValues[i] += dataPoint[j].getIntensity();
          }
        }
      }
    }
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable getSeriesKey(int series) {
    return getRowKey(series);
  }

  public Comparable<?> getRowKey(int item) {
    return scans[item].toString();
  }

  @Override
  public int getItemCount(int series) {
    return mobility.size();
  }

  @Override
  public Number getX(int series, int item) {
    return xValues[item];
  }

  @Override
  public Number getY(int series, int item) {
    return yValues[item];
  }
}
