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
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Type to link processed {@link OtherFeature} to their original raw data (also an unprocessed
 * {@link OtherFeature}).
 */
public class RawTraceType extends DataType<OtherFeature> implements NoTextColumn, NullColumnType {

  private static final Logger logger = Logger.getLogger(RawTraceType.class.getName());

  @Override
  public @NotNull String getUniqueID() {
    return "raw_trace";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Raw trace";
  }

  @Override
  public Property<OtherFeature> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<OtherFeature> getValueClass() {
    return OtherFeature.class;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!(value instanceof OtherFeature of) || file == null) {
      return;
    }

    final OtherDataFile otherDataFile = of.getFeatureData().getOtherDataFile();
    final String name = of.getFeatureData().getName();

    writer.writeAttribute(CONST.XML_OTHER_FILE_DESC_ATTR, otherDataFile.getDescription());
    writer.writeAttribute(CONST.XML_OTHER_TIME_SERIES_NAME_ATTR, name);
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)
        && getUniqueID().equals(reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR)))) {
      throw new IllegalStateException("Wrong element");
    }

    if (file == null) {
      return null;
    }

    final String otherFileDesc = reader.getAttributeValue(null, CONST.XML_OTHER_FILE_DESC_ATTR);
    final String otherSeriesName = reader.getAttributeValue(null,
        CONST.XML_OTHER_TIME_SERIES_NAME_ATTR);

    final OtherDataFile otherFile = OtherDataFile.findInRawFile(file, otherFileDesc);
    if (otherFile == null) {
      return null;
    }

    final OtherTimeSeriesData data = otherFile.getOtherTimeSeriesData();
    if (data == null) {
      return null;
    }

    final OtherFeature rawTrace = data.getRawTraces().stream()
        .filter(t -> Objects.equals(t.getFeatureData().getName(), otherSeriesName)).findFirst()
        .orElse(null);
    if (rawTrace == null) {
      logger.info("Unable to determine raw trace");
    }

    return rawTrace;
  }
}
