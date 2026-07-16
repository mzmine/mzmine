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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;

public class LegacyIonTypeXMLReader {

  public static IonType loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(IonType.XML_ELEMENT))) {
      throw new IllegalStateException("Current element is not an iontype");
    }

    final Integer molecules = ParsingUtils.stringToInteger(
        reader.getAttributeValue(null, "molecules"));
    Objects.requireNonNull(molecules);
    // charge and mass is calculated from rest
//    final Integer charge = ParsingUtils.stringToInteger(reader.getAttributeValue(null, "charge"));
//    Objects.requireNonNull(charge);
//    final Integer mass = ParsingUtils.stringToInteger(reader.getAttributeValue(null, "mass"));
//    Objects.requireNonNull(mass);

    final List<@NotNull IonPart> parts = new ArrayList<>();

    LegacyIonModification adduct = null;
    LegacyIonModification mod = null;
    // has to load the new and the legacy ion modifications from projects
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(IonType.XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      // new ions
      if (reader.getLocalName().equals(IonParts.XML_ELEMENT)) {
        var part = IonParts.loadFromXML(reader);
        Objects.requireNonNull(part);
        parts.add(part);
      }
      // old legacy ion
      else if (reader.getLocalName().equals("adduct")) {
        if (ParsingUtils.progressToStartElement(reader, LegacyIonModification.XML_ELEMENT,
            CONST.XML_DATA_TYPE_ELEMENT)) {
          adduct = LegacyIonModification.loadFromXML(reader);
        } else {
          return null;
        }
      } else if (reader.getLocalName().equals("modification")) {
        if (ParsingUtils.progressToStartElement(reader, LegacyIonModification.XML_ELEMENT,
            CONST.XML_DATA_TYPE_ELEMENT)) {
          mod = LegacyIonModification.loadFromXML(reader);
        }
      }
    }

    // old always have adduct but mod is optional
    if (adduct != null) {
      return mod != null ? new LegacyIonType(molecules, adduct, mod).toNewIonType()
          : new LegacyIonType(molecules, adduct).toNewIonType();
    }
    if (parts.isEmpty()) {
      throw new IllegalStateException("No ion parts found in xml");
    }

    return IonType.create(parts, molecules);
  }
}
