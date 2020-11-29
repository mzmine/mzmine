package io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder;

import io.github.mzmine.datamodel.DataPoint;

public class MobilityDataPoint implements DataPoint {

  private final double mz;
  private final double intensity;
  private final double mobility;
  private final int scanNum;

  public MobilityDataPoint(double mz, double intensity, double mobility, int scanNum) {
    this.mz = mz;
    this.intensity = intensity;
    this.mobility = mobility;
    this.scanNum = scanNum;
  }

  public double getMobility() {
    return mobility;
  }

  @Override
  public double getMZ() {
    return mz;
  }

  @Override
  public double getIntensity() {
    return intensity;
  }

  public int getScanNum() {
    return scanNum;
  }
}
