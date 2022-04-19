package io.github.mzmine.datamodel.features.types.annotations.compounddb.sirius;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseMatchInfoType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SiriusAnnotationListType extends ListWithSubsType<CompoundDBAnnotation> implements
    AnnotationType {

  @Override
  public @NotNull String getUniqueID() {
    return "sirius_annotations";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Sirius";
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return List.of(new SiriusAnnotationListType(), new CompoundNameType(), new IonTypeType(),
        new SiriusRankType(), new SiriusConfidenceScoreType(), new SiriusFingerIdScoreType(),
        new SiriusZodiacScore(), new SmilesStructureType(), new DatabaseMatchInfoType(),
        new FormulaType(), new SiriusIdType());
  }

  @Override
  protected Map<Class<? extends DataType>, Function<CompoundDBAnnotation, Object>> getMapper() {
    return Map.ofEntries(//
        createEntry(SiriusAnnotationListType.class, CompoundDBAnnotation::getCompoundName), //
        createEntry(CompoundNameType.class, CompoundDBAnnotation::getCompoundName),//
        createEntry(IonTypeType.class, CompoundDBAnnotation::getAdductType), //
        createEntry(SiriusRankType.class, c -> c.get(SiriusRankType.class)),//
        createEntry(SiriusConfidenceScoreType.class, c -> c.get(SiriusConfidenceScoreType.class)),//
        createEntry(SiriusFingerIdScoreType.class, c -> c.get(SiriusFingerIdScoreType.class)),//
        createEntry(SiriusZodiacScore.class, c -> c.get(SiriusZodiacScore.class)),//
        createEntry(SmilesStructureType.class, c -> c.get(SmilesStructureType.class)),//
        createEntry(DatabaseMatchInfoType.class, c -> c.get(DatabaseMatchInfoType.class)),//
        createEntry(FormulaType.class, CompoundDBAnnotation::getFormula),//
        createEntry(SiriusIdType.class, c -> c.get(SiriusIdType.class)));
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value == null) {
      return;
    }

    if (!(value instanceof List<?> list)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
              + value.getClass());
    }

    for (Object o : list) {
      if (!(o instanceof CompoundDBAnnotation id)) {
        continue;
      }
      id.saveToXML(writer, flist, row);
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)
        && reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR).equals(getUniqueID()))) {
      throw new IllegalStateException("Wrong element");
    }

    final List<CompoundDBAnnotation> ids = new ArrayList<>();

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(FeatureAnnotation.XML_ELEMENT)) {
        final FeatureAnnotation id = FeatureAnnotation.loadFromXML(reader, flist, row);
        if (id instanceof CompoundDBAnnotation sid) {
          ids.add(sid);
        }
      }
    }

    // never return null, if this type was saved we even need empty lists.
    return ids;
  }
}
