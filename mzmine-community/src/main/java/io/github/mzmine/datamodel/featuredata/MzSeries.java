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

import static io.github.mzmine.datamodel.featuredata.impl.StorageUtils.contentEquals;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.DoubleBuffer;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Stores series of m/z values.
 *
 * @author https://github.com/SteffenHeu
 */
public interface MzSeries extends SeriesValueCount {

  /**
   * Appends an {@link MzSeries} element as a child to the current element.
   */
  static void saveMzValuesToXML(XMLStreamWriter writer, MzSeries series) throws XMLStreamException {
    writer.writeStartElement(CONST.XML_MZ_VALUES_ELEMENT);
    writer.writeAttribute(CONST.XML_NUM_VALUES_ATTR, String.valueOf(series.getNumberOfValues()));
    writer.writeCharacters(ParsingUtils.doubleBufferToString(series.getMZValueBuffer()));
    writer.writeEndElement();
  }

  /**
   * Tests a subset of the series for equality. Note that the number of values and the underlying
   * buffer is checked for equality.
   */
  static boolean seriesSubsetEqual(MzSeries s1, MzSeries s2) {
    if (s1.getNumberOfValues() != s2.getNumberOfValues()) {
      return false;
    }

    if(!contentEquals(s1.getMZValueBuffer(), s2.getMZValueBuffer())) {
      return false;
    }

    return true;
  }

  /**
   * @return All mz values corresponding to non-0 intensities.
   */
  MemorySegment getMZValueBuffer();

  /**
   * @param dst results are reflected in this array
   * @return All m/z values of detected data points.
   */
  default double[] getMzValues(double[] dst) {
    if (dst.length < getNumberOfValues()) {
      dst = new double[getNumberOfValues()];
    }
    MemorySegment.copy(getMZValueBuffer(), ValueLayout.JAVA_DOUBLE, 0, dst, 0, getNumberOfValues());
    return dst;
  }

  /**
   * @param index
   * @return The value at the index position. Note the index does not correspond to scan numbers.
   * @see IonTimeSeries#getMzForSpectrum(Scan)
   */
  default double getMZ(int index) {
    return getMZValueBuffer().getAtIndex(ValueLayout.JAVA_DOUBLE, index);
  }

  /**
   * @return The number of mz values corresponding to non-0 intensities.
   */
  default int getNumberOfValues() {
    return (int) StorageUtils.numDoubles(getMZValueBuffer());
  }

}
