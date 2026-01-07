/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.datamodel.features.types.otherdectectors;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.Objects;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OtherFileType extends DataType<OtherDataFile> {

  @Override
  public @NotNull String getUniqueID() {
    return "other_file";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Other file";
  }

  @Override
  public Property<OtherDataFile> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<OtherDataFile> getValueClass() {
    return OtherDataFile.class;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value == null || file == null || !(value instanceof OtherDataFile other)) {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
      return;
    }

    writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, file.getFileName());
    writer.writeCharacters(other.getDescription());
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)
        && reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR).equals(getUniqueID()))) {
      throw new IllegalStateException("Wrong element");
    }

    final String rawFileName = reader.getAttributeValue(null, CONST.XML_RAW_FILE_ELEMENT);
    final String otherFileDescription = reader.getElementText();

    if (rawFileName == null || otherFileDescription == null || file == null) {
      return null;
    }
    final var loadedFile = project.getCurrentRawDataFiles().stream()
        .filter(f -> f.getFileName().equals(rawFileName)).findFirst().orElse(null);
    assert Objects.equals(loadedFile, file);

    return OtherDataFile.findInRawFile(file, otherFileDescription);
  }
}
