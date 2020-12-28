package io.github.mzmine.datamodel;

import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.Coordinates;

public interface ImagingScan {
  /**
   * 
   * @return the xyz coordinates. null if no coordinates were specified
   */
  public Coordinates getCoordinates();

  public void setCoordinates(Coordinates coordinates);
}
