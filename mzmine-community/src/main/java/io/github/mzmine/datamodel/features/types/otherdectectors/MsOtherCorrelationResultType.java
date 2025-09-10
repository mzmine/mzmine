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

package io.github.mzmine.datamodel.features.types.otherdectectors;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationResult;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MsOtherCorrelationResultType extends ListWithSubsType<MsOtherCorrelationResult> {

  @Override
  public @NotNull String getUniqueID() {
    return "ms_other_correlation_result";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Correlated traces";
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value == null || file == null) {
      return;
    }

    if (!(value instanceof List<?> list)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
              + value.getClass());
    }

    for (Object o : list) {
      if (!(o instanceof MsOtherCorrelationResult correlationResult)) {
        continue;
      }

      correlationResult.saveToXML(writer, flist, row, file);
    }
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

    List<MsOtherCorrelationResult> corrResults = new ArrayList<>();
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(MsOtherCorrelationResult.XML_ELEMENT_NAME)) {
        var loaded = MsOtherCorrelationResult.loadFromXML(reader, file.getMemoryMapStorage(),
            project, flist, row, file);
        if (loaded != null) {
          corrResults.add(loaded);
        }
      }
    }

    return corrResults.isEmpty() ? null : corrResults;
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return List.of(new MsOtherCorrelationResultType(), new ChromatogramTypeType(), new AreaType(),
        new HeightType(), new AreaPercentType());
  }

  @Override
  public <K> @Nullable K map(@NotNull DataType<K> subType, MsOtherCorrelationResult parentItem) {
    return (K)switch (subType) {
      case MsOtherCorrelationResultType _ -> parentItem;
      case AreaType a -> parentItem.otherFeature().get(a);
      case HeightType h -> parentItem.otherFeature().get(h);
      case ChromatogramTypeType c -> parentItem.otherFeature().getChromatogramType();
      case AreaPercentType a -> parentItem.otherFeature().get(a);
      default -> throw new UnsupportedOperationException(
          "DataType %s is not covered in map ".formatted(subType.toString()));
    };
  }
}
