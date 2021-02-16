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
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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

  private static final CenterFunction mzCentering = new CenterFunction(CenterMeasure.AVG,
      Weighting.logger10, 0d, 4);

  protected final List<IonMobilitySeries> mobilograms;
  protected final List<Frame> frames;
  protected final DoubleBuffer intensityValues;
  protected final DoubleBuffer mzValues;
  protected final SummedIntensityMobilitySeries summedMobilogram;
  protected DoubleBuffer mobilogramMzValues;
  protected DoubleBuffer mobilogramIntensityValues;

  /**
   * Stores a list of mobilograms. A summed intensity of each mobilogram is automatically calculated
   * and represents this series when plotted as a 2D intensity-vs time chart (accessed via {@link
   * SimpleIonMobilogramTimeSeries#getMZ(int)} and {@link SimpleIonMobilogramTimeSeries#getIntensity(int)}).
   * The mz representing a mobilogram is calculated by a weighted average based on the mzs in eah
   * mobility scan.
   *
   * @param storage     May be null if values shall be stored in ram.
   * @param mobilograms
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[])
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[], List,
   * double[])
   */
  public SimpleIonMobilogramTimeSeries(@Nullable MemoryMapStorage storage,
      @Nonnull List<IonMobilitySeries> mobilograms) {
    if (!checkRawFileIntegrity(mobilograms)) {
      throw new IllegalArgumentException("Cannot combine mobilograms of different raw data files.");
    }

    frames = new ArrayList<>(mobilograms.size());
    this.mobilograms = storeMobilograms(storage, mobilograms);

    for (IonMobilitySeries ims : mobilograms) {
      final Frame frame = ims.getSpectra().get(0).getFrame();
      frames.add(frame);
    }

    double[] summedIntensities = sumIntensities(mobilograms);
    double[] weightedMzs = weightMzs(mobilograms, summedIntensities);
    final double mz = Arrays.stream(weightedMzs).filter(val -> Double.compare(val, 0d) != 0)
        .average().getAsDouble();

    summedMobilogram = new SummedIntensityMobilitySeries(storage,
        mobilograms, mz);

    mzValues = StorageUtils.storeValuesToDoubleBuffer(storage, weightedMzs);
    intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, summedIntensities);
  }

  /**
   * Constructor with custom mzs and intensities. Use with care. After feature creation, {@link
   * SimpleIonMobilogramTimeSeries#SimpleIonMobilogramTimeSeries(MemoryMapStorage, List)} might be
   * more applicable, to automatically calculate summed intensities for each mobilogram. This
   * constructor is meant to be used, when intensities have been altered, e.g. by smoothing.
   *
   * @param storage     May be null if values shall be stored in ram.
   * @param mzs
   * @param intensities
   * @param mobilograms
   * @param frames
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[])
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[], List,
   * double[])
   */
  private SimpleIonMobilogramTimeSeries(@Nullable MemoryMapStorage storage, @Nonnull double[] mzs,
      @Nonnull double[] intensities, List<IonMobilitySeries> mobilograms,
      List<Frame> frames) {
    if (mzs.length != intensities.length || mobilograms.size() != intensities.length
        || mzs.length != mobilograms.size()) {
      throw new IllegalArgumentException(
          "Length of mz, intensity, frames and/or mobilograms does not match.");
    }
    if (!checkRawFileIntegrity(mobilograms)) {
      throw new IllegalArgumentException("Cannot combine mobilograms of different raw data files.");
    }

    this.mobilograms = storeMobilograms(storage, mobilograms);
    this.frames = frames;

    summedMobilogram = new SummedIntensityMobilitySeries(storage,
        mobilograms, mzs[((int) mzs.length / 2)]);

    mzValues = StorageUtils.storeValuesToDoubleBuffer(storage, mzs);
    intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, intensities);
  }

  /**
   * Constructor with custom mzs and intensities. Use with care. After feature creation, {@link
   * SimpleIonMobilogramTimeSeries#SimpleIonMobilogramTimeSeries(MemoryMapStorage, List)} might be
   * more applicable, to automatically calculate summed intensities for each mobilogram. This
   * constructor is meant to be used, when intensities have been altered, e.g. by smoothing.
   *
   * @param storage                             May be null if values shall be stored in ram.
   * @param mzs
   * @param intensities
   * @param mobilograms
   * @param frames
   * @param summedMobilogramMobilitities
   * @param smoothedSummedMobilogramIntensities
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[])
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[], List,
   * double[])
   */
  private SimpleIonMobilogramTimeSeries(@Nullable MemoryMapStorage storage, @Nonnull double[] mzs,
      @Nonnull double[] intensities, @Nonnull List<IonMobilitySeries> mobilograms,
      @Nonnull List<Frame> frames, @Nullable double[] summedMobilogramMobilitities,
      @Nullable double[] smoothedSummedMobilogramIntensities) {

    if (mzs.length != intensities.length || mobilograms.size() != intensities.length
        || mzs.length != mobilograms.size()) {
      throw new IllegalArgumentException(
          "Length of mz, intensity, frames and/or mobilograms does not match.");
    }
    if (!checkRawFileIntegrity(mobilograms)) {
      throw new IllegalArgumentException("Cannot combine mobilograms of different raw data files.");
    }

    this.mobilograms = storeMobilograms(storage, mobilograms);
    this.frames = frames;

    final double mz = Arrays.stream(mzs).filter(val -> Double.compare(val, 0d) != 0).average()
        .getAsDouble();

    if (smoothedSummedMobilogramIntensities != null && summedMobilogramMobilitities != null) {
      summedMobilogram = new SummedIntensityMobilitySeries(storage, summedMobilogramMobilitities,
          smoothedSummedMobilogramIntensities, mz);
    } else {
      summedMobilogram = new SummedIntensityMobilitySeries(storage, mobilograms, mz);
    }

    mzValues = StorageUtils.storeValuesToDoubleBuffer(storage, mzs);
    intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, intensities);
  }

  /**
   * Private copy constructor.
   *
   * @param storage May be null if values shall be stored in ram.
   * @param series
   * @param frames  to be passed directly, since getSpectra wraps in a unmodifiable list. ->
   *                wrapping over and over
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[])
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[], List,
   * double[])
   */
  private SimpleIonMobilogramTimeSeries(@Nullable MemoryMapStorage storage,
      @Nonnull IonMobilogramTimeSeries series, List<Frame> frames) {
    this.frames = frames;
    this.mobilograms = storeMobilograms(storage, series.getMobilograms());

    double[][] data = DataPointUtils
        .getDataPointsAsDoubleArray(series.getMZValues(), series.getIntensityValues());

    final double mz = Arrays.stream(data[0]).filter(val -> Double.compare(val, 0d) != 0).average()
        .getAsDouble();

    summedMobilogram = new SummedIntensityMobilitySeries(storage,
        mobilograms, mz);

    mzValues = StorageUtils.storeValuesToDoubleBuffer(storage, data[0]);
    intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, data[1]);
  }

  @Override
  public IonMobilogramTimeSeries subSeries(@Nullable MemoryMapStorage storage,
      @Nonnull List<Frame> subset) {
    double[] mzs = new double[subset.size()];
    double[] intensities = new double[subset.size()];

    for (int i = 0; i < subset.size(); i++) {
      mzs[i] = getMzForSpectrum(subset.get(i));
      intensities[i] = getIntensityForSpectrum(subset.get(i));
    }

    List<IonMobilitySeries> subMobilograms = new ArrayList<>(subset.size());
    for (IonMobilitySeries mobilogram : mobilograms) {
      if (subset.contains(mobilogram.getSpectrum(0).getFrame())) {
        subMobilograms.add(mobilogram);
      }
    }

    return new SimpleIonMobilogramTimeSeries(storage, mzs, intensities, subMobilograms, subset);
  }

  private List<IonMobilitySeries> storeMobilograms(@Nullable MemoryMapStorage storage,
      List<IonMobilitySeries> mobilograms) {
    int[] offsets = new int[mobilograms.size()];
    DoubleBuffer[] stored = StorageUtils
        .storeIonSeriesToSingleBuffer(storage, mobilograms, offsets);
    mobilogramMzValues = stored[0];
    mobilogramIntensityValues = stored[1];

    List<IonMobilitySeries> storedMobilograms = new ArrayList<>();
    for (int i = 0; i < offsets.length; i++) {
      storedMobilograms.add(new StorableIonMobilitySeries(this, offsets[i],
          mobilograms.get(i).getNumberOfValues(), mobilograms.get(i).getSpectra()));
    }
    return storedMobilograms;
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
  public List<IonMobilitySeries> getMobilograms() {
    return Collections.unmodifiableList(mobilograms);
  }

  @Override
  public IonMobilogramTimeSeries copy(@Nullable MemoryMapStorage storage) {
    return new SimpleIonMobilogramTimeSeries(storage, this, frames);
  }

  @Override
  public SummedIntensityMobilitySeries getSummedMobilogram() {
    return summedMobilogram;
  }

  @Override
  public IonMobilogramTimeSeries copyAndReplace(@Nullable MemoryMapStorage storage,
      @Nonnull double[] newMzValues, @Nonnull double[] newIntensityValues) {
    return new SimpleIonMobilogramTimeSeries(storage, newMzValues, newIntensityValues,
        this.getMobilograms(), this.frames);
  }

  /**
   * Used during smoothing. If smoothedSummedMobilogramIntensities are null, the intensities of the
   * summed mobilogram will be recalculated based on the individual mobilograms.
   *
   * @param storage                             May be null if data shall be stored in ram
   * @param newMzValues
   * @param newIntensityValues
   * @param newMobilograms
   * @param smoothedSummedMobilogramIntensities If the summed mobilogram has been smoothed, the
   *                                            smoothed intensities can be passed here. If null,
   *                                            the previous intensities will be used.
   * @return
   */
  @Override
  public IonMobilogramTimeSeries copyAndReplace(@Nullable MemoryMapStorage storage,
      @Nonnull double[] newMzValues, @Nonnull double[] newIntensityValues,
      @Nonnull List<IonMobilitySeries> newMobilograms,
      @Nullable double[] smoothedSummedMobilogramIntensities) {

    double[] summedMobilogramMobilities = null;
    if (smoothedSummedMobilogramIntensities != null) {
      summedMobilogramMobilities = DataPointUtils
          .getDoubleBufferAsArray(getSummedMobilogram().getMobilityValues());
    }

    return new SimpleIonMobilogramTimeSeries(storage, newMzValues, newIntensityValues,
        newMobilograms, this.frames, summedMobilogramMobilities,
        smoothedSummedMobilogramIntensities);
  }

  private double[] weightMzs(List<IonMobilitySeries> mobilograms,
      double[] summedIntensities) {
    double[] weightedMzs = new double[mobilograms.size()];

    for (int i = 0; i < mobilograms.size(); i++) {
      IonMobilitySeries ims = mobilograms.get(i);
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

  private double[] sumIntensities(List<IonMobilitySeries> mobilograms) {
    double[] summedIntensities = new double[mobilograms.size()];
    for (int i = 0; i < mobilograms.size(); i++) {
      IonMobilitySeries ims = mobilograms.get(i);
      DoubleBuffer intensities = ims.getIntensityValues();
      for (int j = 0; j < intensities.capacity(); j++) {
        summedIntensities[i] += intensities.get(j);
      }
    }
    return summedIntensities;
  }

  private boolean checkRawFileIntegrity(List<IonMobilitySeries> mobilograms) {
    final RawDataFile file = mobilograms.get(0).getSpectrum(0).getDataFile();
    for (IonMobilitySeries mobilogram : mobilograms) {
      if (mobilogram.getSpectrum(0).getDataFile() != file) {
        return false;
      }
    }
    return true;
  }

  protected DoubleBuffer getMobilogramMzValues(
      StorableIonMobilitySeries mobilogram/*, double[] dst*/) {
//    assert mobilogram.getNumberOfValues() <= dst.length;
//    mobilogramMzValues.get(mobilogram.getStorageOffset(), dst, 0, mobilogram.getNumberOfValues());
    double[] values = new double[mobilogram.getNumberOfValues()];
    mobilogramMzValues.get(mobilogram.getStorageOffset(), values, 0, values.length);
    return DoubleBuffer.wrap(values);
  }

  protected DoubleBuffer getMobilogramIntensityValues(
      StorableIonMobilitySeries mobilogram/*, double[] dst*/) {
//    assert mobilogram.getNumberOfValues() <= dst.length;
//    mobilogramIntensityValues.get(mobilogram.getStorageOffset(), dst, 0, mobilogram.getNumberOfValues());
    double[] values = new double[mobilogram.getNumberOfValues()];
    mobilogramIntensityValues.get(mobilogram.getStorageOffset(), values, 0, values.length);
    return DoubleBuffer.wrap(values);
  }

  protected double getMobilogramMzValue(StorableIonMobilitySeries mobilogram, int index) {
    assert index < mobilogram.getNumberOfValues();
    return mobilogramMzValues.get(mobilogram.getStorageOffset() + index);
  }

  protected double getMobilogramIntensityValue(StorableIonMobilitySeries mobilogram, int index) {
    assert index < mobilogram.getNumberOfValues();
    return mobilogramIntensityValues.get(mobilogram.getStorageOffset() + index);
  }
}
