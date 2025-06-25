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

package io.github.mzmine.datamodel.featuredata;


import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Stores retention time values.
 */
public interface TimeSeries extends SeriesValueCount {

  // no FloatBuffer getRetentionTimeValues(), because this usually occurs with scans and
  // thus rt is stored in the scan object and present in ram

  /**
   * Note that the index does not correspond to scan numbers. Usually,
   * {@link io.github.mzmine.datamodel.Scan} are associated with series, making
   * {@link IonSpectrumSeries#getSpectra()} or {@link IonSpectrumSeries#getSpectrum(int)} more
   * convenient.
   *
   * @param index
   * @return The rt value at the index position.
   */
  float getRetentionTime(int index);

  static void saveValuesToXML(XMLStreamWriter writer, TimeSeries series) throws XMLStreamException {
    final float[] rts = new float[series.getNumberOfValues()];
    for (int i = 0; i < series.getNumberOfValues(); i++) {
      rts[i] = series.getRetentionTime(i);
    }

    writer.writeStartElement(CONST.XML_RETENTION_TIME_VALUES_ELEMENT);
    writer.writeAttribute(CONST.XML_NUM_VALUES_ATTR, String.valueOf(rts.length));
    writer.writeCharacters(ParsingUtils.floatArrayToString(rts));
    writer.writeEndElement();
  }


}
