/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import java.lang.foreign.MemorySegment;
import java.nio.DoubleBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores data points of several {@link MobilityScan}s. Usually wrapped in a
 * {@link SimpleIonMobilogramTimeSeries} representing the same feature with mobility resolution.
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleIonMobilitySeries implements IonMobilitySeries, ModifiableSpectra<MobilityScan> {

  private static final Logger logger = Logger.getLogger(SimpleIonMobilitySeries.class.getName());

  protected final List<MobilityScan> scans;

  protected final MemorySegment intensityValues;
  protected final MemorySegment mzValues;

  /**
   * @param storage         May be null if forceStoreInRam is true.
   * @param mzValues
   * @param intensityValues
   * @param scans
   */
  public SimpleIonMobilitySeries(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues, @NotNull List<MobilityScan> scans) {
    if (mzValues.length != intensityValues.length || mzValues.length != scans.size()) {
      throw new IllegalArgumentException("Length of mz, intensity and/or scans does not match.");
    }

    final Frame frame = scans.get(0).getFrame();
    for (MobilityScan scan : scans) {
      if (frame != scan.getFrame()) {
        throw new IllegalArgumentException("All mobility scans must belong to the same frame.");
      }
    }

    this.scans = scans;
    this.mzValues = StorageUtils.storeValuesToDoubleBuffer(storage, mzValues);
    this.intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, intensityValues);
  }

  public static SimpleIonMobilitySeries loadFromXML(@NotNull XMLStreamReader reader,
      @Nullable MemoryMapStorage storage, @NotNull IMSRawDataFile file) throws XMLStreamException {

    List<MobilityScan> scans = null;
    double[] mzs = null;
    double[] intensities = null;
    int frameindex = -1;
    frameindex = Integer.parseInt(
        reader.getAttributeValue(null, IonMobilitySeries.XML_FRAME_INDEX_ELEMENT));

    while (reader.hasNext()) {
      if (reader.isEndElement() && reader.getLocalName()
          .equals(IonMobilitySeries.XML_ION_MOBILITY_SERIES_ELEMENT)) {
        break;
      }

      final int next = reader.next();
      if (next != XMLEvent.START_ELEMENT) {
        continue;
      }
      switch (reader.getLocalName()) {
        case CONST.XML_SCAN_LIST_ELEMENT -> {
          if (frameindex == -1) {
            throw new IllegalStateException(
                "Cannot load mobility scans without frame index being set.");
          }
          int[] indices = ParsingUtils.stringToIntArray(reader.getElementText());
          scans = ParsingUtils.getSublistFromIndices(file.getFrame(frameindex).getMobilityScans(),
              indices);
        }
        case CONST.XML_MZ_VALUES_ELEMENT ->
            mzs = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case CONST.XML_INTENSITY_VALUES_ELEMENT ->
            intensities = ParsingUtils.stringToDoubleArray(reader.getElementText());
      }
    }

    return new SimpleIonMobilitySeries(storage, mzs, intensities, scans);
  }

  @Override
  public double getIntensityForSpectrum(MobilityScan spectrum) {
    int index = scans.indexOf(spectrum);
    if (index != -1) {
      return getIntensity(index);
    }
    return 0d;
  }

  @Override
  public double getMzForSpectrum(MobilityScan spectrum) {
    int index = scans.indexOf(spectrum);
    if (index != -1) {
      return getMZ(index);
    }
    return 0d;
  }

  @Override
  public IonSpectrumSeries<MobilityScan> subSeries(@Nullable MemoryMapStorage storage,
      @NotNull List<MobilityScan> subset) {
    double[] mzs = new double[subset.size()];
    double[] intensities = new double[subset.size()];

    for (int i = 0; i < subset.size(); i++) {
      mzs[i] = getMzForSpectrum(subset.get(i));
      intensities[i] = getIntensityForSpectrum(subset.get(i));
    }

    return new SimpleIonMobilitySeries(storage, mzs, intensities, subset);
  }

  @Override
  public MemorySegment getIntensityValueBuffer() {
    return intensityValues;
  }

  @Override
  public MemorySegment getMZValueBuffer() {
    return mzValues;
  }

  public double getMobility(int index) {
    return getSpectra().get(index).getMobility();
  }

  @Override
  public List<MobilityScan> getSpectra() {
    return Collections.unmodifiableList(scans);
  }

  @Override
  public IonSpectrumSeries<MobilityScan> copy(@Nullable MemoryMapStorage storage) {
    double[][] data = DataPointUtils.getDataPointsAsDoubleArray(getMZValueBuffer(),
        getIntensityValueBuffer());

    return new SimpleIonMobilitySeries(storage, data[0], data[1], scans);
  }

  @Override
  public List<MobilityScan> getSpectraModifiable() {
    return scans;
  }

  @Override
  public IonSpectrumSeries<MobilityScan> copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull double[] newMzValues, @NotNull double[] newIntensityValues) {
    return new SimpleIonMobilitySeries(storage, newMzValues, newIntensityValues, scans);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimpleIonMobilitySeries)) {
      return false;
    }
    SimpleIonMobilitySeries that = (SimpleIonMobilitySeries) o;
    return Objects.equals(scans, that.scans) && contentEquals(intensityValues,
        that.intensityValues) && contentEquals(mzValues, that.mzValues)
        && IntensitySeries.seriesSubsetEqual(this, that);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scans, intensityValues.byteSize(), mzValues.byteSize());
  }
}
