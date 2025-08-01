/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.scores.CorrelationCoefficientType;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.modules.io.projectload.version_3_0.FeatureListLoadTask;
import io.github.mzmine.modules.io.projectsave.FeatureListSaveTask;
import io.github.mzmine.util.MemoryMapStorage;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map.Entry;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MsOtherCorrelationResult(@NotNull OtherFeature otherFeature,
                                       @NotNull MsOtherCorrelationType type,
                                       @Nullable Float correlation) {

  public static final String XML_ELEMENT_NAME = "msothercorrelationresult";
  public static final String XML_CORRELATION_TYPE_ATTR = "msothercorrelationtype";
  public static final String XML_PEARSON_ATTR = new CorrelationCoefficientType().getUniqueID();

  public static MsOtherCorrelationResult loadFromXML(@NotNull XMLStreamReader reader,
      @Nullable MemoryMapStorage storage, MZmineProject project, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @NotNull RawDataFile file) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT_NAME))) {
      throw new IllegalStateException("Wrong element");
    }

    final String correlationTypeStr = reader.getAttributeValue(null, XML_CORRELATION_TYPE_ATTR);
    final MsOtherCorrelationType correlationType = MsOtherCorrelationType.valueOf(
        correlationTypeStr);

    final String correlationStr = reader.getAttributeValue(null, XML_PEARSON_ATTR);
    Float correlation = null;
    if (correlationStr != null) {
      try {
        correlation = Float.parseFloat(correlationStr);
      } catch (NumberFormatException e) {
        // ignore
      }
    }

    OtherFeature otherFeature = new OtherFeatureImpl();
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT_NAME))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(CONST.XML_OTHER_FEATURE_ELEMENT)) {
        while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
            .equals(CONST.XML_OTHER_FEATURE_ELEMENT))) {
          reader.next();
          if (!reader.isStartElement()) {
            continue;
          }

          if (reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)) {
            // the data types are responsible for loading their values
            DataType type = DataTypes.getTypeForId(
                reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
            Object value = FeatureListLoadTask.parseDataType(reader, type, project, flist, row,
                null, file);
            if (type != null && value != null) {
              try {
                otherFeature.set(type, value);
              } catch (RuntimeException e) {
                // TODO - maybe log?
                // cannot set bound values. can go silent.
              }
            }
          }
        }
      }
    }

    return new MsOtherCorrelationResult(otherFeature, correlationType, correlation);
  }

  public void saveToXML(@NotNull XMLStreamWriter writer, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @NotNull RawDataFile file) throws XMLStreamException {

    // main element
    writer.writeStartElement(XML_ELEMENT_NAME);
    writer.writeAttribute(XML_CORRELATION_TYPE_ATTR, type.name());
    if (correlation != null) {
      writer.writeAttribute(XML_PEARSON_ATTR, String.valueOf(correlation));
    }

    writer.writeStartElement(CONST.XML_OTHER_FEATURE_ELEMENT);
    final List<Entry<DataType, Object>> entries = otherFeature.stream().toList();
    for (Entry<DataType, Object> entry : entries) {
      FeatureListSaveTask.writeDataType(writer, entry.getKey(), entry.getValue(), flist, row, null,
          file);
    }
    writer.writeEndElement();
//
    // main element
    writer.writeEndElement();
  }

  @Override
  public String toString() {
    final ChromatogramType chromType = otherFeature.getChromatogramType();
    final NumberFormat score = ConfigService.getGuiFormats().scoreFormat();

    return "%s, %s (%s)".formatted(chromType.toString(), type.toString(),
        correlation != null ? score.format(correlation) : "?");
  }
}
