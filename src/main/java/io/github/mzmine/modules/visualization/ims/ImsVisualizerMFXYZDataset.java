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
  ArrayList<Double> mzValues;
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
    mzValues = new ArrayList<>();
    intensity = new ArrayList<>();
    for (int i = 0; i + 1 < scans.length; i++) {
      if (scans[i].getRetentionTime() == scans[i + 1].getRetentionTime()) {
        mobility.add(scans[i].getMobility());
        // Take value in only selected mz range.
        DataPoint dataPoint[] = scans[i].getDataPointsByMass(mzRange);
         double intensitySum = 0;
        for ( int j = 0; j < dataPoint.length; j++ )
        {
          mzValues.add(dataPoint[j].getMZ());
          intensitySum += dataPoint[j].getIntensity();
        }

        intensity.add(intensitySum);

      }
    }

     yValues = new double[mobility.size()];
     zValues = new double[ intensity.size() ];
    for (int i = 0; i < mobility.size(); i++) {
      yValues[i] = mobility.get(i);
      zValues[i] = intensity.get(i);
    }

    xValues = new double[ mzValues.size() ];

    for ( int i = 0; i < xValues.length; i++ )
    {
      xValues[i] = mzValues.get(i);
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
