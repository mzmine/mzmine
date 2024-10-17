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

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.modules.io.projectsave.FeatureListSaveTask;
import java.util.Map.Entry;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;

public record MsOtherCorrelationResult(OtherFeature otherFeature, MsOtherCorrelationType type) {

  public static final String XML_ELEMENT_NAME = "msothercorrelationresult";
  public static final String XML_CORRELATION_TYPE_ATTR = "msothercorrelationtype";

  public void saveToXML(@NotNull XMLStreamWriter writer, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row) throws XMLStreamException {

    // main element
    writer.writeStartElement(XML_ELEMENT_NAME);

    writer.writeAttribute(XML_CORRELATION_TYPE_ATTR, type.name());

    writer.writeStartElement(CONST.XML_OTHER_FEATURE_ELEMENT);
    for (Entry<DataType, Object> entry : otherFeature.getMap().entrySet()) {
      FeatureListSaveTask.writeDataType(writer, entry.getKey(), entry.getValue(), flist, row, null, null);
    }
    writer.writeEndElement();
//
    // main element
    writer.writeEndElement();
  }
}
