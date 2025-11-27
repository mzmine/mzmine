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

package io.github.mzmine.datamodel.identities;

import io.github.mzmine.datamodel.identities.io.IonLibraryPreset;
import io.github.mzmine.datamodel.identities.io.StorableIonLibrary;
import io.github.mzmine.util.io.JsonUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Load and save the {@link GlobalIonLibraryService} that holds all ions. Libraries are saved
 * separately as {@link IonLibraryPreset}.
 */
class GlobalIonLibraryIO {

  /**
   * Track the last modified state of the loaded global library to see if changes need to be loaded
   */
  private static final AtomicLong globalFileLastModified = new AtomicLong(-1);

  public static @NotNull String saveGlobalIonLibrary() {
    final File file = getGlobalFile();
    saveJson(file);
    globalFileLastModified.set(file.lastModified());

    logger.fine("Saved global ion library to file: " + file.getAbsolutePath());
    final StorableIonLibrary storableLibrary = new StorableIonLibrary(library);
    return JsonUtils.writeStringOrThrow(storableLibrary);
  }

  /**
   *
   * @return the ion library
   */
  public static @NotNull IonLibrary fromJson(@NotNull String json) {
    final StorableIonLibrary storable = JsonUtils.readValueOrThrow(json, StorableIonLibrary.class);
    return convert(storable);
  }


  /**
   * Loads the global library from file, also if the library has changed since the last import
   *
   * @return a global ion library
   */
  public static void loadGlobalIonLibrary() {
    File file = getGlobalFile();
    if (globalLibrary == null || (file.exists()
        && file.lastModified() != globalFileLastModified.get())) {
      globalLibrary = loadGlobalIonLibrary();
    }
    // maybe already initialized - then no need to add mzmine internal ions
    // just check if reload of file is needed
    boolean alreadyInitialized = globalFileLastModified.get() != -1;

    File file = getGlobalFile();
    GlobalIonLibraryService global = null;
    if (file.exists() && file.lastModified() != globalFileLastModified.get()) {
      try {
        global = loadJson(file);
        globalFileLastModified.set(file.lastModified());
        logger.fine("Loaded global ion library from file: " + file.getAbsolutePath());
      } catch (IOException ex) {
        logger.warning(
            "Cannot load file: " + file.getAbsolutePath() + " because " + ex.getMessage());
      }
    }

    // use already initialzied one or new
    final GlobalIonLibraryService internalLibrary;
    if (alreadyInitialized) {
      internalLibrary = globalLibrary;
    } else {
      // might also have new internal ion types defined in mzmine - combine these into a new library
      var types = Arrays.stream(IonTypes.values()).map(IonTypes::asIonType)
          .collect(Collectors.toCollection(ArrayList::new));
      internalLibrary = new GlobalIonLibraryService(new ArrayList<>(IonParts.PREDEFINED_PARTS),
          types);
    }

    // merge both libraries: from file and internal
    GlobalIonLibraryService merged = GlobalIonLibraryService.merge(global, internalLibrary);

    // if changed then save
    if (global == null || merged.numIonTypes() != global.numIonTypes()
        || merged.numIonParts() != global.numIonParts()) {
      logger.info("Initializing ion libraries file: " + file.getAbsolutePath());
      try {
        merged.saveGlobalLibrary();
      } catch (IOException e) {
        logger.log(Level.WARNING,
            "Cannot initialize ion libraries file in: " + file.getAbsolutePath()
                + ". Will continue with default list of ions.", e);
      }
    }

    return merged;
  }
}
