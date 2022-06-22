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

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellFactory;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellValueFactory;
import io.github.mzmine.datamodel.features.types.fx.EditComboCellFactory;
import io.github.mzmine.datamodel.features.types.fx.EditableDataTypeCellFactory;
import io.github.mzmine.datamodel.features.types.modifiers.AddElementDialog;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.StringParser;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class of data types: Provides formatters. Should be added to one {@link ModularDataModel}
 *
 * @param <T>
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public abstract class DataType<T> {

  private static final Logger logger = Logger.getLogger(DataType.class.getName());

  public DataType() {
  }

  /**
   * Creates a standard column and handles editable columns
   *
   * @param <T>
   * @param type
   * @param raw
   * @param parentType
   * @return
   */
  public static <T> TreeTableColumn<ModularFeatureListRow, Object> createStandardColumn(
      @NotNull DataType<T> type, @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType,
      int subColumnIndex) {
    TreeTableColumn<ModularFeatureListRow, Object> col = new TreeTableColumn<>(
        type.getHeaderString());
    col.setUserData(type);
    col.setSortable(true);

    // define observable
    col.setCellValueFactory(new DataTypeCellValueFactory(raw, type, parentType, subColumnIndex));
    // value representation
    if (type instanceof EditableColumnType) {
      col.setCellFactory(type.getEditableCellFactory(raw, parentType, subColumnIndex));
      col.setEditable(true);
      col.setOnEditCommit(event -> {
        Object data = event.getNewValue();
        if (data != null) {
          ModularFeatureListRow row = event.getRowValue().getValue();
          ModularDataModel model = raw == null ? row : row.getFeature(raw);

          // if parent type is different than type - parent type will handle the value change
          // e.g. see io.github.mzmine.datamodel.features.types.ListWithSubsType
          if (type instanceof ListDataType && type instanceof AddElementDialog addDialog
              && data instanceof String && AddElementDialog.BUTTON_TEXT.equals(data)) {
            addDialog.createNewElementDialog(model, parentType, type, subColumnIndex,
                (newElement) -> { // refresh table due to change
                  col.getTreeTableView().refresh();
                });
          } else if (parentType != null && !parentType.equals(type)) {
            parentType.valueChanged(model, (DataType) type, subColumnIndex, data);
            col.getTreeTableView().refresh();
          } else {
            if (type instanceof ListDataType) {
              try {
                List list = (List) model.get(type);
                if (list != null) {
                  list = new ArrayList<>(list);
                  list.remove(data);
                  list.add(0, (T) data);
                  model.set((DataType) type, list);
                }
              } catch (Exception ex) {
                logger.log(Level.SEVERE,
                    "Cannot set value from table cell to data type: " + type.getHeaderString());
                logger.log(Level.SEVERE, ex.getMessage(), ex);
              }
            } else {
              // TODO check if this cast is safe
              model.set(type, (T) data);
            }
          }
        }
        event.getTreeTableView().refresh();
      });
    } else {
      col.setCellFactory(new DataTypeCellFactory(raw, type, parentType, subColumnIndex));
    }
    return col;
  }

  /**
   * A unique ID that is used to store and retrieve data types. This value should never be changed
   * after introducing a new type to retain backwards compatibility. (even if the class name or
   * string representation changes)
   *
   * @return a unique identifier
   */
  @NotNull
  public abstract String getUniqueID();

  /**
   * A formatted string representation of the value
   *
   * @return the formatted representation of the value (or an empty String)
   */
  @NotNull
  public String getFormattedString(T value) {
    return value != null ? value.toString() : "";
  }

  /**
   * A formatted string representation of the value, if value is instance of {@link
   * #getValueClass()}.
   *
   * @return the formatted representation of the value (or an empty String)
   */
  @NotNull
  public String getFormattedStringCheckType(Object value) {
    if (value == null) {
      return getFormattedString(null);
    } else if (getValueClass().isInstance(value)) {
      return getFormattedString(getValueClass().cast(value));
    } else {
      throw new IllegalArgumentException("value is not ValueClass: " + getValueClass().toString());
    }
  }

  /**
   * The header string (name) of this data type
   *
   * @return
   */
  @NotNull
  public abstract String getHeaderString();

  /**
   * The default value for this data type
   *
   * @return the default value (null for most types)
   */
  public @Nullable T getDefaultValue() {
    return null;
  }

  /**
   * Creates a TreeTableColumn or null if the value is not represented in a column. A {@link
   * SubColumnsFactory} DataType can also add multiple sub columns to the main column generated by
   * this class.
   *
   * @param raw        null if this is a FeatureListRow column. For Feature columns: the raw data
   *                   file specifies the feature.
   * @param parentType if this type is a sub type of modularParentType (or null): Changes the
   *                   CellFactory for editable cells and the CellValueFactory
   * @return the TreeTableColumn or null if this DataType.value is not represented in a column
   */
  @Nullable
  public TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      final @Nullable RawDataFile raw, final @Nullable SubColumnsFactory parentType) {
    return createColumn(raw, parentType, -1);
  }

  /**
   * Creates a TreeTableColumn or null if the value is not represented in a column. A {@link
   * SubColumnsFactory} DataType can also add multiple sub columns to the main column generated by
   * this class.
   *
   * @param raw        null if this is a FeatureListRow column. For Feature columns: the raw data
   *                   file specifies the feature.
   * @param parentType if this type is a sub type of modularParentType (or null): Changes the
   *                   CellFactory for editable cells and the CellValueFactory
   * @return the TreeTableColumn or null if this DataType.value is not represented in a column
   */
  @Nullable
  public TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      final @Nullable RawDataFile raw, final @Nullable SubColumnsFactory parentType,
      int subColumnIndex) {
    if (this instanceof NullColumnType) {
      return null;
    }
    // create column
    if (this instanceof SubColumnsFactory sub) {
      TreeTableColumn<ModularFeatureListRow, Object> col = new TreeTableColumn<>(getHeaderString());
      col.setUserData(this);

      col.setSortable(false);
      // add sub columns
      var children = sub.createSubColumns(raw, parentType);
      col.getColumns().addAll(children);
      return col;
    } else {
      // create a standard column and handle editable columns
      return createStandardColumn(this, raw, parentType, subColumnIndex);
    }
  }

  protected Callback<TreeTableColumn<ModularFeatureListRow, Object>, TreeTableCell<ModularFeatureListRow, Object>> getEditableCellFactory(
      RawDataFile raw, SubColumnsFactory parentType, int subColumnIndex) {
    if (this instanceof ListDataType) {
      return new EditComboCellFactory(raw, this, parentType, subColumnIndex);
    } else if (this instanceof StringParser<?>) {
      return new EditableDataTypeCellFactory(this);
    } else {
      throw new UnsupportedOperationException(
          "Programming error: No edit CellFactory for " + "data type: " + this.getHeaderString()
              + " class " + this.getClass().toString());
    }
  }

  // TODO dirty hack to make this a "singleton"
  @Override
  public boolean equals(Object obj) {
    return obj != null && obj.getClass().equals(this.getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  /**
   * Creating a property which is used in a {@link ModularDataModel}
   *
   * @return
   */
  public abstract Property<T> createProperty();


  /**
   * In case this DataType is added to a {@link ModularFeature}, these row bindings are added to the
   * {@link ModularFeatureList} to automatically calculate or visualize summary datatypes in a row
   *
   * @return
   */
  @NotNull
  public List<RowBinding> createDefaultRowBindings() {
    return List.of();
  }

  /**
   * @param row       The row the double click was applied to.
   * @param file      The file the click was applied to. Either multiple (= row clicked, or a single
   *                  = feature column clicked)
   * @param superType The super type of the clicked daty type or null.
   * @param value     The cell value or null.
   * @return A runnable for execution. Must be explicitly executed on fx thread if the gui is
   * modified.
   */
  @Nullable
  public Runnable getDoubleClickAction(@NotNull ModularFeatureListRow row,
      @NotNull List<RawDataFile> file, @Nullable DataType<?> superType,
      @Nullable final Object value) {
    return null;
  }

  @Override
  public String toString() {
    return getHeaderString();
  }

  /**
   * Writes the given value of this data type to an XML using the given writer. An element for the
   * data type will have been created by the calling method and will be closed by the calling
   * method. Attributes may be set in this method. Additional elements may be created, but must be
   * closed.
   *
   * @param writer The writer.
   * @param value  The value.
   */
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    if (value == null) {
      return;
    }
    writer.writeCharacters(String.valueOf(value));
  }

  /**
   * @param reader  The xml reader. The current position is an element of this data type.
   * @param flist   The current {@link ModularFeatureList}. Not null.
   * @param row     The current {@link ModularFeatureListRow}. Not null.
   * @param feature The current {@link ModularFeature}. May be null.
   * @param file    The {@link RawDataFile} of the current feature. May be null.
   * @return The value of the data type being read.
   * @throws XMLStreamException
   */
  public Object loadFromXML(@NotNull final XMLStreamReader reader,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    return null;
  }

  /**
   * @return The value class
   */
  public abstract Class<T> getValueClass();

  /**
   * Evaluate a binding for a list of data models (calc the mean value, etc). Used in {@link
   * SimpleRowBinding} to bind a row type to its feautre types.
   *
   * @param bindingType type of binding
   * @param models
   * @return
   */
  public Object evaluateBindings(@NotNull BindingsType bindingType,
      @NotNull List<? extends ModularDataModel> models) {
    // general cases here - special cases handled in other classes
    switch (bindingType) {
      case COUNT: {
        int c = 0;
        for (var model : models) {
          if (model.get(this) != null) {
            c++;
          }
        }
        return c;
      }
      case LIST: {
        List<T> list = new ArrayList<>();
        for (var model : models) {
          T value = model.get(this);
          if (value != null) {
            list.add(value);
          }
        }
        return list;
      }
      default:
        return null;
    }
  }
}
