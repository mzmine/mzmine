/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.identities.io;

import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.datamodel.identities.IonPart;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.SimpleIonLibrary;
import io.github.mzmine.datamodel.identities.io.StorableIonLibrary.IonPartID;
import io.github.mzmine.datamodel.identities.io.StorableIonLibrary.IonPartNoCountDTO;
import io.github.mzmine.datamodel.identities.io.StorableIonLibrary.IonTypeDTO;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.JsonUtils;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Load and save ion libraries as json or xml
 */
public class IonLibraryIO {

  /**
   * @throws RuntimeException if io was not successful
   */
  public static void toJsonFile(@NotNull File file, @NotNull IonLibrary library) {
    file = FileAndPathUtil.getRealFilePath(file, "json");
    final StorableIonLibrary storableLibrary = new StorableIonLibrary(library);
    JsonUtils.writeToFileReplaceOrThrow(file, storableLibrary);
  }

  public static @NotNull String toJson(@NotNull IonLibrary library) {
    final StorableIonLibrary storableLibrary = new StorableIonLibrary(library);
    return JsonUtils.writeStringOrThrow(storableLibrary);
  }

  /**
   *
   * @return the ion library
   * @throws RuntimeException in case of io issues
   */
  public static @NotNull LoadedIonLibrary loadFromJsonFile(@NotNull File file) {
    final StorableIonLibrary storable = JsonUtils.readValueOrThrow(file, StorableIonLibrary.class);
    return new LoadedIonLibrary(storable.savedDate(), convert(storable));
  }

  /**
   *
   * @return the ion library
   */
  public static @NotNull LoadedIonLibrary loadFromJson(@NotNull String json) {
    final StorableIonLibrary storable = JsonUtils.readValueOrThrow(json, StorableIonLibrary.class);
    return new LoadedIonLibrary(storable.savedDate(), convert(storable));
  }

  /**
   * Converts the storable library to a real ion library. The ion parts are single instances per
   * count like one instance for 1Na+ and one for 2Na+
   *
   * @return ion library
   */
  static @NotNull IonLibrary convert(@NotNull StorableIonLibrary storable) {
    // keeps a single instance of each part (including count) like one for 1H+ and 2H+
    Map<IonPartID, IonPart> actualParts = new HashMap<>();

    List<IonType> types = new ArrayList<>();
    for (IonTypeDTO ion : storable.ionTypes()) {
      // con
      final List<IonPart> ionParts = ion.parts().stream()
          .map(p -> actualParts.computeIfAbsent(p, _ -> {
            final IonPartNoCountDTO noCount = storable.parts().get(p.id());
            return noCount.withCount(p.count());
          })).toList();

      types.add(IonType.create(ionParts, ion.molecules()));
    }

    return new SimpleIonLibrary(storable.name(), types);
  }

  /**
   * @return the library or null if this element does not contain any library
   */
  @Nullable
  public static LoadedIonLibrary loadFromXML(Element parent) {
    // create wrapper to avoid attributes leak to parent that shoudl be the parameter element
    final Element xmlElement = (Element) parent.getElementsByTagName("ionLibrary").item(0);

    if (xmlElement == null) {
      return null;
    }
    final String libraryName = xmlElement.getAttribute("name");
    final LocalDateTime savedDate = LocalDateTime.parse(xmlElement.getAttribute("savedDate"));
    final NodeList partsList = xmlElement.getElementsByTagName("parts");

    // Load ion parts
    var partElements = ((Element) partsList.item(0)).getElementsByTagName("part");
    Map<Integer, IonPartNoCountDTO> parts = LinkedHashMap.newLinkedHashMap(
        partElements.getLength());

    for (int i = 0; i < partElements.getLength(); i++) {
      Element partElement = (Element) partElements.item(i);
      int id = Integer.parseInt(partElement.getAttribute("id"));
      String name = partElement.getAttribute("name");
      String formula = partElement.getAttribute("formula");
      int charge = Integer.parseInt(partElement.getAttribute("charge"));
      double mass = Double.parseDouble(partElement.getAttribute("mass"));

      parts.put(id, new IonPartNoCountDTO(name, formula, mass, charge));
    }

    // Load ion types
    Element typesElement = (Element) xmlElement.getElementsByTagName("ionTypes").item(0);
    var typeElements = typesElement.getElementsByTagName("ionType");
    List<StorableIonLibrary.IonTypeDTO> types = new ArrayList<>();
    for (int i = 0; i < typeElements.getLength(); i++) {
      Element typeElement = (Element) typeElements.item(i);
      int molecules = Integer.parseInt(typeElement.getAttribute("molecules"));

      List<IonPartID> ionParts = new ArrayList<>();
      var partRefElements = typeElement.getElementsByTagName("part");
      for (int j = 0; j < partRefElements.getLength(); j++) {
        Element partRef = (Element) partRefElements.item(j);
        int partId = Integer.parseInt(partRef.getAttribute("id"));
        int count = Integer.parseInt(partRef.getAttribute("count"));
        ionParts.add(new IonPartID(partId, count));
      }
      types.add(new StorableIonLibrary.IonTypeDTO(ionParts, molecules));
    }

    final StorableIonLibrary storable = new StorableIonLibrary(libraryName, savedDate, parts,
        types);
    return new LoadedIonLibrary(storable.savedDate(), convert(storable));
  }

  public static void saveToXML(Element parent, @Nullable IonLibrary library) {
    if (library == null || library.ions().isEmpty()) {
      // null just dont save anything
      return;
    }
    final StorableIonLibrary storable = new StorableIonLibrary(library);

    final Document doc = parent.getOwnerDocument();
    Element xmlElement = doc.createElement("ionLibrary");
    parent.appendChild(xmlElement);

    // Save library name
    xmlElement.setAttribute("name", storable.name());
    xmlElement.setAttribute("savedDate", storable.savedDate().toString());

    // Save ion parts
    Element partsElement = doc.createElement("parts");
    xmlElement.appendChild(partsElement);
    storable.parts().forEach((id, part) -> {
      Element partElement = doc.createElement("part");
      partElement.setAttribute("id", String.valueOf(id));
      partElement.setAttribute("name", part.name());
      partElement.setAttribute("formula", part.formula());
      partElement.setAttribute("charge", String.valueOf(part.charge()));
      partElement.setAttribute("mass", String.valueOf(part.mass()));
      partsElement.appendChild(partElement);
    });

    // Save ion types
    Element typesElement = doc.createElement("ionTypes");
    xmlElement.appendChild(typesElement);
    storable.ionTypes().forEach(type -> {
      Element typeElement = doc.createElement("ionType");
      typeElement.setAttribute("molecules", String.valueOf(type.molecules()));

      type.parts().forEach(part -> {
        Element partRef = doc.createElement("part");
        partRef.setAttribute("id", String.valueOf(part.id()));
        partRef.setAttribute("count", String.valueOf(part.count()));
        typeElement.appendChild(partRef);
      });

      typesElement.appendChild(typeElement);
    });
  }
}
