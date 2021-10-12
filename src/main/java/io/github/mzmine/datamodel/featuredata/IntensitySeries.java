/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
