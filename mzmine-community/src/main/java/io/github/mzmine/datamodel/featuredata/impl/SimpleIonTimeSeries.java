/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import com.google.common.collect.Comparators;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.MzSeries;
import io.github.mzmine.modules.io.projectload.CachedIMSFrame;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import java.lang.foreign.MemorySegment;
import java.nio.DoubleBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to store LC-MS data.
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleIonTimeSeries implements IonTimeSeries<Scan> {

  public static final String XML_ELEMENT = "simpleiontimeseries";

  protected final List<? extends Scan> scans;
  protected final MemorySegment intensityValues;
  protected final MemorySegment mzValues;

  /**
   * @param storage         may be null if forceStoreInRam is true
   * @param mzValues
   * @param intensityValues
   * @param scans
   */
  public SimpleIonTimeSeries(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues, @NotNull List<? extends Scan> scans) {
    if (mzValues.length != intensityValues.length || mzValues.length != scans.size()) {
      throw new IllegalArgumentException("Length of mz, intensity and/or scans does not match.");
    }
    for (int i = 1; i < scans.size(); i++) {
      if (scans.get(i).getRetentionTime() < scans.get(i - 1).getRetentionTime()) {
        throw new IllegalArgumentException(
            "Scans not sorted in retention time dimension! Cannot create chromatogram.");
      }
    }

    this.scans = scans;

    this.mzValues = StorageUtils.storeValuesToDoubleBuffer(storage, mzValues);
    this.intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, intensityValues);
  }

  public static SimpleIonTimeSeries loadFromXML(XMLStreamReader reader, MemoryMapStorage storage,
      RawDataFile file) throws XMLStreamException {

    List<Scan> scans = null;
    double[] mzs = null;
    double[] intensities = null;

    while (reader.hasNext()) {
      if (reader.isEndElement() && reader.getLocalName().equals(SimpleIonTimeSeries.XML_ELEMENT)) {
        break;
      }

      final int next = reader.next();
      if (next != XMLEvent.START_ELEMENT) {
        continue;
      }
      switch (reader.getLocalName()) {
        case CONST.XML_SCAN_LIST_ELEMENT -> {
          int[] indices = ParsingUtils.stringToIntArray(reader.getElementText());
          scans = ParsingUtils.getSublistFromIndices(file.getScans(), indices); // use all scans

          // if the scans were CachedFrames, we have to replace them when storing them to the series,
          // otherwise, we would keep the refences to cached mobility scans alive.
          if (scans.get(0) instanceof CachedIMSFrame) {
            scans = scans.stream().map(scan -> ((CachedIMSFrame) scan).getOriginalFrame())
                .map(f -> (Scan) f).toList();
          }
        }
        case CONST.XML_MZ_VALUES_ELEMENT ->
            mzs = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case CONST.XML_INTENSITY_VALUES_ELEMENT ->
            intensities = ParsingUtils.stringToDoubleArray(reader.getElementText());
      }
    }

    return new SimpleIonTimeSeries(storage, mzs, intensities, scans);
  }

  @Override
  public SimpleIonTimeSeries subSeries(@Nullable MemoryMapStorage storage,
      @NotNull List<Scan> subset) {
    double[] mzs = new double[subset.size()];
    double[] intensities = new double[subset.size()];

    final List<Scan> spectra = getSpectra();
    int sindex = 0;
    for (int i = 0; i < subset.size(); i++) {
      Scan sub = subset.get(i);
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

    return new SimpleIonTimeSeries(storage, mzs, intensities, subset);
  }

  @Override
  public MemorySegment getIntensityValueBuffer() {
    return intensityValues;
  }

  @Override
  public MemorySegment getMZValueBuffer() {
    return mzValues;
  }

  @Override
  public List<Scan> getSpectra() {
    return Collections.unmodifiableList(scans);
  }

  @Override
  public float getRetentionTime(int index) {
    return scans.get(index).getRetentionTime();
  }

  @Override
  public IonSpectrumSeries<Scan> copy(MemoryMapStorage storage) {
    double[][] data = DataPointUtils.getDataPointsAsDoubleArray(getMZValueBuffer(),
        getIntensityValueBuffer());

    return copyAndReplace(storage, data[0], data[1]);
  }

  @Override
  public IonTimeSeries<Scan> copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull double[] newMzValues, @NotNull double[] newIntensityValues) {

    return new SimpleIonTimeSeries(storage, newMzValues, newIntensityValues, this.scans);
  }

  @Override
  public void saveValueToXML(XMLStreamWriter writer, List<Scan> allScans)
      throws XMLStreamException {
    writer.writeStartElement(SimpleIonTimeSeries.XML_ELEMENT);

    IonSpectrumSeries.saveSpectraIndicesToXML(writer, this, allScans); // use all scans
    IntensitySeries.saveIntensityValuesToXML(writer, this);
    MzSeries.saveMzValuesToXML(writer, this);

    writer.writeEndElement();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimpleIonTimeSeries that)) {
      return false;
    }
    return Objects.equals(scans, that.scans) && IntensitySeries.seriesSubsetEqual(this, that)
        && MzSeries.seriesSubsetEqual(this, that);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scans, intensityValues.byteSize(), mzValues.byteSize());
  }
}
