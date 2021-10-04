/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ModularType;
import io.github.mzmine.datamodel.features.types.ModularTypeProperty;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManualAnnotationType extends ModularType implements AnnotationType {

  // Unmodifiable list of all subtypes
  private final List<DataType> subTypes = List
      .of(new IdentityType(), new CommentType(), new CompoundNameType(), new IonAdductType(),
          new FormulaType(), new SmilesStructureType());

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "manual_annotation";
  }

  @NotNull
  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Manual annotation";
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!(value instanceof Map map)) {
      return;
    }

    for (Object o : map.entrySet()) {
      if (!(o instanceof Map.Entry entry)) {
        continue;
      }

      if (entry.getKey() instanceof IdentityType) {
        List<FeatureIdentity> ids = (List<FeatureIdentity>) entry.getValue();
        for (FeatureIdentity id : ids) {
          id.saveToXML(writer);
        }
      }
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {
    while (!(reader.isStartElement() && reader.getLocalName().equals(FeatureIdentity.XML_ELEMENT))
        && reader.hasNext()) {
      reader.next();
      if ((reader.isEndElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT))) {
        // do not overshoot the current element.
        return null;
      }
    }

    List<FeatureIdentity> ids = new ArrayList<>();
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      if (reader.isStartElement() && reader.getLocalName().equals(FeatureIdentity.XML_ELEMENT)) {
        var id = SimpleFeatureIdentity.loadFromXML(reader);
        ids.add(id);
      }
      reader.next();
    }
    ModularTypeProperty property = createProperty();
    property.set(IdentityType.class, ids);
    return property;
  }
}
