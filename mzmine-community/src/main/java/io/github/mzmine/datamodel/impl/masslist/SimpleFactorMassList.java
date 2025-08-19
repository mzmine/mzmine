/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.impl.masslist;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import java.lang.foreign.MemorySegment;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.math.plot.utils.Array;

/**
 * This class represent detected masses (ions) in one mass spectrum. But the spectrum intensities
 * were multiplied by a factor.
 */
public class SimpleFactorMassList extends SimpleMassList {

  public static final String XML_ELEMENT = "simple_factor_masslist";
  public static final String XML_ATTR_FACTOR = "factor";

  private final double factor;

  public SimpleFactorMassList(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues, final double factor) {
    super(storage, mzValues, intensityValues);
    this.factor = factor;
  }

  /**
   * @param storage       the storage
   * @param mzIntensities 2D array with mzs[0][] an d intensities[1][].
   */
  public SimpleFactorMassList(@Nullable MemoryMapStorage storage, @NotNull double[][] mzIntensities,
      final double factor) {
    this(storage, mzIntensities[0], mzIntensities[1], factor);
  }

  protected SimpleFactorMassList(MemorySegment mzValues, MemorySegment intensityValues,
      final double factor) {
    super(mzValues, intensityValues);
    this.factor = factor;
  }

  /**
   * Use mzValues and intensityValues constructor
   *
   * @param storageMemoryMap
   * @param dps
   */
  @Deprecated
  public static SimpleFactorMassList create(MemoryMapStorage storageMemoryMap, DataPoint[] dps,
      final double factor) {
    double[][] mzIntensity = DataPointUtils.getDataPointsAsDoubleArray(dps);
    return new SimpleFactorMassList(storageMemoryMap, mzIntensity[0], mzIntensity[1], factor);
  }

  /**
   * Provides the original intensity value before it was multiplied by {@link #getFactor()}
   *
   * @param index data point index
   * @return intensity / factor
   */
  public double getOriginalIntensityValue(int index) {
    return getIntensityValue(index) / factor;
  }

  /**
   * Provides the original intensity value before it was multiplied by {@link #getFactor()}
   *
   * @param dst the array to hold the array of intensities
   * @return intensity / factor
   */
  public double[] getOriginalIntensityValues(double @NotNull [] dst) {
    // make defensive copy here
    final double[] values = Array.copy(getIntensityValues(dst));
    for (int i = 0; i < values.length; i++) {
      values[i] /= factor;
    }
    return values;
  }

  /**
   * Provides the original intensity value before it was multiplied by {@link #getFactor()}
   *
   * @return intensity / factor
   */
  public double[] getOriginalIntensityValues() {
    // make defensive copy here
    final double[] values = Array.copy(getIntensityValues(new double[getNumberOfDataPoints()]));
    for (int i = 0; i < values.length; i++) {
      values[i] /= factor;
    }
    return values;
  }

  public double getFactor() {
    return factor;
  }

  @Override
  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);

    writer.writeAttribute(XML_ATTR_FACTOR, String.valueOf(factor));
    super.writeSpectralArrays(writer);

    writer.writeEndElement();
  }

  public static MassList loadFromXML(XMLStreamReader reader, @Nullable MemoryMapStorage storage)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Wrong element.");
    }

    final double factor = Double.parseDouble(reader.getAttributeValue(null, XML_ATTR_FACTOR));

    double[] intensities = null;
    double[] mzs = null;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
      reader.next();

      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case CONST.XML_MZ_VALUES_ELEMENT ->
            mzs = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case CONST.XML_INTENSITY_VALUES_ELEMENT ->
            intensities = ParsingUtils.stringToDoubleArray(reader.getElementText());
      }
    }

    return new SimpleFactorMassList(storage, mzs, intensities, factor);
  }
}
