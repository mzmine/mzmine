package io.github.mzmine.modules.visualization.ims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import org.jfree.data.xy.AbstractXYZDataset;

import java.util.ArrayList;

public class ImsVisualizerMFXYZDataset extends AbstractXYZDataset {

  private RawDataFile dataFiles[];
  private Scan scans[];
  private Range<Double> mzRange;
  ArrayList<Double> mobility;
  ArrayList<Double> mzAverage;
  ArrayList<Double> intensity;
  private double[] xValues;
  private double[] yValues;
  private double[] zValues;
  public ImsVisualizerMFXYZDataset(ParameterSet parameters) {
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

    mobility = new ArrayList<>();
    for (int i = 0; i + 1 < scans.length; i++) {
      if (scans[i].getRetentionTime() == scans[i + 1].getRetentionTime()) {
        mobility.add(scans[i].getMobility());
      }
    }

    xValues = new double[mobility.size()];
    for (int i = 0; i < mobility.size(); i++) {
      xValues[i] = mobility.get(i);
    }

    yValues = new double[mobility.size()];
    zValues = new double[mobility.size()];

    for (int i = 0; i < mobility.size(); i++) {
      for (int k = 0; k < scans.length; k++) {
        if (scans[k].getMobility() == mobility.get(i)) {
          // Take value in only selected mz range.
          DataPoint dataPoint[] = scans[k].getDataPointsByMass(mzRange);
          double mzSum = 0;
          for (int j = 0; j < dataPoint.length; j++) {
            zValues[i] += dataPoint[j].getIntensity();
            mzSum += dataPoint[j].getMZ();
          }
          yValues[i] = dataPoint.length > 0 ? mzSum / dataPoint.length : 0;
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

  @Override
  public Number getZ(int series, int item) {
    return zValues[item];
  }
}
