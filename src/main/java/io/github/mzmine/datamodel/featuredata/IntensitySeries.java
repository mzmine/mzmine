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
import java.nio.DoubleBuffer;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Stores a series of intensities.
 *
 * @author https://github.com/SteffenHeu
 */
public interface IntensitySeries extends SeriesValueCount {

  static void saveIntensityValuesToXML(XMLStreamWriter writer, IntensitySeries series)
      throws XMLStreamException {
    writer.writeStartElement(CONST.XML_INTENSITY_VALUES_ELEMENT);
    writer.writeAttribute(CONST.XML_NUM_VALUES_ATTR, String.valueOf(series.getNumberOfValues()));
    writer.writeCharacters(ParsingUtils.doubleBufferToString(series.getIntensityValueBuffer()));
    writer.writeEndElement();
  }

  /**
   * Tests a subset of intensity values of both series for equality. Note that the number of values
   * and the underlying buffer is checked for equality. However, of the actual series values, only
   * a subset of five points is compared.
   */
  static boolean seriesSubsetEqual(IntensitySeries s1, IntensitySeries s2) {
    if (s1.getNumberOfValues() != s2.getNumberOfValues()) {
      return false;
    }

    if (!s1.getIntensityValueBuffer().equals(s2.getIntensityValueBuffer())) {
      return false;
    }

    final int max = s1.getNumberOfValues() - 1;

    for (int i = 1; i < 5; i++) {
      if (Double.compare(s1.getIntensity(max / i), s2.getIntensity(max / i)) != 0) {
        return false;
      }
    }

    return true;
  }

  /**
   * @return All non-zero intensities.
   */
  DoubleBuffer getIntensityValueBuffer();

  /**
   * @param dst results are reflected in this array
   * @return All non-zero intensities.
   */
  default double[] getIntensityValues(double[] dst) {
    if (dst.length < getNumberOfValues()) {
      dst = new double[getNumberOfValues()];
    }
    getIntensityValueBuffer().get(0, dst, 0, getNumberOfValues());
    return dst;
  }

  /**
   * @param index
   * @return The intensity at the index position. Note that this
   */
  default double getIntensity(int index) {
    return getIntensityValueBuffer().get(index);
  }

  /**
   * @return The number of non-zero intensity values in this series.
   */
  default int getNumberOfValues() {
    return getIntensityValueBuffer().capacity();
  }
}
