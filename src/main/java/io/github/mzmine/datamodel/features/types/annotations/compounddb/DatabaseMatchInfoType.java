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

package io.github.mzmine.datamodel.features.types.annotations.compounddb;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.DatabaseMatchInfo;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDatabases;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DatabaseMatchInfoType extends DataType<DatabaseMatchInfo> {

  private static final Logger logger = Logger.getLogger(DatabaseMatchInfoType.class.getName());

  @Override
  public @NotNull String getUniqueID() {
    return "database_match_info";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Database Match";
  }

  @Override
  public Property<DatabaseMatchInfo> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<DatabaseMatchInfo> getValueClass() {
    return DatabaseMatchInfo.class;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!(value instanceof DatabaseMatchInfo info)) {
      return;
    }

    writer.writeAttribute("database_id", ParsingUtils.parseNullableString(info.id()));
    writer.writeAttribute("database_name",
        ParsingUtils.parseNullableString(info.onlineDatabase().name()));
    writer.writeCharacters(ParsingUtils.parseNullableString(info.url()));
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!reader.isStartElement() && reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR)
        .equals(getUniqueID())) {
      throw new IllegalStateException("Current element is not a database match info element.");
    }

    final String database_id = ParsingUtils.readNullableString(
        reader.getAttributeValue(null, "database_id"));
    final OnlineDatabases database = OnlineDatabases.valueOf(
        ParsingUtils.readNullableString(reader.getAttributeValue(null, "database_name")));
    final String url = reader.getElementText();

    return new DatabaseMatchInfo(database, database_id, url);
  }

  @Override
  public @NotNull String getFormattedString(DatabaseMatchInfo value) {
    if (value == null) {
      return "";
    }

    return value.toString();
  }

  @Override
  public @Nullable Runnable getDoubleClickAction(@NotNull ModularFeatureListRow row,
      @NotNull List<RawDataFile> file, DataType<?> superType, @Nullable final Object value) {
    final List<CompoundDBAnnotation> compoundAnnotations = row.getCompoundAnnotations();
    if (compoundAnnotations.isEmpty()) {
      return null;
    }

    final DatabaseMatchInfo databaseId = compoundAnnotations.get(0)
        .get(DatabaseMatchInfoType.class);
    if (databaseId == null || databaseId.onlineDatabase() == null || databaseId.id() == null
        || databaseId.url() == null) {
      return null;
    }

    try {
      final URL url = new URL(databaseId.url());
      return () -> {
        MZmineCore.getDesktop().openWebPage(url);
      };
    } catch (MalformedURLException e) {
      logger.log(Level.WARNING, "Cannot open URL for compound ID " + databaseId);
      return null;
    }
  }
}
