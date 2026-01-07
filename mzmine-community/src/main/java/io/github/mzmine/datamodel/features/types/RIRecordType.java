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

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.RIRecord;
import io.github.mzmine.util.StringUtils;
import java.util.function.Function;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RIRecordType extends DataType<RIRecord> implements NullColumnType {

  static Function<@Nullable String, @Nullable RIRecord> mapper = RIRecord::fromString;

  @Override
  public @NotNull String getUniqueID() {
    return "ri_record";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "RI record";
  }

  @Override
  public Property<RIRecord> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<RIRecord> getValueClass() {
    return RIRecord.class;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if(!(value instanceof RIRecord riRecord)) {
      return;
    }

    writer.writeCharacters(riRecord.toString());
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {

    final String text = ParsingUtils.parseNullableString(reader.getElementText());
    if(StringUtils.isBlank(text)) {
      return null;
    }

    final RIRecord riRecord = new RIRecord(text);
    return riRecord.isEmpty() ? null : riRecord;
  }

  @Override
  public @Nullable Function<@Nullable String, @Nullable RIRecord> getMapper() {
    return mapper;
  }
}
