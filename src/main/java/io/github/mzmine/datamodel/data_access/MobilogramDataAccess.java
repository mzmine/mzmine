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

package io.github.mzmine.datamodel.data_access;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilogramAccessType;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonSeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.util.MemoryMapStorage;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobilogramDataAccess implements IonMobilitySeries, Iterator<IonMobilitySeries> {

  protected final IonMobilogramTimeSeries imts;
  protected final MobilogramAccessType accessType;
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

  protected MobilogramDataAccess(@NotNull final IonMobilogramTimeSeries imts, @NotNull
      MobilogramAccessType accessType) {
    assert imts.getNumberOfValues() > 0;

    this.imts = imts;
    this.accessType = accessType;
    if (accessType == MobilogramAccessType.INCLUDE_ZEROS) {
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
    if (accessType == MobilogramAccessType.ONLY_DETECTED) {
      currentSpectra.addAll(currentMobilogram.getSpectra());
      currentMobilogram.getIntensityValues(currentIntensities);
      currentMobilogram.getMzValues(currentMzs);
      currentNumDataPoints = currentMobilogram.getNumberOfValues();
    } else {
      currentSpectra.addAll(currentFrame.getMobilityScans());
      currentMobilogram.getMzValues(detectedMzs);
      currentMobilogram.getIntensityValues(detectedIntensities);
      final int numValues = currentMobilogram.getNumberOfValues();

      int currentSpectrumIndex = 0;
      int dpIndex = 0;
      for (int i = 0, numSpectra = currentSpectra.size(); i < numSpectra; i++) {
        MobilityScan mobilityScan = currentSpectra.get(i);
        if (mobilityScan == currentMobilogram.getSpectrum(currentSpectrumIndex)) {
          currentIntensities[dpIndex] = detectedIntensities[currentSpectrumIndex];
          currentMzs[dpIndex] = detectedMzs[currentSpectrumIndex];
          currentSpectrumIndex++;
        } else {
          currentIntensities[dpIndex] = 0d;
          currentMzs[dpIndex] = 0d;
        }
        if(currentSpectrumIndex == numValues && i < numSpectra - 1) {
          Arrays.fill(currentIntensities, i + 1, currentIntensities.length, 0d);
          break;
        }
        dpIndex++;
      }
    }

    return currentMobilogram;
  }

  @Override
  public DoubleBuffer getIntensityValueBuffer() {
    throw new IllegalArgumentException(
        "MobilogramDataAccess shall be used to iterate over the mzs and intensities.");
  }

  @Override
  public IonSeries copy(MemoryMapStorage storage) {
    return currentMobilogram.copy(storage);
  }

  @Override
  public DoubleBuffer getMZValueBuffer() {
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
      @NotNull List<MobilityScan> subset) {
    return currentMobilogram.subSeries(storage, subset);
  }

  @Override
  public IonSpectrumSeries<MobilityScan> copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull double[] newMzValues, @NotNull double[] newIntensityValues) {
    // depending on the type of data access, we can have more scans in currentSpectra than data points. (if 0s are included)
    // hence, this method is unsupported. A new SimpleIonMobilitySeries should be created with the respective spectra instead.
    throw new IllegalArgumentException(
        "MobilogramDataAccess shall be used to iterate over the mzs and intensities.");
  }
}
