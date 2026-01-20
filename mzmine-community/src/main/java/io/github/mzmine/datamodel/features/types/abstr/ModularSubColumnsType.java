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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * handles save/ load from xml
 */
public abstract class ModularSubColumnsType<T extends ModularDataRecord> extends
    SimpleSubColumnsType<T> implements SubColumnsFactory {

  private static final Logger logger = Logger.getLogger(ModularSubColumnsType.class.getName());

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
    return model.isEmpty() ? null : createRecord(model);
  }


  @Override
  public boolean getDefaultVisibility() {
    return false;
  }
}
