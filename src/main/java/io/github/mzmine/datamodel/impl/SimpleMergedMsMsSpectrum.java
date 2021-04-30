/*
 *  Copyright 2006-2020 The MZmine Development Team
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

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging.MergingType;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a merged spectrum from scans of the same raw data file. If a merged spectrum across
 * multiple raw data files is needed, implementations have to check for compatibility. {@link
 * SimpleMergedMsMsSpectrum#getScanNumber()} will return -1 to represent the artificial state of
 * this spectrum.
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleMergedMsMsSpectrum extends SimpleMergedMassSpectrum implements
    MergedMsMsSpectrum {

  private static final Logger logger = Logger.getLogger(SimpleMergedMsMsSpectrum.class.getName());

  protected final float collisionEnergy;
  protected final double precursorMz;
  protected final int precursorCharge;

  public SimpleMergedMsMsSpectrum(@Nullable MemoryMapStorage storage, @Nonnull double[] mzValues,
      @Nonnull double[] intensityValues, double precursorMz, int precursorCharge,
      float collisionEnergy, int msLevel, @Nonnull List<? extends MassSpectrum> sourceSpectra,
      @Nonnull MergingType mergingType, @Nonnull CenterFunction centerFunction) {
    super(storage, mzValues, intensityValues, msLevel, sourceSpectra, mergingType,
        centerFunction);

    this.precursorMz = precursorMz;
    this.collisionEnergy = collisionEnergy;
    this.precursorCharge = precursorCharge;
    this.scanDefinition = ScanUtils.scanToString(this, true);
  }

  @Override
  public float getCollisionEnergy() {
    return collisionEnergy;
  }

  @Override
  public double getPrecursorMZ() {
    return precursorMz;
  }

  @Override
  public int getPrecursorCharge() {
    return precursorCharge;
  }
}
