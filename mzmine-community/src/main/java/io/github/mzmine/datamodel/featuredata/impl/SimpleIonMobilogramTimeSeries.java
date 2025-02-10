/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.featuredata.impl;

import static io.github.mzmine.datamodel.featuredata.impl.StorageUtils.contentEquals;
import static io.github.mzmine.datamodel.featuredata.impl.StorageUtils.numDoubles;

import com.google.common.collect.Comparators;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.featuredata.MzSeries;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IonMobilityTraceBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing.MobilogramBinningModule;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout.OfDouble;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to store ion mobility-LC-MS data.
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleIonMobilogramTimeSeries implements IonMobilogramTimeSeries {

  public static final String XML_ELEMENT = "simpleionmobilogramtimeseries";

  protected final List<Frame> frames;
  protected final SummedIntensityMobilitySeries summedMobilogram;
  // all segments contain doubles
  protected final MemorySegment intensityValues;
  protected final MemorySegment mzValues;
  protected final StoredMobilograms mobilograms;

  /**
   * Creates a new {@link SimpleIonMobilogramTimeSeries}. For more convenient usage, see
   * {@link IonMobilogramTimeSeriesFactory#of(MemoryMapStorage, List, BinningMobilogramDataAccess)}
   * The indices of mzs, intensities, mobilograms and frames must match. All arrays/lists must have
   * the same length.
   *
   * @param storage          The {@link MemoryMapStorage} to be used. May be null.
   * @param mzs              The mz values of this series. Should be calculated from all detected
   *                         signals in the {@link IonMobilitySeries} of the same index.
   * @param intensities      The intensity values of this series. Should be calculated from all
   *                         detected signals in the {@link IonMobilitySeries} of the same index.
   * @param mobilograms      The mobilograms of this series.
   * @param frames           The frames the mobilograms were detected in.
   * @param summedMobilogram A summed mobilogram calculated from all {@link IonMobilitySeries}.
   *                         Intensity should be summed within given mobility bins, specified in the
   *                         last module call of {@link IonMobilityTraceBuilderModule} or
   *                         {@link MobilogramBinningModule}. The last binning value can be obtained
   *                         via
   *                         {@link
   *                         BinningMobilogramDataAccess#getPreviousBinningWith(ModularFeatureList,
   *                         MobilityType)}
   */
  public SimpleIonMobilogramTimeSeries(@Nullable MemoryMapStorage storage, @NotNull double[] mzs,
      @NotNull double[] intensities, @NotNull List<IonMobilitySeries> mobilograms,
      @NotNull List<Frame> frames, @NotNull final SummedIntensityMobilitySeries summedMobilogram) {

    if (mzs.length != intensities.length || mobilograms.size() != intensities.length) {
      throw new IllegalArgumentException(
          "Length of mz, intensity, frames and/or mobilograms does not match.");
    }
    if (!checkRawFileIntegrity(mobilograms)) {
      throw new IllegalArgumentException("Cannot combine mobilograms of different raw data files.");
    }

    this.frames = frames;
    this.summedMobilogram = summedMobilogram;
    this.mobilograms = IonMobilogramTimeSeriesFactory.storeMobilograms(this, storage, mobilograms);

    mzValues = StorageUtils.storeValuesToDoubleBuffer(storage, mzs);
    intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, intensities);
  }

  /**
   * Creates a new {@link SimpleIonMobilogramTimeSeries}. For more convenient usage, see
   * {@link IonMobilogramTimeSeriesFactory#of(MemoryMapStorage, List, BinningMobilogramDataAccess)}
   * The indices of mzs, intensities, mobilograms and frames must match. All arrays/lists must have
   * the same length.
   *
   * @param storage          The {@link MemoryMapStorage} to be used. May be null.
   * @param mzValues         The mz values of this series. Should be calculated from all detected
   *                         signals in the {@link IonMobilitySeries} of the same index.
   * @param intensityValues  The intensity values of this series. Should be calculated from all
   *                         detected signals in the {@link IonMobilitySeries} of the same index.
   * @param mobilograms      The mobilograms of this series.
   * @param frames           The frames the mobilograms were detected in.
   * @param summedMobilogram A summed mobilogram calculated from all {@link IonMobilitySeries}.
   *                         Intensity should be summed within given mobility bins, specified in the
   *                         last module call of {@link IonMobilityTraceBuilderModule} or
   *                         {@link MobilogramBinningModule}. The last binning value can be obtained
   *                         via
   *                         {@link
   *                         BinningMobilogramDataAccess#getPreviousBinningWith(ModularFeatureList,
   *                         MobilityType)}
   */
  public SimpleIonMobilogramTimeSeries(@NotNull MemorySegment mzValues,
      MemorySegment intensityValues, @Nullable MemoryMapStorage storage,
      @NotNull List<IonMobilitySeries> mobilograms, @NotNull List<Frame> frames,
      @NotNull final SummedIntensityMobilitySeries summedMobilogram) {
    if (mzValues.byteSize() != intensityValues.byteSize() || mobilograms.size() != numDoubles(
        intensityValues)) {
      throw new IllegalArgumentException(
          "Length of mz, intensity, frames and/or mobilograms does not match.");
    }
    if (!checkRawFileIntegrity(mobilograms)) {
      throw new IllegalArgumentException("Cannot combine mobilograms of different raw data files.");
    }

    this.intensityValues = intensityValues;
    this.mzValues = mzValues;
    this.frames = frames;
    this.summedMobilogram = summedMobilogram;
    this.mobilograms = IonMobilogramTimeSeriesFactory.storeMobilograms(this, storage, mobilograms);
  }

  @Override
  public List<Frame> getSpectraModifiable() {
    return frames;
  }

  @Override
  public IonMobilogramTimeSeries subSeries(@Nullable MemoryMapStorage storage,
      @NotNull List<Frame> subset) {
    throw new UnsupportedOperationException(
        "Unsupported operation. Requires BinningMobilogramDataAccess for IonMobilogramTimeSeries.");
  }

  @Override
  public IonMobilogramTimeSeries subSeries(@Nullable MemoryMapStorage storage,
      @NotNull List<Frame> subset, @NotNull final BinningMobilogramDataAccess mobilogramBinning) {
    if (subset.isEmpty()) {
      return IonMobilogramTimeSeries.EMPTY;
    }

    double[] mzs = new double[subset.size()];
    double[] intensities = new double[subset.size()];

    final List<Frame> spectra = getSpectra();
    int sindex = 0;
    for (int i = 0; i < subset.size(); i++) {
      Frame sub = subset.get(i);
      // find next spectrum
      while (spectra.get(sindex) != sub) {
        sindex++;
        if (sindex >= spectra.size()) {
          // exceptional case: no scan in spectra that matches sub from the subset, throw
          if (!Comparators.isInOrder(subset, Comparator.comparingInt(Scan::getScanNumber))) {
            throw new IllegalArgumentException(
                "Subset of scans was not sorted by scan number (which should reflect retention time / mobility)");
          }
          if (!Comparators.isInOrder(spectra, Comparator.comparingInt(Scan::getScanNumber))) {
            throw new IllegalArgumentException(
                "Original IonTimeSeries scans were not sorted by scan number (which should reflect retention time / mobility)");
          }
          throw new IllegalArgumentException(
              "Not all scans of subset were present in this IonTimeSeries");
        }
      }
      // set mz
      mzs[i] = getMZ(sindex);
      intensities[i] = getIntensity(sindex);
    }

    List<IonMobilitySeries> subMobilograms = new ArrayList<>(subset.size());
    for (IonMobilitySeries mobilogram : mobilograms.storedMobilograms()) {
      if (subset.contains(mobilogram.getSpectrum(0).getFrame())) {
        subMobilograms.add(mobilogram);
      }
    }

    return IonMobilogramTimeSeriesFactory.of(storage, mzs, intensities, subMobilograms,
        mobilogramBinning);
  }

  @Override
  public IonMobilogramTimeSeries subSeries(MemoryMapStorage storage, int startIndexInclusive,
      int endIndexExclusive, BinningMobilogramDataAccess mobilogramBinning) {

    if (endIndexExclusive - startIndexInclusive <= 0) {
      return IonMobilogramTimeSeries.EMPTY;
    }

    final List<IonMobilitySeries> mobilograms = IntStream.range(startIndexInclusive,
        endIndexExclusive).mapToObj(this::getMobilogram).toList();
    mobilogramBinning.setMobilogram(mobilograms);

    return new SimpleIonMobilogramTimeSeries(
        StorageUtils.sliceDoubles(mzValues, startIndexInclusive, endIndexExclusive),
        StorageUtils.sliceDoubles(intensityValues, startIndexInclusive, endIndexExclusive), storage,
        mobilograms, frames.subList(startIndexInclusive, endIndexExclusive),
        mobilogramBinning.toSummedMobilogram(storage));
  }

  @Override
  public MemorySegment getIntensityValueBuffer() {
    return intensityValues;
  }

  @Override
  public MemorySegment getMZValueBuffer() {
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
    return Collections.unmodifiableList(mobilograms.storedMobilograms());
  }

  @Override
  public IonMobilogramTimeSeries copy(@Nullable MemoryMapStorage storage) {
    double[][] data = DataPointUtils.getDataPointsAsDoubleArray(getMZValueBuffer(),
        getIntensityValueBuffer());
    return IonMobilogramTimeSeriesFactory.of(storage, data[0], data[1],
        mobilograms.storedMobilograms(), frames, summedMobilogram.copy(storage));
  }

  @Override
  public SummedIntensityMobilitySeries getSummedMobilogram() {
    return summedMobilogram;
  }

  @Override
  public IonMobilogramTimeSeries copyAndReplace(@Nullable final MemoryMapStorage storage,
      @NotNull final double[] newIntensityValues) {
    MemorySegment intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage,
        newIntensityValues);
    return new SimpleIonMobilogramTimeSeries(mzValues, intensityValues, storage,
        mobilograms.storedMobilograms(), frames, summedMobilogram);
  }

  @Override
  public IonMobilogramTimeSeries copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull double[] newMzValues, @NotNull double[] newIntensityValues) {
    return IonMobilogramTimeSeriesFactory.of(storage, newMzValues, newIntensityValues,
        mobilograms.storedMobilograms(), this.frames, summedMobilogram.copy(storage));
  }

  /**
   * Allows creation of a new {@link IonMobilogramTimeSeries} with processed
   * {@link SummedIntensityMobilitySeries}.
   *
   * @param storage
   * @param summedMobilogram
   * @return
   */
  @Override
  public IonMobilogramTimeSeries copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull SummedIntensityMobilitySeries summedMobilogram) {

    return new SimpleIonMobilogramTimeSeries(storage,
        DataPointUtils.getDoubleBufferAsArray(mzValues),
        DataPointUtils.getDoubleBufferAsArray(intensityValues), mobilograms.storedMobilograms(),
        this.frames, summedMobilogram);
  }


  private boolean checkRawFileIntegrity(@NotNull List<IonMobilitySeries> mobilograms) {
    if (mobilograms.isEmpty()) {
      return true;
    }
    RawDataFile file = null;
    for (IonMobilitySeries mobilogram : mobilograms) {
      if (file == null) {
        file = mobilogram.getSpectrum(0).getDataFile();
      } else if (mobilogram.getSpectrum(0).getDataFile() != file) {
        return false;
      }
    }
    return true;
  }

  protected MemorySegment getMobilogramMzValues(
      StorableIonMobilitySeries mobilogram/*, double[] dst*/) {
    return StorageUtils.sliceDoubles(mobilograms.storedMzValues(), mobilogram.getStorageOffset(),
        mobilogram.getStorageOffset() + mobilogram.getNumberOfValues());
  }

  protected MemorySegment getMobilogramIntensityValues(
      StorableIonMobilitySeries mobilogram/*, double[] dst*/) {
    return StorageUtils.sliceDoubles(mobilograms.storedIntensityValues(),
        mobilogram.getStorageOffset(),
        mobilogram.getStorageOffset() + mobilogram.getNumberOfValues());
  }

  protected double getMobilogramMzValue(StorableIonMobilitySeries mobilogram, int index) {
    assert index < mobilogram.getNumberOfValues();
    return mobilograms.storedMzValues()
        .getAtIndex(OfDouble.JAVA_DOUBLE, mobilogram.getStorageOffset() + index);
  }

  protected double getMobilogramIntensityValue(StorableIonMobilitySeries mobilogram, int index) {
    assert index < mobilogram.getNumberOfValues();
    return mobilograms.storedIntensityValues()
        .getAtIndex(OfDouble.JAVA_DOUBLE, mobilogram.getStorageOffset() + index);
  }

  public List<IonMobilitySeries> getMobilogramsModifiable() {
    return mobilograms.storedMobilograms();
  }

  @Override
  public void saveValueToXML(XMLStreamWriter writer, List<Frame> allScans)
      throws XMLStreamException {
    writer.writeStartElement(SimpleIonMobilogramTimeSeries.XML_ELEMENT);

    IntensitySeries.saveIntensityValuesToXML(writer, this);
    MzSeries.saveMzValuesToXML(writer, this);
    IonSpectrumSeries.saveSpectraIndicesToXML(writer, this, allScans);

    summedMobilogram.saveValueToXML(writer);

    for (IonMobilitySeries mobilogram : mobilograms.storedMobilograms()) {
      IonMobilitySeries.saveMobilogramToXML(writer, mobilogram,
          mobilogram.getSpectrum(0).getFrame().getMobilityScans());
    }

    writer.writeEndElement();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimpleIonMobilogramTimeSeries)) {
      return false;
    }
    SimpleIonMobilogramTimeSeries that = (SimpleIonMobilogramTimeSeries) o;
    return Objects.equals(getMobilograms(), that.getMobilograms()) && Objects.equals(frames,
        that.frames) && contentEquals(intensityValues, that.intensityValues) && contentEquals(
        mzValues, that.mzValues) && Objects.equals(getSummedMobilogram(),
        that.getSummedMobilogram()) && contentEquals(mobilograms.storedMzValues(),
        that.mobilograms.storedMzValues()) && contentEquals(mobilograms.storedIntensityValues(),
        that.mobilograms.storedIntensityValues());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMobilograms(), frames, intensityValues.byteSize(), mzValues.byteSize(),
        getSummedMobilogram(), mobilograms.storedMzValues().byteSize(),
        mobilograms.storedIntensityValues().byteSize());
  }

  @Override
  public IonMobilogramTimeSeries emptySeries() {
    return IonMobilogramTimeSeries.EMPTY;
  }
}
