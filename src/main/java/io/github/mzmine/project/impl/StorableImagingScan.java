/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.project.impl;

import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleImagingScan;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.Coordinates;

/**
 * Implementation of the Scan interface which stores raw data points in a temporary file, accessed
 * by RawDataFileImpl.readFromFloatBufferFile()
 */
public class StorableImagingScan extends StorableScan implements ImagingScan {

  private Coordinates coordinates;

  /**
   * Constructor for creating a storable scan from a given scan
   */
  public StorableImagingScan(Scan originalScan, RawDataFile rawDataFile, int numberOfDataPoints,
      int storageID) {
    super(originalScan, rawDataFile, numberOfDataPoints, storageID);
    if (SimpleImagingScan.class.isInstance(originalScan)) {
      this.setCoordinates(((SimpleImagingScan) originalScan).getCoordinates());
    }
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
}
