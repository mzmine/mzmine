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
import io.github.mzmine.datamodel.*;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple implementation of the Scan interface.
 */
public class PasefScan extends SimpleScan {

  double[] mobilities;

  public PasefScan(@NotNull RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
                    @Nullable MsMsInfo msMsInfo, double[] mzValues, double[] intensityValues,
                    MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
                    Range<Double> scanMZRange) {

    super(dataFile, scanNumber, msLevel, retentionTime, msMsInfo, mzValues, intensityValues, spectrumType, polarity, scanDefinition, scanMZRange);
  }

  public void setMobilities(double[] mobilities){
    this.mobilities = mobilities;
  }
}

