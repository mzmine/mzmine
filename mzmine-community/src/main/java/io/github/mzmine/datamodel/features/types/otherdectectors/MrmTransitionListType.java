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
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.otherdetectors.MrmTransition;
import io.github.mzmine.datamodel.otherdetectors.MrmTransitionList;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.stream.Collectors;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MrmTransitionListType extends DataType<MrmTransitionList> {

  public MrmTransitionListType() {
    super();
  }

  @Override
  public @NotNull String getUniqueID() {
    return "mrm_transition_list";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "MRM transitions";
  }

  @Override
  public Property<MrmTransitionList> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<MrmTransitionList> getValueClass() {
    return MrmTransitionList.class;
  }

  @Override
  public @NotNull String getFormattedString(MrmTransitionList list, boolean export) {
    return list == null ? "" : list.transitions().stream()
        .map(t -> t.toString() + (t == list.quantifier() ? " (quant)" : ""))
        .collect(Collectors.joining(", ")).toString();
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!reader.isStartElement() || !getUniqueID().equals(
        reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR))) {
      throw new RuntimeException("Wrong element");
    }

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(MrmTransitionList.XML_ELEMENT)) {
        return MrmTransitionList.loadFromXML(reader, flist, file);
      }
    }

    return null;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (file == null || feature == null || !(value instanceof MrmTransitionList mrm)) {
      return;
    }

    mrm.saveToXML(writer, file.getScans());
  }
}
