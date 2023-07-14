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

package io.github.mzmine.datamodel.features.types.abstr;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataRecord;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.TreeTableColumn;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * handles save/ load from xml
 */
public abstract class SimpleSubColumnsType<T extends ModularDataRecord> extends
    DataType<T> implements SubColumnsFactory {

  private static final Logger logger = Logger.getLogger(SimpleSubColumnsType.class.getName());

  @SuppressWarnings("rawtypes")
  public abstract @NotNull List<DataType> getSubDataTypes();

  /**
   * Create record when loading from xml
   *
   * @param model datatypes mapped to values
   * @return
   */
  protected abstract T createRecord(final SimpleModularDataModel model);

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!(value instanceof ModularDataRecord record)) {
      return;
    }
    writer.writeStartElement(SUB_TYPES_XML_ELEMENT);
    List<DataType> subTypes = getSubDataTypes();

    for (DataType<?> sub : subTypes) {
      Object subValue = record.getValue(sub);
      if (subValue != null) {
        writer.writeStartElement(CONST.XML_DATA_TYPE_ELEMENT);
        writer.writeAttribute(CONST.XML_DATA_TYPE_ID_ATTR, sub.getUniqueID());

        try {
          // catch here, so we can easily debug and don't destroy the flist while saving in case an unexpected exception happens
          sub.saveToXML(writer, subValue, flist, row, feature, file);
        } catch (XMLStreamException e) {
          logger.log(Level.WARNING,
              "Error while writing data type " + sub.getClass().getSimpleName() + " with value "
              + subValue + " to xml.  " + e.getMessage(), e);
        }
        // end sub parameter
        writer.writeEndElement();
      }
    }
    // end outer element for sub types
    writer.writeEndElement();
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    SimpleModularDataModel model = SubColumnsFactory.super.loadSubColumnsFromXML(reader, project,
        flist, row, feature, file);
    return model.isEmpty()? null : createRecord(model);
  }

  @Override
  public @NotNull List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType) {
    // add column for each sub data type
    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();

    List<DataType> subTypes = getSubDataTypes();
    // create column per name
    for (int index = 0; index < subTypes.size(); index++) {
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
  public int getNumberOfSubColumns() {
    return getSubDataTypes().size();
  }

  @Override
  public @Nullable String getHeader(int subcolumn) {
    return getType(subcolumn).getHeaderString();
  }

  @Override
  public @Nullable String getUniqueID(int subcolumn) {
    return getType(subcolumn).getUniqueID();
  }


  @Override
  public @NotNull DataType<?> getType(int index) {
    var list = getSubDataTypes();
    if (index < 0 || index >= list.size()) {
      throw new IndexOutOfBoundsException(
          String.format("Sub column index %d is out of bounds %d", index, list.size()));
    }
    return list.get(index);
  }


  @Override
  public @Nullable Object getSubColValue(int subcolumn, Object cellData) {
    DataType sub = getType(subcolumn);
    return sub == null ? null : getSubColValue(sub, cellData);
  }

  @Override
  public @Nullable Object getSubColValue(DataType sub, Object value) {
    if (value == null) {
      return null;
    } else if (value instanceof ModularDataRecord record) {
      return record.getValue(sub);
    } else {
      throw new IllegalArgumentException(
          String.format("value of type %s needs to be of type ModularDataRecord",
              value.getClass().getName()));
    }
  }

  @Override
  public boolean getDefaultVisibility() {
    return false;
  }
}
