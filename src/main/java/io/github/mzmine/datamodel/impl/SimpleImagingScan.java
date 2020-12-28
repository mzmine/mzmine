package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.Coordinates;


public class SimpleImagingScan extends SimpleScan implements ImagingScan {

  private Coordinates coordinates;

  public SimpleImagingScan(RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
      double precursorMZ, int precursorCharge, DataPoint[] dataPoints,
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, Coordinates coordinates) {
    super(dataFile, scanNumber, msLevel, retentionTime, precursorMZ, precursorCharge, dataPoints,
        spectrumType, polarity, scanDefinition, scanMZRange);
    this.setCoordinates(coordinates);
  }

  public SimpleImagingScan(Scan sc) {
    super(sc);
  }


  /**
   * 
   * @return the xyz coordinates. null if no coordinates were specified
   */
  @Override
  public Coordinates getCoordinates() {
    return coordinates;
  }

  @Override
  public void setCoordinates(Coordinates coordinates) {
    this.coordinates = coordinates;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((coordinates == null) ? 0 : coordinates.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SimpleImagingScan other = (SimpleImagingScan) obj;
    if (coordinates == null) {
      if (other.coordinates != null)
        return false;
    } else if (!coordinates.equals(other.coordinates))
      return false;
    return true;
  }

}
