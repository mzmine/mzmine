/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.datamodel.features.compoundannotations.Database;
import io.github.mzmine.datamodel.features.compoundannotations.DatabaseMatchInfo;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DatabaseMatchInfoType extends ListWithSubsType<DatabaseMatchInfo> {

  private static final Logger logger = Logger.getLogger(DatabaseMatchInfoType.class.getName());

  public static final List<DataType> subTypes = List.of(new DatabaseMatchInfoType(), new IDType(),
      new DatabaseNameType());

  @Override
  public @NotNull String getUniqueID() {
    return "database_match_info";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Database Match";
  }


  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value == null) {
      return;
    }
    if (!(value instanceof List<?> list)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
          + value.getClass());
    }

    for (Object o : list) {
      if (!(o instanceof DatabaseMatchInfo info)) {
        continue;
      }

      info.saveToXML(writer);
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {

    if (!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)
          && reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR).equals(getUniqueID()))) {
      throw new IllegalStateException("Wrong element");
    }

    List<DatabaseMatchInfo> infos = new ArrayList<>();
    // try reading attributes from older version of load/save: single entry
    final String database_id = ParsingUtils.readNullableString(
        reader.getAttributeValue(null, "database_id"));
    final String database = ParsingUtils.readNullableString(reader.getAttributeValue(null, "database_name"));
    if (database_id != null || database != null) {
      final String url = reader.getElementText();
      infos.add(new DatabaseMatchInfo(Database.getForShortName(database), database_id, url));
      return infos;
    }

    // load new List of infos
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if(reader.getLocalName().equals(DatabaseMatchInfo.XML_ELEMENT)) {
        final DatabaseMatchInfo info = DatabaseMatchInfo.loadFromXML(reader);
        infos.add(info);
      }
    }

    return infos.isEmpty() ? null : infos;
  }

  @Override
  public @Nullable Runnable getDoubleClickAction(@NotNull ModularFeatureListRow row,
      @NotNull List<RawDataFile> file, DataType<?> superType, @Nullable final Object value) {

    if (!(value instanceof DatabaseMatchInfo databaseId) || databaseId.database() == null
        || databaseId.id() == null || databaseId.url() == null) {
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

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  protected <K> @Nullable K map(@NotNull final DataType<K> subType, final DatabaseMatchInfo item) {
    return (K) switch (subType) {
      case DatabaseMatchInfoType __ -> item;
      case IDType __ -> item.id();
      case DatabaseNameType __ -> item.database();
      default -> null;
    };
  }

}
