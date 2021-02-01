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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.scans.SpectraMerging.MergingType;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public class SimpleMergedMsMsSpectrum extends AbstractStorableSpectrum implements
    MergedMsMsSpectrum {

  private final List<MassSpectrum> sourceSpectra;
  private final MergingType mergingType;
  private final CenterFunction centerFunction;
  private final Range<Double> isolationWindow;
  private final float collisionEnergy;
  private final double precursorMz;
  private final int msLevel;

  public SimpleMergedMsMsSpectrum(@Nonnull MemoryMapStorage storage, @Nonnull double[] mzValues,
      @Nonnull double[] intensityValues, double precursorMz, Range<Double> isolationWindow,
      float collisionEnery, int msLevel, @Nonnull List<MassSpectrum> sourceSpectra,
      @Nonnull MergingType mergingType, @Nonnull CenterFunction centerFunction) {
    super(storage, mzValues, intensityValues);

    this.sourceSpectra = sourceSpectra;
    this.mergingType = mergingType;
    this.centerFunction = centerFunction;
    this.isolationWindow = isolationWindow;
    this.collisionEnergy = collisionEnery;
    this.precursorMz = precursorMz;
    this.msLevel = msLevel;
  }

  @Override
  public List<MassSpectrum> getSourceSpectra() {
    return Collections.unmodifiableList(sourceSpectra);
  }

  @Override
  public MergingType getMergingType() {
    return mergingType;
  }

  @Override
  public CenterFunction getCenterFunction() {
    return centerFunction;
  }

  @Override
  public double getPrecursorMz() {
    return precursorMz;
  }

  @Override
  public Range<Double> getIsolationWindow() {
    return isolationWindow;
  }

  @Override
  public double getCollisionEnergy() {
    return collisionEnergy;
  }

  @Override
  public int getMsLevel() {
    return msLevel;
  }
}
