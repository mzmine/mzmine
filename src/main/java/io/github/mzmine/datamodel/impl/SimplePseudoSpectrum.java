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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimplePseudoSpectrum extends SimpleScan implements PseudoSpectrum {

  public static final String XML_SCAN_TYPE = "simplepseudospectrum";
  protected final PseudoSpectrumType pseudoSpectrumType;

  public SimplePseudoSpectrum(@NotNull RawDataFile dataFile, int msLevel, float retentionTime,
      @Nullable MsMsInfo msMsInfo, double[] mzValues, double[] intensityValues,
      @NotNull PolarityType polarity, @Nullable String scanDefinition,
      @NotNull final PseudoSpectrumType type) {

    super(dataFile, -1, msLevel, retentionTime, msMsInfo, mzValues, intensityValues,
        MassSpectrumType.CENTROIDED, polarity, scanDefinition,
        Range.closed(mzValues[0], mzValues[mzValues.length - 1]));
    // since this is a pseudo spectrum, directly add a mass list. Noise thresholding and every
    // other processing step before this spectrum's creation should have filtered the noise.
    addMassList(new ScanPointerMassList(this));
    this.pseudoSpectrumType = type;
  }

  @Override
  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(Scan.XML_SCAN_ELEMENT);

    writer.writeAttribute(Scan.XML_SCAN_TYPE_ATTR, XML_SCAN_TYPE);
    writer.writeAttribute(CONST.XML_MSLEVEL_ATTR, String.valueOf(getMSLevel()));
    writer.writeAttribute(CONST.XML_RT_ATTR, String.valueOf(getRetentionTime()));
    writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, getDataFile().getName());
    writer.writeAttribute(CONST.XML_POLARITY_ATTR, getPolarity().name());
    writer.writeAttribute(XML_PSEUDO_SPECTRUM_TYPE_ATTR, pseudoSpectrumType.name());
    writer.writeAttribute(CONST.XML_SCAN_DEF_ATTR,
        ParsingUtils.parseNullableString(getScanDefinition()));
    if (getMsMsInfo() != null) {
      getMsMsInfo().writeToXML(writer);
    }

    writer.writeStartElement(CONST.XML_MZ_VALUES_ELEMENT);
    writer.writeCharacters(ParsingUtils.doubleBufferToString(getMzValues()));
    writer.writeEndElement();

    writer.writeStartElement(CONST.XML_INTENSITY_VALUES_ELEMENT);
    writer.writeCharacters(ParsingUtils.doubleBufferToString(getIntensityValues()));
    writer.writeEndElement();

    writer.writeEndElement();
  }

  @Override
  public PseudoSpectrumType getPseudoSpectrumType() {
    return pseudoSpectrumType;
  }

  public static PseudoSpectrum loadFromXML(XMLStreamReader reader, RawDataFile file)
      throws XMLStreamException {
    if (!reader.isStartElement() || !reader.getLocalName().equals(Scan.XML_SCAN_ELEMENT)
        || !reader.getAttributeValue(null, Scan.XML_SCAN_TYPE_ATTR).equals(XML_SCAN_TYPE)) {
      throw new IllegalStateException("Wrong scan type.");
    }

    final int mslevel = Integer.parseInt(reader.getAttributeValue(null, CONST.XML_MSLEVEL_ATTR));
    final float rt = Float.parseFloat(reader.getAttributeValue(null, CONST.XML_RT_ATTR));
    final PseudoSpectrumType type = PseudoSpectrumType.valueOf(
        reader.getAttributeValue(null, XML_PSEUDO_SPECTRUM_TYPE_ATTR));
    final PolarityType polarity = PolarityType.valueOf(
        reader.getAttributeValue(null, CONST.XML_POLARITY_ATTR));
    final String scanDef = ParsingUtils.readNullableString(
        reader.getAttributeValue(null, CONST.XML_SCAN_DEF_ATTR));
    assert file.getName().equals(reader.getAttributeValue(null, CONST.XML_RAW_FILE_ELEMENT));

    double[] mzs = null;
    double[] intensities = null;
    MsMsInfo info = null;
    while (reader.hasNext()) {
      int next = reader.next();
      if (next == XMLEvent.END_ELEMENT && reader.getLocalName().equals(Scan.XML_SCAN_ELEMENT)) {
        break;
      }
      if (next != XMLEvent.START_ELEMENT) {
        continue;
      }
      switch (reader.getLocalName()) {
        case CONST.XML_MZ_VALUES_ELEMENT ->
            mzs = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case CONST.XML_INTENSITY_VALUES_ELEMENT ->
            intensities = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case MsMsInfo.XML_ELEMENT -> info = MsMsInfo.loadFromXML(reader, file, List.of(file));
      }
    }

    assert mzs != null && intensities != null;
    return new SimplePseudoSpectrum(file, mslevel, rt, info, mzs, intensities, polarity, scanDef,
        type);
  }

}
