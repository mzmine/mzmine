package io.github.mzmine.modules.visualization.ims.imsVisualizer;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerParameters;
import io.github.mzmine.parameters.ParameterSet;
import org.jfree.data.xy.AbstractXYZDataset;

import java.util.ArrayList;

public class MobilityFrameXYZDataset extends AbstractXYZDataset {

  private RawDataFile dataFiles[];
  private Scan scans[];
  private Range<Double> mzRange;
  ArrayList<Double> mobility;
  ArrayList<Double> mzValues;
  ArrayList<Double> intensity;
  private Double[] xValues;
  private Double[] yValues;
  private Double[] zValues;
  private Double selectedRetentionTime;
  private int itemSize;

  public MobilityFrameXYZDataset(ParameterSet parameters, double retentionTime) {
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

    selectedRetentionTime = retentionTime;

    mobility = new ArrayList<>();
    mzValues = new ArrayList<>();
    intensity = new ArrayList<>();
    double maxIntensity = -1;
    if (selectedRetentionTime == -1) {
      for (int i = 0; i < scans.length; i++) {

        DataPoint dataPoint[] = scans[i].getDataPointsByMass(mzRange);
        double intensitySum = 0;
        for (int j = 0; j < dataPoint.length; j++) {
          intensitySum += dataPoint[j].getIntensity();
        }

        if (maxIntensity < intensitySum) {
          maxIntensity = intensitySum;
          selectedRetentionTime = scans[i].getRetentionTime();
        }
      }
    }

    for (int i = 0; i < scans.length; i++) {
      if (scans[i].getRetentionTime() == selectedRetentionTime) {
        DataPoint dataPoint[] = scans[i].getDataPointsByMass(mzRange);

        for (int j = 0; j < dataPoint.length; j++) {
          mobility.add(scans[i].getMobility());
          mzValues.add(dataPoint[j].getMZ());
          intensity.add(dataPoint[j].getIntensity());
        }
      }
    }

    itemSize = mobility.size();
    xValues = new Double[itemSize];
    yValues = new Double[itemSize];
    zValues = new Double[itemSize];
    xValues = mzValues.toArray(new Double[itemSize]);
    yValues = mobility.toArray(new Double[itemSize]);
    zValues = intensity.toArray(new Double[itemSize]);
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
    return itemSize;
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
