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

package io.github.mzmine.datamodel.features.compoundannotations;

import static io.github.mzmine.util.ParsingUtils.readNullableString;

import io.github.mzmine.util.ParsingUtils;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.Nullable;

public record DatabaseMatchInfo(@Nullable Database database, @Nullable String id,
                                @Nullable String url) {

  public static final String XML_ELEMENT = "database_match_info";

  public DatabaseMatchInfo(@Nullable Database database, @Nullable String id) {
    this(database, id, database!=null? database.getUrl(id) : null);
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    if (database != null) {
      b.append(database).append(": ");
    } else {
      b.append("N/A: ");
    }

    if (id != null) {
      b.append(id);
    } else {
      b.append("no id");
    }
    return b.toString();
  }

  public void saveToXML(final XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);

    writer.writeAttribute("database_id", ParsingUtils.parseNullableString(id()));
    writer.writeAttribute("database_url", ParsingUtils.parseNullableString(url()));

    if (database != null) {
      database.saveToXML(writer);
    }

    writer.writeEndElement(); // this entry
  }

  public static DatabaseMatchInfo loadFromXML(final XMLStreamReader reader)
      throws XMLStreamException {
    final String id = readNullableString(reader.getAttributeValue(null, "database_id"));
    final String dbName = readNullableString(reader.getAttributeValue(null, "database_name"));
    final String dbUrl = readNullableString(reader.getAttributeValue(null, "database_url"));
    Database db = null;

    // load new List of infos
    while (reader.hasNext() && !reader.isEndElement()) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(Database.XML_ELEMENT)) {
        db = Database.loadFromXML(reader);
        break;
      }
    }
    return new DatabaseMatchInfo(db, id, dbUrl);
  }

}
