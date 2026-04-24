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

package io.github.mzmine.datamodel.identities.global;

import io.github.mzmine.datamodel.identities.io.IonLibraryIO;
import io.github.mzmine.datamodel.identities.io.IonLibraryPreset;
import io.github.mzmine.datamodel.identities.io.LoadedIonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonPartDefinition;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.UnmodifiableIonLibrary;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Load and save the {@link GlobalIonLibraryService} that holds all ions. Libraries are saved
 * separately as {@link IonLibraryPreset}.
 */
class GlobalIonLibraryIO {

  private static final Logger logger = Logger.getLogger(GlobalIonLibraryIO.class.getName());
  /**
   * Track the last modified state of the loaded global library to see if changes need to be loaded
   */
  private static final AtomicLong globalFileLastModified = new AtomicLong(-1);

  static boolean saveGlobalIonDefinitionLibrary(@NotNull File file) {
    final GlobalIonLibraryService library = GlobalIonLibraryService.getGlobalLibrary();
    final GlobalIonLibraryDTO current = library.getCurrentGlobalLibrary();

    try {
      // only save ion definitions because those are important
      // we use an ion library here to use latest update date and compatible saving
      final List<IonType> ions = current.partDefinitions().stream().map(IonType::create).toList();

      IonLibraryIO.toJsonFile(file,
          new UnmodifiableIonLibrary("mzmine_global_ions_definitions", ions));
      globalFileLastModified.set(file.lastModified());
      logger.fine("Saved global ion library to file: " + file.getAbsolutePath());
      return true;
    } catch (Exception ex) {
      logger.log(Level.WARNING,
          "Could not write global ions library. File might be locked or path not accessible. Skipping step. Try later again.");
    }
    return false;
  }

  /**
   * Loads the global library from file, also if the library has changed since the last import
   */
  static void loadGlobalIonDefinitionLibrary() {
    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();

    File file = GlobalIonLibraryService.getGlobalFile();
    if (!file.exists()) {
      logger.fine(
          "There is no local global ion library file. This file only exists after first changes to the library. The mzmine internal defaults are available.");
      return;
    }

    if (file.lastModified() == globalFileLastModified.get()) {
      logger.fine(
          "Skipping global ion library loading as it was already loaded and is up-to-date.");
      return;
    }

    final LoadedIonLibrary loadedLib;
    try {
      loadedLib = IonLibraryIO.loadFromJsonFile(file);
      globalFileLastModified.set(file.lastModified());
      logger.fine("Loaded global ion library from file: " + file.getAbsolutePath());
    } catch (Exception ex) {
      logger.warning("Cannot load file: " + file.getAbsolutePath() + " because " + ex.getMessage());
      return;
    }

    // this loaded library represents ion part definitions as ion types but we add the definitions
    final List<@NotNull IonPartDefinition> definitions = loadedLib.library().ions().stream()
        .map(ion -> IonPartDefinition.of(ion.parts().getFirst())).toList();
    global.addPartDefinitions(definitions);
  }
}
