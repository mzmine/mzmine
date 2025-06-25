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
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.util.MemoryMapStorage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;

public interface OtherTimeSeries extends IntensityTimeSeries {

  String XML_ELEMENT = "othertimeseries";
  String XML_OTHER_TIME_SERIES_ATTR = "othertimeseriestype";

  static OtherTimeSeries loadFromXML(XMLStreamReader reader, @NotNull RawDataFile file)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(OtherTimeSeries.XML_ELEMENT))) {
      throw new IllegalStateException("Wrong element");
    }

    return switch (reader.getAttributeValue(null, XML_OTHER_TIME_SERIES_ATTR)) {
      case SimpleOtherTimeSeries.XML_OTHER_TIME_SERIES_ATTR_VALUE ->
          SimpleOtherTimeSeries.loadFromXML(reader, file);
      default -> throw new IllegalStateException(
          "Unknown OtherTImeSeries data type (%s).".formatted(
              reader.getAttributeValue(null, XML_OTHER_TIME_SERIES_ATTR)));
    };
  }

  String getName();

  ChromatogramType getChromatoogramType();

  @NotNull OtherDataFile getOtherDataFile();

  @NotNull OtherTimeSeriesData getTimeSeriesData();

  @Override
  OtherTimeSeries subSeries(MemoryMapStorage storage, int startIndexInclusive,
      int endIndexExclusive);

  @Override
  OtherTimeSeries subSeries(MemoryMapStorage storage, float start, float end);

  OtherTimeSeries copyAndReplace(MemoryMapStorage storage, double[] newIntensities, String newName);

  /**
   * Saves this time series to xml. The implementing class is responsible for creating the xml
   * element and closing the xml element. The created element must be a
   * {@link OtherTimeSeries#XML_ELEMENT} and set the
   * {@link OtherTimeSeries#XML_OTHER_TIME_SERIES_ATTR} to a distinctive value. The loading method
   *
   * @param writer The writer.
   */
  void saveToXML(XMLStreamWriter writer) throws XMLStreamException;

  @Override
  OtherTimeSeries emptySeries();
}
