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

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Represents a transition during MRM experiments in triple quad analysis. Note that the
 * chromatograms in this {@link MrmTransition} are typically not resolved in rt dimension, but only
 * smoothed and baseline corrected. This helps during reintegration because smoothing and
 * bl-correction would have to be re-applied when changing the rt boundaries. The actual feature can
 * be obtained using {@link io.github.mzmine.datamodel.featuredata.MrmUtils#getChromInRtRange(FeatureListRow, RawDataFile, MrmTransition)}
 *
 * @param chromatogram The individual transitions. Important note: the masses in the
 *                     {@link IonTimeSeries} must be the q1 mass.
 */
public record MrmTransition(double q1mass, double q3mass,
                            IonTimeSeries<? extends Scan> chromatogram) {

  public static final String XML_ATTRIBUTE_MRM_Q1_MASS = "q1mass";
  public static final String XML_ATTRIBUTE_MRM_Q3_MASS = "q3mass";
  public static final String XML_ELEMENT = "mrmtransition";

  public MrmTransition(double q1mass, double q3mass, IonTimeSeries<? extends Scan> chromatogram) {
    this.q1mass = q1mass;
    this.q3mass = q3mass;
    this.chromatogram = (IonTimeSeries<Scan>) chromatogram;

    checkChromatogramMasses(q1mass, chromatogram);
  }

  private static void checkChromatogramMasses(double q1mass,
      IonTimeSeries<? extends Scan> chromatogram) {
    if (chromatogram.getNumberOfValues() != 0
        && Double.compare(chromatogram.getMZ(0), q1mass) != 0) {
      // this must be the case to not alter the feature m/z if the {@link FeatureDataType} is updated.
      throw new IllegalArgumentException(
          "The m/zs in this chromatogram are not equal to the q1 mass.");
    }
  }

  public static MrmTransition loadFromXML(XMLStreamReader reader, ModularFeatureList flist,
      RawDataFile file) throws XMLStreamException {
    if (!reader.isStartElement() || !XML_ELEMENT.equals(reader.getLocalName())) {
      throw new RuntimeException("Wrong element");
    }

    final double q1Mass = Double.parseDouble(
        reader.getAttributeValue(null, XML_ATTRIBUTE_MRM_Q1_MASS));
    final double q3Mass = Double.parseDouble(
        reader.getAttributeValue(null, XML_ATTRIBUTE_MRM_Q3_MASS));
    IonTimeSeries<? extends Scan> chromatogram = null;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
      reader.next();

      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.isStartElement() && (reader.getLocalName().equals(SimpleIonTimeSeries.XML_ELEMENT)
          || reader.getLocalName().equals(SimpleIonMobilogramTimeSeries.XML_ELEMENT))) {
        // found start element
        chromatogram = switch (reader.getLocalName()) {
          case SimpleIonTimeSeries.XML_ELEMENT ->
              SimpleIonTimeSeries.loadFromXML(reader, flist.getMemoryMapStorage(), file);
          case SimpleIonMobilogramTimeSeries.XML_ELEMENT ->
              IonMobilogramTimeSeriesFactory.loadFromXML(reader, flist.getMemoryMapStorage(),
                  (IMSRawDataFile) file);
          default ->
              throw new RuntimeException("Unknown IonTimeSeries while loading MrmTransition");
        };
        break;
      }
    }

    if (chromatogram == null) {
      throw new RuntimeException("No chromatogram found while parsing MrmTransition");
    }

    return new MrmTransition(q1Mass, q3Mass, chromatogram);
  }

  @Override
  public String toString() {
    return "%.2f -> %.2f".formatted(q1mass, q3mass);
  }

  public MrmTransition with(IonTimeSeries<? extends Scan> chromatogram) {
    checkChromatogramMasses(q1mass, chromatogram);
    return new MrmTransition(q1mass, q3mass, chromatogram);
  }

  public boolean sameIsolations(MrmTransition other) {
    return Double.compare(q1mass, other.q1mass) == 0 && Double.compare(q3mass, other.q3mass) == 0;
  }

  public void saveToXML(XMLStreamWriter writer, List<? extends Scan> allScansOfDataFile)
      throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(XML_ATTRIBUTE_MRM_Q1_MASS, String.valueOf(q1mass));
    writer.writeAttribute(XML_ATTRIBUTE_MRM_Q3_MASS, String.valueOf(q3mass));
    chromatogram.saveValueToXML(writer, (List) allScansOfDataFile);

    // XML_ELEMENT
    writer.writeEndElement();
  }
}
