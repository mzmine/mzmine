package io.github.mzmine.datamodel.otherdetectors;

import com.alanmrace.jimzmlparser.mzml.Run;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.XMLUtils;
import java.io.File;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.math.io.files.DataFile;

/**
 * @param chromatogram The individidual transitions. Important note: the masses in the
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
    chromatogram.saveValueToXML(writer, (List)allScansOfDataFile);

    // XML_ELEMENT
    writer.writeEndElement();
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
}
