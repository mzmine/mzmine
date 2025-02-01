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

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleOtherTimeSeries implements OtherTimeSeries {

  public static final String XML_OTHER_TIME_SERIES_ATTR_VALUE = "simpleothertimeseries";
  public static final String XML_TRACE_NAME_ATTR = "othertimeseries_name";

  public static final SimpleOtherTimeSeries empty = new SimpleOtherTimeSeries(null, new float[0],
      new double[0], "",
      new OtherTimeSeriesDataImpl(new OtherDataFileImpl(RawDataFile.createDummyFile())));

  /**
   * doubles
   */
  protected final MemorySegment intensityBuffer;

  /**
   * floats
   */
  protected final MemorySegment timeBuffer;
  protected final String name;
  private final @NotNull OtherTimeSeriesData timeSeriesData;

  public SimpleOtherTimeSeries(@Nullable MemoryMapStorage storage, @NotNull float[] rtValues,
      @NotNull double[] intensityValues, String name, @NotNull OtherTimeSeriesData timeSeriesData) {
    if (intensityValues.length != rtValues.length) {
      throw new IllegalArgumentException("Intensities and RT values must have the same length");
    }
    for (int i = 0; i < rtValues.length - 1; i++) {
      if (rtValues[i + 1] < rtValues[i]) {
        throw new IllegalArgumentException("Chromatogram not sorted in retention time");
      }
    }

    this.timeSeriesData = timeSeriesData;
    intensityBuffer = StorageUtils.storeValuesToDoubleBuffer(storage, intensityValues);
    timeBuffer = StorageUtils.storeValuesToFloatBuffer(storage, rtValues);
    this.name = name;
  }

  public SimpleOtherTimeSeries(@NotNull MemorySegment rtValues,
      @NotNull MemorySegment intensityValues, String name,
      @NotNull OtherTimeSeriesData timeSeriesData) {
    if (StorageUtils.numDoubles(intensityValues) != StorageUtils.numFloats(rtValues)) {
      throw new IllegalArgumentException("Intensities and RT values must have the same length");
    }
    this.timeSeriesData = timeSeriesData;
    intensityBuffer = intensityValues;
    timeBuffer = rtValues;
    this.name = name;
  }

  public static OtherTimeSeries loadFromXML(XMLStreamReader reader, RawDataFile currentFile)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(OtherTimeSeries.XML_ELEMENT)
        && Objects.equals(
        reader.getAttributeValue(null, OtherTimeSeries.XML_OTHER_TIME_SERIES_ATTR),
        XML_OTHER_TIME_SERIES_ATTR_VALUE))) {
      throw new IllegalStateException("Wrong element");
    }

    double[] intensities = null;
    float[] rts = null;
    OtherTimeSeriesData timeSeriesData = null;

    final String name = ParsingUtils.readNullableString(
        reader.getAttributeValue(null, XML_TRACE_NAME_ATTR));

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(OtherTimeSeries.XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case CONST.XML_INTENSITY_VALUES_ELEMENT ->
            intensities = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case CONST.XML_OTHER_TIME_VALUES_ELEMENT ->
            rts = ParsingUtils.stringToFloatArray(reader.getElementText());
        case OtherTimeSeriesData.XML_ELEMENT -> {
          timeSeriesData = OtherTimeSeriesData.loadFromXML(reader, currentFile);
        }
        default -> {
        }
      }
    }

    if (rts == null || intensities == null || timeSeriesData == null) {
      return null;
    }

    return new SimpleOtherTimeSeries(currentFile.getMemoryMapStorage(), rts, intensities, name,
        timeSeriesData);
  }

  @Override
  public MemorySegment getIntensityValueBuffer() {
    return intensityBuffer;
  }

  @Override
  public float getRetentionTime(int index) {
    assert index < StorageUtils.numFloats(timeBuffer);
    return timeBuffer.getAtIndex(ValueLayout.JAVA_FLOAT, index);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ChromatogramType getChromatoogramType() {
    return getTimeSeriesData().getChromatogramType();
  }

  @Override
  public @NotNull OtherDataFile getOtherDataFile() {
    return timeSeriesData.getOtherDataFile();
  }

  @Override
  @NotNull
  public OtherTimeSeriesData getTimeSeriesData() {
    return timeSeriesData;
  }

  @Override
  public OtherTimeSeries copyAndReplace(MemoryMapStorage storage, double[] newIntensities,
      String newName) {
    if (getNumberOfValues() != newIntensities.length) {
      throw new IllegalArgumentException("The number of intensities does not match number of rts.");
    }

    return new SimpleOtherTimeSeries(timeBuffer,
        StorageUtils.storeValuesToDoubleBuffer(storage, newIntensities), newName,
        getTimeSeriesData());
  }

  @Override
  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(OtherTimeSeries.XML_ELEMENT);
    writer.writeAttribute(OtherTimeSeries.XML_OTHER_TIME_SERIES_ATTR,
        XML_OTHER_TIME_SERIES_ATTR_VALUE);
    writer.writeAttribute(XML_TRACE_NAME_ATTR, ParsingUtils.parseNullableString(name));

    IntensitySeries.saveIntensityValuesToXML(writer, this);
    saveRtValuesToXML(writer);
    timeSeriesData.saveToXML(writer);

    writer.writeEndElement();
  }

  private void saveRtValuesToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(CONST.XML_OTHER_TIME_VALUES_ELEMENT);
    writer.writeAttribute(CONST.XML_NUM_VALUES_ATTR, String.valueOf(getNumberOfValues()));
    writer.writeCharacters(ParsingUtils.floatBufferToString(timeBuffer));
    writer.writeEndElement();
  }

  @Override
  public OtherTimeSeries subSeries(MemoryMapStorage storage, float start, float end) {
    // todo does this work with float to double?
    final IndexRange indexRange = BinarySearch.indexRange(start, end, getNumberOfValues(),
        this::getRetentionTime);
    return subSeries(storage, indexRange.min(), indexRange.maxExclusive());
  }

  @Override
  public OtherTimeSeries subSeries(MemoryMapStorage storage, int startIndexInclusive,
      int endIndexExclusive) {

    return new SimpleOtherTimeSeries(
        StorageUtils.sliceFloats(timeBuffer, startIndexInclusive, endIndexExclusive),
        StorageUtils.sliceDoubles(intensityBuffer, startIndexInclusive, endIndexExclusive), name,
        timeSeriesData);
  }

  @Override
  public @Nullable MemoryMapStorage getStorage() {
    return getTimeSeriesData().getOtherDataFile().getCorrespondingRawDataFile()
        .getMemoryMapStorage();
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimpleOtherTimeSeries that)) {
      return false;
    }

    return StorageUtils.contentEquals(intensityBuffer, that.intensityBuffer)
        && StorageUtils.contentEquals(timeBuffer, that.timeBuffer) && Objects.equals(getName(),
        that.getName()) && getTimeSeriesData().equals(that.getTimeSeriesData());
  }

  @Override
  public int hashCode() {
    int result = 31 * Objects.hashCode(getName());
    result = 31 * result + getTimeSeriesData().hashCode();
    return result;
  }

  @Override
  public OtherTimeSeries emptySeries() {
    return empty;
  }
}
