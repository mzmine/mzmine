/*
 * Copyright 2006-2021 The MZmine Development Team
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

package io.github.mzmine.datamodel;

import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.CompoundDBIdentity;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.util.Collection;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;

/**
 * This interface represents an identification result.
 */
public interface FeatureIdentity extends Cloneable {

  public static final String XML_IDENTITY_TYPE_ATTR = "identitytype";
  public static final String XML_GENERAL_IDENTITY_ELEMENT = "featureidentity";
  public static final String XML_PROPERTY_ELEMENT = "property";
  public static final String XML_NAME_ATTR = "name";

  /**
   * These variables define standard properties. The PROPERTY_NAME must be present in all instances
   * of FeatureIdentity. It defines the value which is returned by the toString() method.
   */
  String PROPERTY_NAME = "Name";
  String PROPERTY_FORMULA = "Molecular formula";
  String PROPERTY_METHOD = "Identification method";
  String PROPERTY_ID = "ID";
  String PROPERTY_URL = "URL";
  String PROPERTY_SPECTRUM = "SPECTRUM";
  String PROPERTY_COMMENT = "Comment";
  String PROPERTY_ADDUCT ="Adduct";
  String PROPERTY_SMILES = "Smiles";
  String PROPERTY_INCHI_KEY = "InChIKey";
  String PROPERTY_RT = "RT (lib)";
  String PROPERTY_CCS = "CCS (lib)";
  String PROPERTY_MOBILITY = "Mobility (lib)";
  String PROPERTY_PRECURSORMZ = "Precursor mz";

  /**
   * Returns the value of the PROPERTY_NAME property. This value must always be set. Same value is
   * returned by the toString() method.
   *
   * @return Name
   */
  @NotNull String getName();

  /**
   * Returns full, multi-line description of this identity, one property per line (key: value)
   *
   * @return Description
   */
  @NotNull String getDescription();

  /**
   * Returns the value for a
   *
   * @param property
   * @return Description
   */
  @NotNull String getPropertyValue(String property);

  /**
   * Returns all the properties in the form of a map key --> value
   *
   * @return Description
   */
  @NotNull Map<String, String> getAllProperties();

  @NotNull
  public Object clone();

  /**
   * Appends a feature identity to the current element.
   */
  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException;

  public static FeatureIdentity loadFromXML(XMLStreamReader reader,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_GENERAL_IDENTITY_ELEMENT))) {
      throw new IllegalStateException("Current element is not a feature identity element");
    }

    return switch (reader.getAttributeValue(null, XML_IDENTITY_TYPE_ATTR)) {
      case SimpleFeatureIdentity.XML_IDENTITY_TYPE -> SimpleFeatureIdentity.loadFromXML(reader);
      case SpectralDBFeatureIdentity.XML_IDENTITY_TYPE -> SpectralDBFeatureIdentity
          .loadFromXML(reader, possibleFiles);
      case CompoundDBIdentity.XML_IDENTITY_TYPE -> CompoundDBIdentity.loadFromXML(reader);
      default -> null;
    };
  }
}
