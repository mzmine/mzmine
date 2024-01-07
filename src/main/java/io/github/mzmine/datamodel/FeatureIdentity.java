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

package io.github.mzmine.datamodel;

import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.CompoundDBIdentity;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.util.Collection;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;

/**
 * This interface represents an identification result.
 *
 * To be replaced by {@link io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation}.
 */
@Deprecated
@ScheduledForRemoval
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
  String PROPERTY_ADDUCT = "Adduct";
  String PROPERTY_SMILES = "Smiles";
  String PROPERTY_INCHI_KEY = "InChIKey";
  String PROPERTY_RT = "RT (lib)";
  String PROPERTY_CCS = "CCS (lib)";
  String PROPERTY_MOBILITY = "Mobility (lib)";
  String PROPERTY_PRECURSORMZ = "Precursor mz";

  public static FeatureIdentity loadFromXML(XMLStreamReader reader, MZmineProject project,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_GENERAL_IDENTITY_ELEMENT))) {
      throw new IllegalStateException("Current element is not a feature identity element");
    }

    return switch (reader.getAttributeValue(null, XML_IDENTITY_TYPE_ATTR)) {
      case SimpleFeatureIdentity.XML_IDENTITY_TYPE -> SimpleFeatureIdentity.loadFromXML(reader);
      case SpectralDBFeatureIdentity.XML_IDENTITY_TYPE ->
          SpectralDBFeatureIdentity.loadFromXML(reader, project, possibleFiles);
      case CompoundDBIdentity.XML_IDENTITY_TYPE -> CompoundDBIdentity.loadFromXML(reader);
      default -> null;
    };
  }

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
}
