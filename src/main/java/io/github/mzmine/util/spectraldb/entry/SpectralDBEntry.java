/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.util.spectraldb.entry;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectralDBEntry extends SimpleMassList implements SpectralLibraryEntry {

  public static final String XML_DB_FIELD_LIST_ELEMENT = "databasefieldslist";
  public static final String XML_LIBRARY_FILE_NAME_ATTR = "library_file";
  private static final String XML_DB_FIELD_ELEMENT = "entry";
  private static final String XML_FIELD_NAME_ATTR = "name";
  private final Map<DBEntryField, Object> fields;

  @Nullable
  private SpectralLibrary library;

  public SpectralDBEntry(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues, @Nullable Map<DBEntryField, Object> fields,
      @Nullable SpectralLibrary library) {
    super(storage, mzValues, intensityValues);
    this.fields = new HashMap<>();
    if (fields != null) {
      this.fields.putAll(fields);
    }
    this.library = library;
  }

  public SpectralDBEntry(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues, @Nullable Map<DBEntryField, Object> fields) {
    this(storage, mzValues, intensityValues, fields, null);
  }

  public SpectralDBEntry(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues, SpectralLibrary library) {
    this(storage, mzValues, intensityValues, null, library);
  }

  public SpectralDBEntry(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues) {
    this(storage, mzValues, intensityValues, null, null);
  }

  public static SpectralLibraryEntry loadFromXML(XMLStreamReader reader, MZmineProject project)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT_ENTRY))) {
      throw new IllegalStateException(
          "Cannot load spectral db entry from the current element. Wrong name.");
    }

    double[] mzs = null;
    double[] intensities = null;
    Map<DBEntryField, Object> fields = null;

    final String libraryFileName = reader.getAttributeValue(null, XML_LIBRARY_FILE_NAME_ATTR);

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT_ENTRY))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      switch (reader.getLocalName()) {
        case CONST.XML_MZ_VALUES_ELEMENT ->
            mzs = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case CONST.XML_INTENSITY_VALUES_ELEMENT ->
            intensities = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case XML_DB_FIELD_LIST_ELEMENT -> fields = loadDBEntriesFromXML(reader);
        default -> {
        }
      }
    }

    assert mzs != null && intensities != null;
    assert mzs.length == intensities.length;

    if (libraryFileName != null) {
      final SpectralLibrary library = project.getCurrentSpectralLibraries().stream()
          .filter(l -> l.getName().equals(libraryFileName)).findFirst().orElse(null);
      return new SpectralDBEntry(null, mzs, intensities, fields, library);
    } else {
      return new SpectralDBEntry(null, mzs, intensities, fields);
    }
  }

  private static Map<DBEntryField, Object> loadDBEntriesFromXML(XMLStreamReader reader)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_DB_FIELD_LIST_ELEMENT))) {
      throw new IllegalStateException(
          "Cannot load spectral db entry fields from the current element. Wrong name.");
    }

    Map<DBEntryField, Object> fields = new EnumMap<>(DBEntryField.class);
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_DB_FIELD_LIST_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      final String keyName = reader.getAttributeValue(null, XML_FIELD_NAME_ATTR);
      final String value = reader.getElementText();

      DBEntryField field = DBEntryField.valueOf(keyName);
      Object convertValue = field.convertValue(value);

      fields.put(field, convertValue);
    }

    return fields;
  }

  @Override
  public void putAll(Map<DBEntryField, Object> fields) {
    this.fields.putAll(fields);
  }

  @Override
  public boolean putIfNotNull(DBEntryField field, Object value) {
    if (field != null && value != null) {
      fields.put(field, value);
      return true;
    }
    return false;
  }

  @Override
  public Double getPrecursorMZ() {
    return (Double) fields.get(DBEntryField.PRECURSOR_MZ);
  }

  @Override
  public Optional<Object> getField(DBEntryField f) {
    return Optional.ofNullable(fields.get(f));
  }

  @Override
  public <T> T getOrElse(DBEntryField f, T defaultValue) {
    final Object value = fields.get(f);
    return value == null ? defaultValue : (T) value;
  }

  @Override
  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT_ENTRY);
    if (getLibraryName() != null) {
      writer.writeAttribute(XML_LIBRARY_FILE_NAME_ATTR, getLibraryName());
    }

    double[] mzs = getMzValues(new double[getNumberOfDataPoints()]);
    double[] intensities = getIntensityValues(new double[getNumberOfDataPoints()]);

    writer.writeStartElement(CONST.XML_MZ_VALUES_ELEMENT);
    writer.writeCharacters(ParsingUtils.doubleArrayToString(mzs, mzs.length));
    writer.writeEndElement(); // mzs
    writer.writeStartElement(CONST.XML_INTENSITY_VALUES_ELEMENT);
    writer.writeCharacters(ParsingUtils.doubleArrayToString(intensities, intensities.length));
    writer.writeEndElement(); // intensities

    writer.writeStartElement(XML_DB_FIELD_LIST_ELEMENT);
    for (Entry<DBEntryField, Object> entry : fields.entrySet()) {
      var key = entry.getKey();
      var value = entry.getValue();
      writer.writeStartElement(XML_DB_FIELD_ELEMENT);
      writer.writeAttribute(XML_FIELD_NAME_ATTR, key.name());
      writer.writeCharacters(String.valueOf(value));
      writer.writeEndElement(); // field
    }
    writer.writeEndElement(); // list

    writer.writeEndElement(); // this entry
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpectralDBEntry that = (SpectralDBEntry) o;
    return Objects.equals(fields, that.fields)
        && getNumberOfDataPoints() == that.getNumberOfDataPoints();
  }

  @Override
  public String toString() {
    return String.format("Entry: %s (dp: %d)", getOrElse(DBEntryField.NAME, ""),
        getNumberOfDataPoints());
  }

  @Override
  public int hashCode() {
    return Objects.hash(fields, getNumberOfDataPoints());
  }

  @Override
  public Map<DBEntryField, Object> getFields() {
    return fields;
  }

  public @Nullable SpectralLibrary getLibrary() {
    return library;
  }

  public void setLibrary(@Nullable SpectralLibrary library) {
    this.library = library;
  }

  @Nullable
  public String getLibraryName() {
    return library != null ? library.getName() : null;
  }

}
