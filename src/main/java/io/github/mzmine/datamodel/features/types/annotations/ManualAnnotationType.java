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
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeTableColumn;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManualAnnotationType extends DataType<ManualAnnotation> implements SubColumnsFactory,
    AnnotationType {

  private static final Logger logger = Logger.getLogger(ManualAnnotationType.class.getName());

  // Unmodifiable list of all subtypes
  private static final List<DataType> subTypes = List.of(new IdentityType(), new CommentType(),
      new CompoundNameType(), new IonAdductType(), new FormulaType(), new InChIStructureType(),
      new SmilesStructureType());

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "manual_annotation";
  }

  @NotNull
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Manual annotation";
  }

  @Override
  public Property<ManualAnnotation> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value == null) {
      return;
    }
    if (!(value instanceof ManualAnnotation manual)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
          + value.getClass());
    }

    for (int i = 0; i < subTypes.size(); i++) {
      DataType sub = subTypes.get(i);
      Object subValue = getSubColValue(sub, manual);
      if (subValue != null) {
        writer.writeStartElement(CONST.XML_DATA_TYPE_ELEMENT);
        writer.writeAttribute(CONST.XML_DATA_TYPE_ID_ATTR, sub.getUniqueID());

        try { // catch here, so we can easily debug and don't destroy the flist while saving in case an unexpected exception happens
          sub.saveToXML(writer, subValue, flist, row, feature, file);
        } catch (XMLStreamException e) {
          final Object finalVal = subValue;
          logger.warning(
              () -> "Error while writing data type " + sub.getClass().getSimpleName()
                    + " with value "
                    + finalVal + " to xml.");
          e.printStackTrace();
        }

        writer.writeEndElement();
      }
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {
    ManualAnnotation manual = null;
    while (reader.hasNext()) {
      int next = reader.next();

      if (next == XMLEvent.END_ELEMENT && reader.getLocalName()
          .equals(CONST.XML_DATA_TYPE_ELEMENT)) {
        break;
      }
      if (reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)) {
        DataType type = DataTypes.getTypeForId(
            reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
        Object o = type.loadFromXML(reader, flist, row, feature, file);
        if (manual == null) {
          manual = new ManualAnnotation();
        }
        manual.set(type, o);
      }
    }
    return manual;
  }

  @Override
  public Class<ManualAnnotation> getValueClass() {
    return ManualAnnotation.class;
  }

  @Override
  public @NotNull List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType) {
    // add column for each sub data type
    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();

    List<DataType> subTypes = getSubDataTypes();
    // create column per name
    for (int index = 0; index < getNumberOfSubColumns(); index++) {
      DataType type = subTypes.get(index);
      if (this.getClass().isInstance(type)) {
        // create a special column for this type that actually represents the list of data
        cols.add(DataType.createStandardColumn(type, raw, this, index));
      } else {
        // create all other columns
        var col = type.createColumn(raw, this, index);
        // override type in CellValueFactory with this parent type
        cols.add(col);
      }
    }
    return cols;
  }

  @Override
  public <T> void valueChanged(ModularDataModel model, DataType<T> subType, int subColumnIndex,
      T newValue) {
    try {
      ManualAnnotation manual = Objects.requireNonNullElse(model.get(this), new ManualAnnotation());

      if (subType.getClass().equals(CommentType.class)) {
        manual.setComment((String) newValue);
      } else if (subType.getClass().equals(SmilesStructureType.class)) {
        manual.setSmiles((String) newValue);
      } else if (subType.getClass().equals(InChIStructureType.class)) {
        manual.setInchi((String) newValue);
      } else if (subType.getClass().equals(IdentityType.class)) {
        List<FeatureIdentity> identities = Objects
            .requireNonNullElse(manual.getIdentities(), new ArrayList<>());
        identities.remove(newValue);
        identities.add(0, (FeatureIdentity) newValue);
        manual.setIdentities(identities);
      } else if (subType.getClass().equals(FormulaType.class)) {
        manual.setFormula((String) newValue);
      } else if (subType.getClass().equals(IonAdductType.class)) {
        manual.setIon((String) newValue);
      } else if (subType.getClass().equals(CompoundNameType.class)) {
        manual.setCompoundName((String) newValue);
      }
      // finally set annotation
      model.set(ManualAnnotationType.class, manual);
    } catch (Exception ex) {
      logger.log(Level.WARNING, () -> String.format(
          "Cannot handle change in subtype %s at index %d in parent type %s with new value %s",
          subType.getClass().getName(), subColumnIndex, this.getClass().getName(), newValue));
    }
  }

  @Override
  public int getNumberOfSubColumns() {
    return subTypes.size();
  }

  @Override
  public @Nullable String getHeader(int subcolumn) {
    List<DataType> list = getSubDataTypes();
    if (subcolumn >= 0 && subcolumn < list.size()) {
      return list.get(subcolumn).getHeaderString();
    } else {
      throw new IndexOutOfBoundsException(
          "Sub column index " + subcolumn + " is out of range " + list.size());
    }
  }

  @Override
  public @Nullable String getUniqueID(int subcolumn) {
    List<DataType> list = getSubDataTypes();
    if (subcolumn >= 0 && subcolumn < list.size()) {
      return list.get(subcolumn).getUniqueID();
    } else {
      throw new IndexOutOfBoundsException(
          "Sub column index " + subcolumn + " is out of range " + list.size());
    }
  }


  @Override
  public @NotNull DataType<?> getType(int index) {
    if (index < 0 || index >= getSubDataTypes().size()) {
      throw new IndexOutOfBoundsException(
          String.format("Sub column index %d is out of bounds %d", index,
              getSubDataTypes().size()));
    }
    return getSubDataTypes().get(index);
  }

  @Override
  public @Nullable String getFormattedSubColValue(int subcolumn, Object value) {
    DataType sub = getType(subcolumn);
    if (sub == null) {
      return "";
    }
    if (value == null) {
      return sub.getFormattedString(sub.getDefaultValue());
    }

    Object subvalue = null;
    try {
      subvalue = getSubColValue(sub, value);
      return sub.getFormattedString(subvalue == null ? sub.getDefaultValue() : subvalue);
    } catch (Exception ex) {
      logger.log(Level.WARNING, String.format(
          "Error while formatting sub column value in type %s. Sub type %s cannot format value of %s",
          this.getClass().getName(), sub.getClass().getName(),
          (subvalue == null ? "null" : subvalue.getClass())), ex);
      return "";
    }
  }

  @Override
  public @Nullable Object getSubColValue(int subcolumn, Object cellData) {
    DataType sub = getType(subcolumn);
    return sub == null ? null : getSubColValue(sub, cellData);
  }

  public @Nullable Object getSubColValue(DataType sub, Object value) {
    if (value == null) {
      return null;
    } else if (value instanceof ManualAnnotation man) {
      return man.get(sub);
    } else {
      throw new IllegalArgumentException(String
          .format("value of type %s needs to be of type manual annotation",
              value.getClass().getName()));
    }
  }
}
