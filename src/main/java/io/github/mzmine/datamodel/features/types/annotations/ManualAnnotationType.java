/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ModularType;
import io.github.mzmine.datamodel.features.types.ModularTypeMap;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManualAnnotationType extends ModularType implements AnnotationType {

  // Unmodifiable list of all subtypes
  private final List<DataType> subTypes = List.of(new IdentityType(), new CommentType(),
      new CompoundNameType(), new IonAdductType(), new FormulaType(), new SmilesStructureType());

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
  public ModularTypeMap createProperty() {
    final ModularTypeMap property = super.createMap();

    property.get(IdentityType.class)
        .addListener((ListChangeListener<? super FeatureIdentity>) change -> {
          ObservableList<? extends FeatureIdentity> list = change.getList();
          boolean firstElementChanged = false;
          while (change.next()) {
            firstElementChanged = firstElementChanged || change.getFrom() == 0;
          }
          if (firstElementChanged) {
            // first list elements has changed - set all other fields
            setCurrentElement(property, list.isEmpty() ? null : list.get(0));
          }
        });

    // set listeners to update stored values if changes occur from the user
    property.get(new CommentType()).addListener((observable, oldValue, newValue) -> {
      if(newValue == null) {
        return;
      }
      ((SimpleFeatureIdentity) property.get(new IdentityType()).get(0)).setPropertyValue(
          FeatureIdentity.PROPERTY_COMMENT, newValue);
    });

    property.get(new CompoundNameType()).addListener((observable, oldValue, newValue) -> {
      if(newValue == null) {
        return;
      }
      ((SimpleFeatureIdentity) property.get(new IdentityType()).get(0)).setPropertyValue(
          FeatureIdentity.PROPERTY_NAME, newValue);
    });

    property.get(new IonAdductType()).addListener((observable, oldValue, newValue) -> {
      if(newValue == null) {
        return;
      }
      ((SimpleFeatureIdentity) property.get(new IdentityType()).get(0)).setPropertyValue(
          FeatureIdentity.PROPERTY_ADDUCT, newValue);
    });

    property.get(new FormulaType()).addListener((observable, oldValue, newValue) -> {
      if(newValue == null) {
        return;
      }
      ((SimpleFeatureIdentity) property.get(new IdentityType()).get(0)).setPropertyValue(
          FeatureIdentity.PROPERTY_FORMULA, newValue);
    });

    property.get(new SmilesStructureType()).addListener((observable, oldValue, newValue) -> {
      if(newValue == null) {
        return;
      }
      ((SimpleFeatureIdentity) property.get(new IdentityType()).get(0)).setPropertyValue(
          FeatureIdentity.PROPERTY_SMILES, newValue);
    });

    return property;
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
    while (!(reader.isStartElement() && reader.getLocalName()
        .equals(FeatureIdentity.XML_GENERAL_IDENTITY_ELEMENT)) && reader.hasNext()) {
      reader.next();
      if ((reader.isEndElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT))) {
        // do not overshoot the current element.
        return null;
      }
    }

    List<FeatureIdentity> ids = new ArrayList<>();
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      if (reader.isStartElement() && reader.getLocalName()
          .equals(FeatureIdentity.XML_GENERAL_IDENTITY_ELEMENT)) {
        var id = FeatureIdentity.loadFromXML(reader, flist.getRawDataFiles());
        ids.add(id);
      }
      reader.next();
    }
    ModularTypeMap property = createProperty();
    property.set(IdentityType.class, ids);
    return property;
  }

  private void setCurrentElement(ModularTypeMap data, FeatureIdentity identity) {
    if (identity == null) {
      for (DataType type : this.getSubDataTypes()) {
        if (!(type instanceof IdentityType)) {
          data.set(type, null);
        }
      }
    } else {
      data.set(CommentType.class, identity.getPropertyValue(FeatureIdentity.PROPERTY_COMMENT));
      data.set(CompoundNameType.class, identity.getName());
      data.set(IonAdductType.class, identity.getPropertyValue(FeatureIdentity.PROPERTY_ADDUCT));
      data.set(FormulaType.class, identity.getPropertyValue(FeatureIdentity.PROPERTY_FORMULA));
      data.set(SmilesStructureType.class,
          identity.getPropertyValue(FeatureIdentity.PROPERTY_SMILES));
    }
  }
}
