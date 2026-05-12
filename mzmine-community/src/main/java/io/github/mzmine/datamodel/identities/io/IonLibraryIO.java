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

package io.github.mzmine.datamodel.identities.io;

import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.datamodel.identities.io.StorableIonLibrary.IonPartDTO;
import io.github.mzmine.datamodel.identities.io.StorableIonLibrary.IonPartID;
import io.github.mzmine.datamodel.identities.io.StorableIonLibrary.IonTypeDTO;
import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.LibraryOrigin;
import io.github.mzmine.datamodel.identities.iontype.UnmodifiableIonLibrary;
import io.github.mzmine.util.XMLUtils;
import io.github.mzmine.util.collections.CollectionUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.JsonUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Load and save ion libraries as json or xml
 */
public class IonLibraryIO {

  private static final Logger logger = Logger.getLogger(IonLibraryIO.class.getName());

  /**
   * @throws RuntimeException if io was not successful
   */
  public static void toJsonFile(@NotNull File file, @NotNull IonLibrary library) {
    file = FileAndPathUtil.getRealFilePath(file, "json");
    final StorableIonLibrary storableLibrary = StorableIonLibrary.of(library);
    JsonUtils.writeToFileReplaceOrThrow(file, storableLibrary);
  }

  public static @NotNull String toJson(@NotNull IonLibrary library) {
    final StorableIonLibrary storableLibrary = StorableIonLibrary.of(library);
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
    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    // keeps a single instance of each part (including count) like one for 1H+ and 2H+
    // deduplicate
    Map<IonPart, IonPart> singletonPartsCounted = new HashMap<>();
    final Map<Integer, IonPartDTO> partIdMap = HashMap.newHashMap(storable.parts().size());
    for (IonPartDTO p : storable.parts()) {
      partIdMap.put(p.id(), p);
    }
    List<IonType> types = new ArrayList<>();
    for (IonTypeDTO ion : storable.ionTypes()) {
      final List<IonPart> ionParts = new ArrayList<>(ion.parts().size());

      for (IonPartID partID : ion.parts()) {
        IonPart part = partIdMap.get(partID.id()).withCount(partID.count());
        // check if the same part (exact same) is in global then use this instance
        part = global.deduplicateAvailable(part);
        // deduplicate with local map in case there is a difference in mass or definition
        part = singletonPartsCounted.computeIfAbsent(part, Function.identity());
        // add singleton instance
        ionParts.add(part);
      }

      types.add(IonType.create(ionParts, ion.molecules()));
    }
    // use library instance that is already created and known if the content equals
    IonLibrary existing = global.getLibraryForName(storable.name()).orElse(null);
    if (existing != null && CollectionUtils.equalContentIgnoreOrder(existing.ions(), types)) {
      // same library content and name
      return existing;
    }

    // handle default libraries that may be loaded and may now have different content
    if (IonLibraries.isInternalLibrary(storable.name())) {
      // a loaded library with name default mzmine
      // this might point to a change of the default library content
      // use different name instead - content is different as this was checked before
      String newName = storable.name()
          .replaceAll(IonLibraries.RESERVED_NAME, "internal library (changed)");
      logger.fine(
          "A library named '%s' was loaded but this internal library has now a different content. Using a different name '%s' now to refer to this library.".formatted(
              storable.name(), newName));
      // renamed: no longer matches any builtin, treat as local with fresh identity
      return new UnmodifiableIonLibrary(UUID.randomUUID(), LibraryOrigin.LOCAL, newName, types);
    }

    // preserve persisted identity when present; generate a fresh one for legacy files
    final UUID id = storable.id();
    final LibraryOrigin origin = storable.origin();
    return new UnmodifiableIonLibrary(id, origin, storable.lastUpdatedDate(), storable.name(),
        types);
  }

  /**
   * @return the library or null if this element does not contain any library
   */
  @Nullable
  public static LoadedIonLibrary loadFromXML(Node parent) {
    try {
      if (parent instanceof Element elParent && !"ionLibrary".equals(elParent.getTagName())) {
        try {
          parent = XMLUtils.findChildElement(elParent, "ionLibrary");
        } catch (Exception e) {
          return null;
        }
      }
      StorableIonLibrary storable = XMLUtils.loadFromDOM(parent, StorableIonLibrary.class);

      return new LoadedIonLibrary(storable.savedDate(), convert(storable));
    } catch (Exception e) {
      throw new RuntimeException("Failed to load ion library from XML", e);
    }
  }

  public static void saveToXML(Node parent, @Nullable IonLibrary library) {
    if (library == null || library.ions().isEmpty()) {
      // null just dont save anything
      return;
    }
    final StorableIonLibrary storable = StorableIonLibrary.of(library);
    XMLUtils.saveToDOM(parent, storable);
  }
}
