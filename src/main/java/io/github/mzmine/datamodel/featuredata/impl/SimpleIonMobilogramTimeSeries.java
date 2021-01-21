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

package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Used to store ion mobility-LC-MS data.
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleIonMobilogramTimeSeries implements IonMobilogramTimeSeries {

  private static final Logger logger = Logger.getLogger(SimpleIonTimeSeries.class.getName());

  protected final List<SimpleIonMobilitySeries> mobilograms;
  protected final List<Frame> frames;
  protected final DoubleBuffer intensityValues;
  protected final DoubleBuffer mzValues;
  protected final SummedIntensityMobilitySeries summedMobilogram;
  protected final boolean forceStoreInRam;

  /**
   * Stores a list of mobilograms. A summed intensity of each mobilogram is automatically calculated
   * and represents this series when plotted as a 2D intensity-vs time chart (accessed via {@link
   * SimpleIonMobilogramTimeSeries#getMZ(int)} and {@link SimpleIonMobilogramTimeSeries#getIntensity(int)}).
   * The mz representing a mobilogram is calculated by a weighted average based on the mzs in eah
   * mobility scan.
   *
   * @param storage
   * @param mobilograms
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[])
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[], List)
   */
  public SimpleIonMobilogramTimeSeries(@Nonnull MemoryMapStorage storage,
      @Nonnull List<SimpleIonMobilitySeries> mobilograms) {
    this(storage, mobilograms, false);
  }

  /**
   * Stores a list of mobilograms. A summed intensity of each mobilogram is automatically calculated
   * and represents this series when plotted as a 2D intensity-vs time chart (accessed via {@link
   * SimpleIonMobilogramTimeSeries#getMZ(int)} and {@link SimpleIonMobilogramTimeSeries#getIntensity(int)}).
   * The mz representing a mobilogram is calculated by a weighted average based on the mzs in eah
   * mobility scan.
   *
   * @param storage         May be null if forceStoreInRam is true
   * @param mobilograms
   * @param forceStoreInRam Forces storage of mz and intensity values in ram. Note that all series
   *                        created as subset or copy from this series will also be stored in ram.
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[])
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[], List)
   */
  public SimpleIonMobilogramTimeSeries(@Nullable MemoryMapStorage storage,
      @Nonnull List<SimpleIonMobilitySeries> mobilograms, boolean forceStoreInRam) {
    if (storage == null && !forceStoreInRam) {
      throw new IllegalArgumentException("MemoryMapStorage is null, cannot store data.");
    }
    if (!checkRawFileIntegrity(mobilograms)) {
      throw new IllegalArgumentException("Cannot combine mobilograms of different raw data files.");
    }

    frames = new ArrayList<>(mobilograms.size());
    this.mobilograms = mobilograms;
    this.forceStoreInRam = forceStoreInRam;

    for (SimpleIonMobilitySeries ims : mobilograms) {
      final Frame frame = ims.getSpectra().get(0).getFrame();
      frames.add(frame);
    }

    double[] summedIntensities = sumIntensities(mobilograms);
    double[] weightedMzs = weightMzs(mobilograms, summedIntensities);

    summedMobilogram = new SummedIntensityMobilitySeries(storage,
        mobilograms, weightedMzs[((int) weightedMzs.length / 2)]);

    DoubleBuffer tempMzs;
    DoubleBuffer tempIntensities;

    if (!forceStoreInRam) {
      try {
        tempMzs = storage.storeData(weightedMzs);
        tempIntensities = storage.storeData(summedIntensities);
      } catch (IOException e) {
        e.printStackTrace();
        tempMzs = DoubleBuffer.wrap(weightedMzs);
        tempIntensities = DoubleBuffer.wrap(summedIntensities);
      }
    } else {
      tempMzs = DoubleBuffer.wrap(weightedMzs);
      tempIntensities = DoubleBuffer.wrap(summedIntensities);
    }
    mzValues = tempMzs;
    intensityValues = tempIntensities;
  }

  /**
   * Constructor with custom mzs and intensities. Use with care. After feature creation, {@link
   * SimpleIonMobilogramTimeSeries#SimpleIonMobilogramTimeSeries(MemoryMapStorage, List)} might be
   * more applicable, to automatically calculate summed intensities for each mobilogram. This
   * constructor is meant to be used, when intensities have been altered, e.g. by smoothing.
   *
   * @param storage
   * @param mzs
   * @param intensities
   * @param mobilograms
   * @param frames
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[])
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[], List)
   */
  private SimpleIonMobilogramTimeSeries(@Nonnull MemoryMapStorage storage, @Nonnull double[] mzs,
      @Nonnull double[] intensities, List<SimpleIonMobilitySeries> mobilograms,
      List<Frame> frames, boolean forceStoreInRam) {
    if (mzs.length != intensities.length || mobilograms.size() != intensities.length
        || mzs.length != mobilograms.size()) {
      throw new IllegalArgumentException(
          "Length of mz, intensity, frames and/or mobilograms does not match.");
    }
    if (!checkRawFileIntegrity(mobilograms)) {
      throw new IllegalArgumentException("Cannot combine mobilograms of different raw data files.");
    }

    this.mobilograms = mobilograms;
    this.frames = frames;
    this.forceStoreInRam = forceStoreInRam;

    summedMobilogram = new SummedIntensityMobilitySeries(storage,
        mobilograms, mzs[((int) mzs.length / 2)]);

    DoubleBuffer tempMzs;
    DoubleBuffer tempIntensities;
    try {
      tempMzs = storage.storeData(mzs);
      tempIntensities = storage.storeData(intensities);
    } catch (IOException e) {
      e.printStackTrace();
      tempMzs = DoubleBuffer.wrap(mzs);
      tempIntensities = DoubleBuffer.wrap(intensities);
    }
    mzValues = tempMzs;
    intensityValues = tempIntensities;
  }

  /**
   * Private copy constructor.
   *
   * @param storage
   * @param series
   * @param frames  to be passed directly, since getSpectra wraps in a unmodifiable list. ->
   *                wrapping over and over
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[])
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[], List)
   */
  private SimpleIonMobilogramTimeSeries(@Nonnull MemoryMapStorage storage,
      @Nonnull IonMobilogramTimeSeries series, List<Frame> frames, boolean forceStoreInRam) {
    this.frames = frames;
    this.mobilograms = new ArrayList<>();
    this.forceStoreInRam = forceStoreInRam;
    series.getMobilograms().forEach(m -> mobilograms.add(
        (SimpleIonMobilitySeries) m.copy(storage)));

    double[][] data = DataPointUtils
        .getDataPointsAsDoubleArray(series.getMZValues(), series.getIntensityValues());

    summedMobilogram = new SummedIntensityMobilitySeries(storage,
        mobilograms, data[0][((int) data[0].length / 2)]);

    DoubleBuffer tempMzs;
    DoubleBuffer tempIntensities;
    try {
      tempMzs = storage.storeData(data[0]);
      tempIntensities = storage.storeData(data[1]);
    } catch (IOException e) {
      e.printStackTrace();
      tempMzs = DoubleBuffer.wrap(data[0]);
      tempIntensities = DoubleBuffer.wrap(data[1]);
    }
    mzValues = tempMzs;
    intensityValues = tempIntensities;
  }

  @Override
  public IonMobilogramTimeSeries subSeries(MemoryMapStorage storage, List<Frame> subset) {
    double[] mzs = new double[subset.size()];
    double[] intensities = new double[subset.size()];

    for (int i = 0; i < subset.size(); i++) {
      mzs[i] = getMzForSpectrum(subset.get(i));
      intensities[i] = getIntensityForSpectrum(subset.get(i));
    }

    List<SimpleIonMobilitySeries> subMobilograms = new ArrayList<>(subset.size());
    for (SimpleIonMobilitySeries mobilogram : mobilograms) {
      if (subset.contains(mobilogram.getSpectrum(0).getFrame())) {
        subMobilograms.add(mobilogram);
      }
    }

    return new SimpleIonMobilogramTimeSeries(storage, mzs, intensities, subMobilograms, subset, forceStoreInRam);
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return intensityValues;
  }

  @Override
  public DoubleBuffer getMZValues() {
    return mzValues;
  }

  /**
   * @return The frames.
   */
  @Override
  public List<Frame> getSpectra() {
    return Collections.unmodifiableList(frames);
  }

  @Override
  public List<SimpleIonMobilitySeries> getMobilograms() {
    return Collections.unmodifiableList(mobilograms);
  }

  @Override
  public IonSpectrumSeries<Frame> copy(MemoryMapStorage storage) {
    return new SimpleIonMobilogramTimeSeries(storage, this, frames, forceStoreInRam);
  }

  @Override
  public SummedIntensityMobilitySeries getSummedMobilogram() {
    return summedMobilogram;
  }

  @Override
  public IonTimeSeries<Frame> copyAndReplace(MemoryMapStorage storage, double[] newMzValues,
      double[] newIntensityValues) {
    return new SimpleIonMobilogramTimeSeries(storage, newMzValues, newIntensityValues,
        this.getMobilograms(), this.frames, forceStoreInRam);
  }

  public IonMobilogramTimeSeries copyAndReplace(MemoryMapStorage storage, double[] newMzValues,
      double[] newIntensityValues, List<SimpleIonMobilitySeries> newMobilograms) {
    return new SimpleIonMobilogramTimeSeries(storage, newMzValues, newIntensityValues,
        newMobilograms, this.frames, forceStoreInRam);
  }

  private double[] weightMzs(List<SimpleIonMobilitySeries> mobilograms,
      double[] summedIntensities) {
    double[] weightedMzs = new double[mobilograms.size()];

    for (int i = 0; i < mobilograms.size(); i++) {
      SimpleIonMobilitySeries ims = mobilograms.get(i);
      DoubleBuffer intensities = ims.getIntensityValues();
      DoubleBuffer mzValues = ims.getMZValues();
      double weightedMz = 0;
      for (int j = 0; j < mzValues.capacity(); j++) {
        weightedMz += mzValues.get(j) * (intensities.get(j) / summedIntensities[i]);
      }
      // due to added zeros, the summed intensity might have been 0 -> NaN
      if (Double.compare(weightedMz, Double.NaN) == 0) {
        weightedMz = 0d;
      }
      weightedMzs[i] = weightedMz;
    }
    return weightedMzs;
  }

  private double[] sumIntensities(List<SimpleIonMobilitySeries> mobilograms) {
    double[] summedIntensities = new double[mobilograms.size()];
    for (int i = 0; i < mobilograms.size(); i++) {
      SimpleIonMobilitySeries ims = mobilograms.get(i);
      DoubleBuffer intensities = ims.getIntensityValues();
      for (int j = 0; j < intensities.capacity(); j++) {
        summedIntensities[i] += intensities.get(j);
      }
    }
    return summedIntensities;
  }

  private boolean checkRawFileIntegrity(List<SimpleIonMobilitySeries> mobilograms) {
    final RawDataFile file = mobilograms.get(0).getSpectrum(0).getDataFile();
    for (SimpleIonMobilitySeries mobilogram : mobilograms) {
      if (mobilogram.getSpectrum(0).getDataFile() != file) {
        return false;
      }
    }
    return true;
  }
}
