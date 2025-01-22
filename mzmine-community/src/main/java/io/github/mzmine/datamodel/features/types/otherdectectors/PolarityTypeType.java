package io.github.mzmine.datamodel.features.types.otherdectectors;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PolarityTypeType extends DataType<PolarityType> implements NullColumnType,
    NoTextColumn {

  @Override
  public @NotNull String getUniqueID() {
    return "polarity";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Polarity";
  }

  @Override
  public Property<PolarityType> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<PolarityType> getValueClass() {
    return PolarityType.class;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if(value == null) {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
      return;
    }

    if(value instanceof PolarityType pol) {
      writer.writeCharacters(pol.name());
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    final String text = ParsingUtils.readNullableString(reader.getElementText());
    if(text == null) {
      return null;
    }
    return PolarityType.valueOf(text);
  }
}
