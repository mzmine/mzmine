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
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.listeners.MassListChangedListener;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging.MergingType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a merged spectrum from scans of the same raw data file. If a merged spectrum across
 * multiple raw data files is needed, implementations have to check for compatibility.
 */
public class SimpleMergedMsMsSpectrum extends AbstractStorableSpectrum implements
    MergedMsMsSpectrum {

  private static final Logger logger = Logger.getLogger(SimpleMergedMsMsSpectrum.class.getName());

  private final List<MassSpectrum> sourceSpectra;
  private final MergingType mergingType;
  private final CenterFunction centerFunction;
  private final float collisionEnergy;
  private final RawDataFile rawDataFile;
  private final float retentionTime;
  private final double precursorMz;
  private final int msLevel;
  private final Range<Double> scanningMzRange;
  private final PolarityType polarity;
  private MassList massList = null;
  private final String scanDefinition;
  private List<MassListChangedListener> massListListener;

  public SimpleMergedMsMsSpectrum(@Nonnull MemoryMapStorage storage, @Nonnull double[] mzValues,
      @Nonnull double[] intensityValues, double precursorMz,
      float collisionEnergy, int msLevel, @Nonnull List<? extends MassSpectrum> sourceSpectra,
      @Nonnull MergingType mergingType, @Nonnull CenterFunction centerFunction) {
    super(storage, mzValues, intensityValues);

    assert !sourceSpectra.isEmpty();

    RawDataFile file = null;
    PolarityType tempPolarity = null;
    Range<Double> tempScanningMzRange = null;
    float tempRt = 0f;
    for (MassSpectrum spectrum : sourceSpectra) {
      if (file == null) {
        if (spectrum instanceof Scan) {
          file = ((Scan) spectrum).getDataFile();
          tempPolarity = ((Scan) spectrum).getPolarity();
          tempScanningMzRange = ((Scan) spectrum).getScanningMZRange();
          tempRt = ((Scan) spectrum).getRetentionTime();
        } else if (spectrum instanceof MobilityScan) {
          file = ((MobilityScan) spectrum).getDataFile();
          tempPolarity = ((MobilityScan) spectrum).getFrame().getPolarity();
          tempScanningMzRange = ((MobilityScan) spectrum).getFrame()
              .getScanningMZRange();
          tempRt = ((MobilityScan) spectrum).getRetentionTime();
        }
      }
      if (spectrum instanceof Scan) {
        if (file != ((Scan) spectrum).getDataFile()) {
          logger.warning("Merging spectra with different raw data files");
        }
      } else if (spectrum instanceof MobilityScan) {
        if (file != ((MobilityScan) spectrum).getDataFile()) {
          logger.warning("Merging spectra with different raw data files");
        }
      }
    }
    rawDataFile = file;

    this.retentionTime = tempRt;
    this.polarity = tempPolarity;
    this.scanningMzRange = tempScanningMzRange;
    this.sourceSpectra = (List<MassSpectrum>) sourceSpectra;
    this.mergingType = mergingType;
    this.centerFunction = centerFunction;
    this.collisionEnergy = collisionEnergy;
    this.precursorMz = precursorMz;
    this.msLevel = msLevel;
    this.scanDefinition = ScanUtils.scanToString(this, true);
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
  public float getCollisionEnergy() {
    return collisionEnergy;
  }

  @Nonnull
  @Override
  public RawDataFile getDataFile() {
    return rawDataFile;
  }

  @Override
  public int getScanNumber() {
    return -1;
  }

  @Nonnull
  @Override
  public String getScanDefinition() {
    return scanDefinition;
  }

  @Override
  public int getMSLevel() {
    return msLevel;
  }

  @Override
  public float getRetentionTime() {
    return retentionTime;
  }

  @Nonnull
  @Override
  public Range<Double> getScanningMZRange() {
    return scanningMzRange;
  }

  @Override
  public double getPrecursorMZ() {
    return precursorMz;
  }

  @Nonnull
  @Override
  public PolarityType getPolarity() {
    return polarity;
  }

  @Override
  public int getPrecursorCharge() {
    return 0;
  }

  @Nullable
  @Override
  public MassList getMassList() {
    return massList;
  }


  @Override
  public synchronized void addMassList(final @Nonnull MassList massList) {
    // we are not going into any details if this.massList equals massList
    // do not call listeners if the same object is passed multiple times
    if (this.massList == massList) {
      return;
    }
    MassList old = this.massList;
    this.massList = massList;

    if (massListListener != null) {
      for (MassListChangedListener l : massListListener) {
        l.changed(this, old, massList);
      }
    }
  }

  @Override
  public void addChangeListener(MassListChangedListener listener) {
    if (massListListener == null) {
      massListListener = new ArrayList<>();
    }
    massListListener.add(listener);
  }

  @Override
  public void removeChangeListener(MassListChangedListener listener) {
    if (massListListener != null) {
      massListListener.remove(listener);
    }
  }

  @Override
  public void clearChangeListener() {
    if (massListListener != null) {
      massListListener.clear();
    }
  }
}
