/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.io.import_rawdata_imzml.Coordinates;


public class SimpleImagingScan extends SimpleScan implements ImagingScan {

  private Coordinates coordinates;

  public SimpleImagingScan(RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
      double precursorMZ, int precursorCharge, double mzValues[], double intensityValues[],
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, Coordinates coordinates) {
    super(dataFile, scanNumber, msLevel, retentionTime,
        null, mzValues, intensityValues, spectrumType, polarity, scanDefinition,
        scanMZRange);
    if(Double.compare(precursorMZ, 0d) != 0) {
      setMsMsInfo(new DDAMsMsInfoImpl(precursorMZ, precursorCharge != 0 ? precursorCharge : null, null, this,
          null, msLevel, null, null));
    }

    this.setCoordinates(coordinates);
  }

  /**
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
