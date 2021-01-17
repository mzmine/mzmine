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

    frames = new ArrayList<>(mobilograms.size());
    this.mobilograms = mobilograms;

    double[] summedIntensities = new double[mobilograms.size()];
    double[] weightedMzs = new double[mobilograms.size()];

    // calc summed intensities for each mobilogram
    final RawDataFile file = mobilograms.get(0).getSpectrum(0).getDataFile();
    for (int i = 0; i < mobilograms.size(); i++) {
      SimpleIonMobilitySeries ims = mobilograms.get(i);
      final Frame frame = ims.getSpectra().get(0).getFrame();
      if (frame.getDataFile() != file) {
        throw new IllegalArgumentException(
            "Cannot combine mobilograms of different raw data files.");
      }
      frames.add(frame);

      DoubleBuffer intensities = ims.getIntensityValues();
      DoubleBuffer mzValues = ims.getMZValues();
      for (int j = 0; j < intensities.capacity(); j++) {
        summedIntensities[i] += intensities.get(j);
      }
      // calculate an intensity weighted average for mz
      // todo use CenterFunction maybe?
      double weightedMz = 0;
      for (int j = 0; j < mzValues.capacity(); j++) {
        weightedMz += mzValues.get(j) * (intensities.get(j) / summedIntensities[i]);
      }
      weightedMzs[i] = weightedMz;
    }

    summedMobilogram = new SummedIntensityMobilitySeries(storage,
        mobilograms, weightedMzs[((int) weightedMzs.length / 2)]);

    DoubleBuffer tempMzs;
    DoubleBuffer tempIntensities;
    try {
      tempMzs = storage.storeData(weightedMzs);
      tempIntensities = storage.storeData(summedIntensities);
    } catch (IOException e) {
      e.printStackTrace();
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
      List<Frame> frames) {

    if (mzs.length != intensities.length || mobilograms.size() != intensities.length) {
      throw new IllegalArgumentException(
          "Length of mz, intensity and/or mobilograms does not match.");
    }

    this.mobilograms = mobilograms;
    this.frames = frames;
    final RawDataFile file = mobilograms.get(0).getSpectrum(0).getDataFile();
    for (SimpleIonMobilitySeries mobilogram : mobilograms) {
      if (mobilogram.getSpectrum(0).getDataFile() != file) {
        throw new IllegalArgumentException(
            "Cannot combine mobilograms of different raw data files.");
      }
    }

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
   * @param frames  to be passed directly, sicne getSpectra wraps in a unmodifiable list. ->
   *                wrapping over and over
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[])
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[], List)
   */
  private SimpleIonMobilogramTimeSeries(@Nonnull MemoryMapStorage storage,
      @Nonnull IonMobilogramTimeSeries series, List<Frame> frames) {
    this.frames = frames;

    this.mobilograms = new ArrayList<>();
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
    return new SimpleIonMobilogramTimeSeries(storage, this, frames);
  }

  @Override
  public SummedIntensityMobilitySeries getSummedMobilogram() {
    return summedMobilogram;
  }

  @Override
  public IonTimeSeries<Frame> copyAndReplace(MemoryMapStorage storage, double[] newMzValues,
      double[] newIntensityValues) {
    return new SimpleIonMobilogramTimeSeries(storage, newMzValues, newIntensityValues,
        this.getMobilograms(), this.frames);
  }

  public IonMobilogramTimeSeries copyAndReplace(MemoryMapStorage storage, double[] newMzValues,
      double[] newIntensityValues, List<SimpleIonMobilitySeries> newMobilograms) {
    return new SimpleIonMobilogramTimeSeries(storage, newMzValues, newIntensityValues,
        newMobilograms,
        this.frames);
  }
}
