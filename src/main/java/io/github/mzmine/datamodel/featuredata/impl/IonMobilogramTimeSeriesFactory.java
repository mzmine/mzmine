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

package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.modules.io.projectload.CachedIMSFrame;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author https://github.com/SteffenHeu
 */
public class IonMobilogramTimeSeriesFactory {

  private IonMobilogramTimeSeriesFactory() {

  }

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
   */
  public static IonMobilogramTimeSeries of(@Nullable MemoryMapStorage storage,
      @NotNull final List<IonMobilitySeries> mobilograms,
      @NotNull final BinningMobilogramDataAccess mobilogramBinning) {

    double[][] summedAndWeighted = sumIntensitiesWeightMzs(mobilograms);

    mobilogramBinning.setMobilogram(mobilograms);
    final SummedIntensityMobilitySeries summedMobilogram = mobilogramBinning.toSummedMobilogram(
        storage);

    return of(storage, summedAndWeighted[0], summedAndWeighted[1], mobilograms, summedMobilogram);
  }

  public static IonMobilogramTimeSeries of(@Nullable MemoryMapStorage storage,
      @NotNull final double[] rtMzs, @NotNull final double[] rtIntensities,
      @NotNull final List<IonMobilitySeries> mobilograms,
      @NotNull final BinningMobilogramDataAccess mobilogramBinning) {

    mobilogramBinning.setMobilogram(mobilograms);

    return of(storage, rtMzs, rtIntensities, mobilograms,
        mobilogramBinning.toSummedMobilogram(storage));
  }

  public static IonMobilogramTimeSeries of(@Nullable MemoryMapStorage storage,
      @NotNull final double[] rtMzs, @NotNull final double[] rtIntensities,
      @NotNull final List<IonMobilitySeries> mobilograms,
      @NotNull final double[] mobilogramMobilities, @NotNull final double[] mobilogramIntensities) {

    return of(storage, rtMzs, rtIntensities, mobilograms,
        new SummedIntensityMobilitySeries(storage, mobilogramMobilities, mobilogramIntensities));
  }

  public static IonMobilogramTimeSeries of(@Nullable MemoryMapStorage storage,
      @NotNull final double[] rtMzs, @NotNull final double[] rtIntensities,
      @NotNull final List<IonMobilitySeries> mobilograms,
      @NotNull final SummedIntensityMobilitySeries summedMobilogram) {

    final List<Frame> frames = new ArrayList<>(mobilograms.size());
    for (IonMobilitySeries ims : mobilograms) {
      Frame frame = ims.getSpectra().get(0).getFrame();
      // project load fix to prevent memory leaks
      frame = frame instanceof CachedIMSFrame cached ? cached.getOriginalFrame() : frame;
      frames.add(frame);
    }

    return of(storage, rtMzs, rtIntensities, mobilograms, frames, summedMobilogram);
  }

  public static IonMobilogramTimeSeries of(@Nullable MemoryMapStorage storage,
      @NotNull final double[] rtMzs, @NotNull final double[] rtIntensities,
      @NotNull final List<IonMobilitySeries> mobilograms, @NotNull final List<Frame> frames,
      @NotNull final SummedIntensityMobilitySeries summedMobilogram) {

    return new SimpleIonMobilogramTimeSeries(storage, rtMzs, rtIntensities, mobilograms, frames,
        summedMobilogram);
  }

  private static double[][] sumIntensitiesWeightMzs(List<IonMobilitySeries> mobilograms) {
    double[] summedIntensities = new double[mobilograms.size()];
    double[] weightedMzs = new double[mobilograms.size()];

    final int maxNumDetected = mobilograms.stream().mapToInt(IonMobilitySeries::getNumberOfValues)
        .max().getAsInt();

    final double[] tmpIntensities = new double[maxNumDetected];
    final double[] tmpMzs = new double[maxNumDetected];

    for (int i = 0; i < mobilograms.size(); i++) {
      final IonMobilitySeries ims = mobilograms.get(i);
      final int numValues = ims.getNumberOfValues();
      ims.getIntensityValues(tmpIntensities);
      for (int j = 0; j < numValues; j++) {
        summedIntensities[i] += tmpIntensities[j];
      }
    }

    for (int i = 0; i < mobilograms.size(); i++) {
      final IonMobilitySeries ims = mobilograms.get(i);
      final int numValues = ims.getNumberOfValues();
      Arrays.fill(tmpMzs, 0, numValues - 1, 0d);
      Arrays.fill(tmpIntensities, 0, numValues - 1, 0d);
      ims.getIntensityValues(tmpIntensities);
      ims.getMzValues(tmpMzs);
      double weightedMz = 0;

      for (int j = 0; j < numValues; j++) {
        weightedMz += tmpMzs[j] * (tmpIntensities[j] / summedIntensities[i]);
      }

      // due to added zeros, the summed intensity might have been 0 -> NaN
      if (Double.isNaN(weightedMz)) {
        weightedMz = 0d;
      }
      weightedMzs[i] = weightedMz;
    }
    return new double[][]{weightedMzs, summedIntensities};
  }

  /**
   * Loads a ion mobility trace from XML.
   *
   * @param reader  The reader.
   * @param storage The storage or null.
   * @param file    The file.
   * @return The loaded trace.
   */
  public static IonMobilogramTimeSeries loadFromXML(@NotNull XMLStreamReader reader,
      @Nullable MemoryMapStorage storage, @NotNull IMSRawDataFile file) throws XMLStreamException {

    List<Frame> scans = null;
    double[] mzs = null;
    double[] intensities = null;
    List<IonMobilitySeries> mobilograms = new ArrayList<>();
    SummedIntensityMobilitySeries summedMobilogram = null;

    while (reader.hasNext()) {
      if (reader.isEndElement() && reader.getLocalName()
          .equals(SimpleIonMobilogramTimeSeries.XML_ELEMENT)) {
        break;
      }

      final int next = reader.next();
      if (next != XMLEvent.START_ELEMENT) {
        continue;
      }
      switch (reader.getLocalName()) {
        case IonMobilitySeries.XML_ION_MOBILITY_SERIES_ELEMENT -> {
          mobilograms.add(SimpleIonMobilitySeries.loadFromXML(reader, null, file));
        }
        case CONST.XML_SCAN_LIST_ELEMENT -> {
          int[] indices = ParsingUtils.stringToIntArray(reader.getElementText());
          scans = ParsingUtils.getSublistFromIndices((List<Frame>) file.getFrames(), indices);
        }
        case CONST.XML_MZ_VALUES_ELEMENT -> mzs = ParsingUtils.stringToDoubleArray(
            reader.getElementText());
        case CONST.XML_INTENSITY_VALUES_ELEMENT -> intensities = ParsingUtils.stringToDoubleArray(
            reader.getElementText());
        case SummedIntensityMobilitySeries.XML_ELEMENT -> summedMobilogram = SummedIntensityMobilitySeries.loadFromXML(
            reader, storage);
      }
    }

    return IonMobilogramTimeSeriesFactory.of(storage, mzs, intensities, mobilograms,
        summedMobilogram);
  }
}
