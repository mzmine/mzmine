package io.github.mzmine.datamodel.features.compoundannotations;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.Collection;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FeatureAnnotation {

  public static final String XML_ELEMENT = "featureannotation";
  public static final String XML_TYPE_ATTR = "annotationtype";

  public void saveToXML(@NotNull XMLStreamWriter writer, ModularFeatureList flist,
      ModularFeatureListRow row) throws XMLStreamException;

  public static FeatureAnnotation loadFromXML(XMLStreamReader reader,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Current element is not a feature annotation element");
    }

    return switch (reader.getAttributeValue(null, XML_TYPE_ATTR)) {
      case SpectralDBAnnotation.XML_ATTR -> SpectralDBAnnotation.loadFromXML(reader, possibleFiles);
      default -> null;
    };
  }

  @Nullable Double getPrecursorMZ();

  @Nullable String getSmiles();

  @Nullable String getCompoundName();

  @Nullable String getFormula();

  @Nullable IonType getAdductType();

  @Nullable Float getMobility();

  @Nullable Float getCCS();

  @Nullable Float getRT();

  @Nullable Float getScore();

  @Nullable String getDatabase();

}
