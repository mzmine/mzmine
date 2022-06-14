/*
 *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_imzml.Coordinates;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleImagingFrame extends SimpleFrame implements ImagingFrame {

  private Coordinates coordinates;

  public SimpleImagingFrame(@NotNull RawDataFile dataFile, int scanNumber, int msLevel,
      float retentionTime, double[] mzValues, double[] intensityValues,
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      @NotNull Range<Double> scanMZRange, MobilityType mobilityType,
      @Nullable Set<PasefMsMsInfo> precursorInfos, Float accumulationTime) {
    super(dataFile, scanNumber, msLevel, retentionTime, mzValues, intensityValues, spectrumType,
        polarity, scanDefinition, scanMZRange, mobilityType, precursorInfos, accumulationTime);
  }

  @Nullable
  @Override
  public Coordinates getCoordinates() {
    return coordinates;
  }

  @Override
  public void setCoordinates(@Nullable Coordinates coordinates) {
    this.coordinates = coordinates;
  }
}
