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

package io.github.mzmine.datamodel.impl.masslist;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.impl.AbstractStorableSpectrum;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class represent detected masses (ions) in one mass spectrum
 */
public class SimpleMassList extends AbstractStorableSpectrum implements MassList {

  public static final String XML_ELEMENT = "simplemasslist";

  public SimpleMassList(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues) {
    super(storage, mzValues, intensityValues);
  }

  /**
   * @param storage       the storage
   * @param mzIntensities 2D array with mzs[0][] an d intensities[1][].
   */
  public SimpleMassList(@Nullable MemoryMapStorage storage, @NotNull double[][] mzIntensities) {
    super(storage, mzIntensities[0], mzIntensities[1]);
  }

  /**
   * Use mzValues and intensityValues constructor
   *
   * @param storageMemoryMap
   * @param dps
   */
  @Deprecated
  public static MassList create(MemoryMapStorage storageMemoryMap, DataPoint[] dps) {
    double[][] mzIntensity = DataPointUtils.getDataPointsAsDoubleArray(dps);
    return new SimpleMassList(storageMemoryMap, mzIntensity[0], mzIntensity[1]);
  }

  @Override
  public DataPoint[] getDataPoints() {
    final double[][] mzIntensity = new double[2][];
    final int numDp = getNumberOfDataPoints();

    mzIntensity[0] = new double[numDp];
    mzIntensity[1] = new double[numDp];
    getMzValues(mzIntensity[0]);
    getIntensityValues(mzIntensity[1]);

    DataPoint[] dps = new DataPoint[numDp];
    for (int i = 0; i < numDp; i++) {
      dps[i] = new SimpleDataPoint(mzIntensity[0][i], mzIntensity[1][i]);
    }

    return dps;
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);

    writer.writeStartElement(CONST.XML_MZ_VALUES_ELEMENT);
    writer.writeCharacters(
        ParsingUtils.doubleArrayToString(DataPointUtils.getDoubleBufferAsArray(mzValues)));
    writer.writeEndElement();
    writer.writeStartElement(CONST.XML_INTENSITY_VALUES_ELEMENT);
    writer.writeCharacters(
        ParsingUtils.doubleArrayToString(DataPointUtils.getDoubleBufferAsArray(intensityValues)));
    writer.writeEndElement();

    writer.writeEndElement();
  }

  public static MassList loadFromXML(XMLStreamReader reader, @Nullable MemoryMapStorage storage)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Wrong element.");
    }

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

    return new SimpleMassList(storage, mzs, intensities);
  }
}
