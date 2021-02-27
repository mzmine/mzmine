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

package io.github.mzmine.datamodel.data_access;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.FeatureDataType;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonSeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.util.MemoryMapStorage;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobilogramDataAccess implements IonMobilitySeries, Iterator<IonMobilitySeries> {

  protected final IonMobilogramTimeSeries imts;
  protected final FeatureDataType accessType;
  protected final List<MobilityScan> currentSpectra;
  protected final int numMobilograms;
  protected final int maxMobilogramDataPoints;
  protected final double[] currentMzs;
  protected final double[] currentIntensities;
  protected final double[] detectedIntensities;
  protected final double[] detectedMzs;
  protected int currentMobilogramIndex;
  protected int currentNumDataPoints;
  protected IonMobilitySeries currentMobilogram;

  protected MobilogramDataAccess(@Nonnull final IonMobilogramTimeSeries imts, @Nonnull
      FeatureDataType accessType) {
    assert imts.getNumberOfValues() > 0;

    this.imts = imts;
    this.accessType = accessType;
    if (accessType == FeatureDataType.INCLUDE_ZEROS) {
      maxMobilogramDataPoints = imts.getSpectra().stream().mapToInt(Frame::getNumberOfMobilityScans)
          .max().getAsInt();

      final int maxNumDetected = imts.getMobilograms().stream()
          .mapToInt(IonMobilitySeries::getNumberOfValues).max().getAsInt();
      detectedMzs = new double[maxNumDetected];
      detectedIntensities = new double[maxNumDetected];
    } else {
      maxMobilogramDataPoints = imts.getMobilograms().stream()
          .mapToInt(IonMobilitySeries::getNumberOfValues).max().getAsInt();
      detectedMzs = new double[maxMobilogramDataPoints];
      detectedIntensities = new double[maxMobilogramDataPoints];
    }

    currentSpectra = new ArrayList<>();
    numMobilograms = imts.getNumberOfValues();
    currentMzs = new double[maxMobilogramDataPoints];
    currentIntensities = new double[maxMobilogramDataPoints];
    currentMobilogramIndex = -1;
  }

  @Override
  public boolean hasNext() {
    return currentMobilogramIndex + 1 < numMobilograms;
  }

  @Override
  public IonMobilitySeries next() {
    if (!hasNext()) {
      throw new NoSuchElementException("Iterator has no next element");
    }

    currentMobilogramIndex++;
    currentMobilogram = imts.getMobilogram(currentMobilogramIndex);
    currentSpectra.clear();

    final Frame currentFrame = imts.getSpectrum(currentMobilogramIndex);
    if (accessType == FeatureDataType.ONLY_DETECTED) {
      currentSpectra.addAll(currentMobilogram.getSpectra());
      currentMobilogram.getIntensityValues(currentIntensities);
      currentMobilogram.getMzValues(currentMzs);
      currentNumDataPoints = currentMobilogram.getNumberOfValues();
    } else {
      currentSpectra.addAll(currentFrame.getMobilityScans());
      currentMobilogram.getMzValues(detectedMzs);
      currentMobilogram.getIntensityValues(detectedIntensities);

      int currentSpectrumIndex = 0;
      int dpIndex = 0;
      for (MobilityScan mobilityScan : currentSpectra) {
        if (mobilityScan == currentMobilogram.getSpectrum(currentSpectrumIndex)) {
          currentIntensities[dpIndex] = detectedIntensities[currentSpectrumIndex];
          currentMzs[dpIndex] = detectedMzs[currentSpectrumIndex];
          currentSpectrumIndex++;
        } else {
          currentIntensities[dpIndex] = 0d;
          currentMzs[dpIndex] = 0d;
        }
        dpIndex++;
      }
    }

    return this;
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    throw new IllegalArgumentException(
        "MobilogramDataAccess shall be used to iterate over the mzs and intensities.");
  }

  @Override
  public IonSeries copy(MemoryMapStorage storage) {
    return currentMobilogram.copy(storage);
  }

  @Override
  public DoubleBuffer getMZValues() {
    throw new IllegalArgumentException(
        "MobilogramDataAccess shall be used to iterate over the mzs and intensities.");
  }

  @Override
  public double[] getIntensityValues(double[] dst) {
    assert dst.length >= currentNumDataPoints;
    System.arraycopy(currentIntensities, 0, dst, 0, currentNumDataPoints);
    return dst;
  }

  @Override
  public double getIntensity(int index) {
    assert index <= currentNumDataPoints;
    return currentIntensities[index];
  }

  @Override
  public double getIntensityForSpectrum(MobilityScan scan) {
    throw new IllegalArgumentException(
        "MobilogramDataAccess shall be used to iterate over the mzs and intensities.");
  }

  @Override
  public double getMzForSpectrum(MobilityScan scan) {
    throw new IllegalArgumentException(
        "MobilogramDataAccess shall be used to iterate over the mzs and intensities.");
  }

  @Override
  public int getNumberOfValues() {
    return currentNumDataPoints;
  }

  @Override
  public MobilityScan getSpectrum(int index) {
    assert index < currentNumDataPoints;
    return currentMobilogram.getSpectrum(index);
  }

  @Override
  public double[] getMzValues(double[] dst) {
    assert dst.length >= currentNumDataPoints;
    System.arraycopy(currentIntensities, 0, dst, 0, currentNumDataPoints);
    return dst;
  }

  @Override
  public double getMZ(int index) {
    assert index <= currentNumDataPoints;
    return currentMzs[index];
  }

  @Override
  public List<MobilityScan> getSpectra() {
    return Collections.unmodifiableList(currentSpectra);
  }

  @Override
  public IonSpectrumSeries<MobilityScan> subSeries(@Nullable MemoryMapStorage storage,
      @Nonnull List<MobilityScan> subset) {
    throw new IllegalArgumentException(
        "MobilogramDataAccess shall be used to iterate over the mzs and intensities.");
  }

  @Override
  public IonSpectrumSeries<MobilityScan> copyAndReplace(@Nullable MemoryMapStorage storage,
      @Nonnull double[] newMzValues, @Nonnull double[] newIntensityValues) {
    throw new IllegalArgumentException(
        "MobilogramDataAccess shall be used to iterate over the mzs and intensities.");
  }
}
