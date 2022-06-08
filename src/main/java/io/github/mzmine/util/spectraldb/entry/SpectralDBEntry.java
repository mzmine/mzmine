/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util.spectraldb.entry;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class SpectralDBEntry {

  public static final String XML_ELEMENT = "spectraldatabaseentry";
  public static final String XML_DB_FIELD_LIST_ELEMENT = "databasefieldslist";
  private static final String XML_DB_FIELD_ELEMENT = "entry";
  private static final String XML_FIELD_NAME_ATTR = "name";

  private final Map<DBEntryField, Object> fields;
  private final DataPoint[] dps;

  public SpectralDBEntry(double precursorMZ, DataPoint[] dps) {
    this.fields = new HashMap<>();
    fields.put(DBEntryField.MZ, precursorMZ);
    this.dps = dps;
  }

  public SpectralDBEntry(double precursorMZ, int charge, DataPoint[] dps) {
    this(precursorMZ, dps);
    fields.put(DBEntryField.CHARGE, charge);
  }

  public SpectralDBEntry(Map<DBEntryField, Object> fields, DataPoint[] dps) {
    this.fields = fields;
    this.dps = dps;
  }

  public static SpectralDBEntry loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException(
          "Cannot load spectral db entry from the current element. Wrong name.");
    }

    double[] mzs = null;
    double[] intensities = null;
    Map<DBEntryField, Object> fields = null;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
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

    DataPoint[] dps = new DataPoint[mzs.length];
    for (int i = 0; i < dps.length; i++) {
      dps[i] = new SimpleDataPoint(mzs[i], intensities[i]);
    }

    return new SpectralDBEntry(fields, dps);
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

  public Double getPrecursorMZ() {
    return (Double) fields.get(DBEntryField.MZ);
  }

  public Optional<Object> getField(DBEntryField f) {
    return Optional.ofNullable(fields.get(f));
  }

  public <T> T getOrElse(DBEntryField f, T defaultValue) {
    final Object value = fields.get(f);
    return value == null ? defaultValue : (T) value;
  }

  public DataPoint[] getDataPoints() {
    return dps;
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);

    double[] mzs = Arrays.stream(dps).mapToDouble(DataPoint::getMZ).toArray();
    double[] intensities = Arrays.stream(dps).mapToDouble(DataPoint::getIntensity).toArray();

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
    boolean b1 = Arrays.equals(dps, that.dps);
    boolean b2 = Objects.equals(fields, that.fields);
    return b1 && b2;
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(fields);
    result = 31 * result + Arrays.hashCode(dps);
    return result;
  }
}
