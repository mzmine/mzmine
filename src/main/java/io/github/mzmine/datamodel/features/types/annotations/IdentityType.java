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

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.AddElementDialog;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.StringParser;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import javafx.util.StringConverter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IdentityType extends ListDataType<FeatureIdentity> implements AnnotationType,
    EditableColumnType, AddElementDialog, StringParser<FeatureIdentity> {

  private static StringConverter<FeatureIdentity> converter = new StringConverter<FeatureIdentity>() {
    @Override
    public String toString(FeatureIdentity object) {
      return object.toString();
    }

    @Override
    public FeatureIdentity fromString(String name) {
      return new SimpleFeatureIdentity(name);
    }
  };

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "identity";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Identity";
  }

  @Override
  public FeatureIdentity fromString(String s) {
    return converter.fromString(s);
  }

  @Override
  public StringConverter<FeatureIdentity> getStringConverter() {
    return converter;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if(value == null) {
      return;
    }
    if (!(value instanceof List ids)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: " + value.getClass());
    }

    for (Object obj : ids) {
      if (obj instanceof FeatureIdentity id) {
        id.saveToXML(writer);
      }
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    while (!(reader.isStartElement() && reader.getLocalName()
        .equals(FeatureIdentity.XML_GENERAL_IDENTITY_ELEMENT)) && reader.hasNext()) {
      if ((reader.isEndElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT))) {
        // do not overshoot the current element.
        return null;
      }
      reader.next();
    }

    List<FeatureIdentity> ids = new ArrayList<>();
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      if (reader.isStartElement() && reader.getLocalName()
          .equals(FeatureIdentity.XML_GENERAL_IDENTITY_ELEMENT) && reader
          .getAttributeValue(null, FeatureIdentity.XML_IDENTITY_TYPE_ATTR)
          .equals(SimpleFeatureIdentity.XML_IDENTITY_TYPE)) {
        var id = SimpleFeatureIdentity.loadFromXML(reader);
        ids.add(id);
      }
      reader.next();
    }
    return ids;
  }

  @Override
  public boolean getDefaultVisibility() {
    return false;
  }
}
