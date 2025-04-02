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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mzmine.datamodel.identities.IonPart.IonPartStringFlavor;
import io.github.mzmine.datamodel.identities.IonType.IonTypeStringFlavor;
import io.github.mzmine.datamodel.identities.fx.sub.IonSorting;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.maths.Precision;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param parts
 * @param ionTypes
 */
public record IonLibrary(List<IonPart> parts, List<IonType> ionTypes) {

  private static final Logger logger = Logger.getLogger(IonLibrary.class.getName());

  /**
   * Track the last modified state of the loaded global library to see if changes need to be loaded
   */
  private static IonLibrary globalLibrary;
  private static final AtomicLong globalFileLastModified = new AtomicLong(-1);

  /**
   * Makes sure that the global library is read from file, also if the file has since changed.
   *
   * @return global ion library
   */
  public static @NotNull IonLibrary getGlobalLibrary() {
    File file = getGlobalFile();
    if (globalLibrary == null || (file.exists()
                                  && file.lastModified() != globalFileLastModified.get())) {
      globalLibrary = loadGlobalIonLibrary();
    }

    return globalLibrary;
  }


  public static File getGlobalFile() {
    return FileAndPathUtil.resolveInMzmineDir("libraries/mzmine_global_ions.json");
  }

  /**
   * Loads the global library from file, also if the library has changed since the last import
   *
   * @return a global ion library
   */
  private static synchronized IonLibrary loadGlobalIonLibrary() {
    // maybe already initialized - then no need to add mzmine internal ions
    // just check if reload of file is needed
    boolean alreadyInitialized = globalFileLastModified.get() != -1;

    File file = getGlobalFile();
    IonLibrary global = null;
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
    final IonLibrary internalLibrary;
    if (alreadyInitialized) {
      internalLibrary = globalLibrary;
    } else {
      // might also have new internal ion types defined in mzmine - combine these into a new library
      var types = Arrays.stream(IonTypes.values()).map(IonTypes::asIonType)
          .collect(Collectors.toCollection(ArrayList::new));
      internalLibrary = new IonLibrary(new ArrayList<>(IonParts.PREDEFINED_PARTS), types);
    }

    // merge both libraries: from file and internal
    IonLibrary merged = IonLibrary.merge(global, internalLibrary);

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

  private int numIonTypes() {
    return ionTypes.size();
  }

  private int numIonParts() {
    return parts.size();
  }

  /**
   * merge two libraries
   *
   * @return merges two non-null libraries into a new instance. Or null if both are null or returns
   * the original instance that is non-null
   */
  public static IonLibrary merge(final @Nullable IonLibrary first,
      final @Nullable IonLibrary second) {
    if (first == null && second == null) {
      return null;
    }
    if (first == null) {
      return second;
    }
    if (second == null) {
      return first;
    }

    List<IonPart> mergedParts = mergeParts(first.parts(), second.parts());
    List<IonType> ionTypes = mergeTypes(first.ionTypes(), second.ionTypes());

    return new IonLibrary(mergedParts, ionTypes);
  }

  private static List<IonPart> mergeParts(final List<IonPart> first, final List<IonPart> second) {
    ArrayList<IonPart> merged = new ArrayList<>(first.size() + second.size());
    // handle conflicts like same name but different mass - or different ion parts
    merged.addAll(first);

    //
    final Map<String, IonPart> existingNameMap = HashMap.newHashMap(first.size());
    for (final IonPart part : first) {
      final String chargedName = part.toString(IonPartStringFlavor.SIMPLE_WITH_CHARGE);
      final IonPart old = existingNameMap.computeIfAbsent(chargedName, k -> part);
      if (old != part) {
        logger.warning(
            "Ion part was already present. This may point to a duplicate in the first ion part list: %s and %s (using the first one)".formatted(
                part.toString(IonPartStringFlavor.FULL_WITH_MASS),
                old.toString(IonPartStringFlavor.FULL_WITH_MASS)));
      }
    }

    for (final IonPart part : second) {
      final IonPart potentialConflict = existingNameMap.get(
          part.toString(IonPartStringFlavor.SIMPLE_NO_CHARGE));
      if (potentialConflict != null && !Objects.equals(potentialConflict, part)) {
        // TODO conflict resolution
        boolean massDiff = Precision.equalDoubleSignificance(part.totalMass(),
            potentialConflict.totalMass());

        logger.warning(
            "Detected conflict between two ion parts: %s and %s (mass within precisions=%s). Will use the first one.".formatted(
                part.toString(IonPartStringFlavor.FULL_WITH_MASS),
                potentialConflict.toString(IonPartStringFlavor.FULL_WITH_MASS), massDiff));
      } else {
        merged.add(part);
      }
    }

    // sort
    merged.sort(IonSorting.getIonPartDefault().createIonPartComparator());
    merged.trimToSize();
    return merged;
  }

  private static List<IonType> mergeTypes(final @NotNull List<IonType> first,
      final @NotNull List<IonType> second) {
    // TODO maybe remap all parts to the global parts list to unify them

    ArrayList<IonType> merged = new ArrayList<>(first.size() + second.size());
    // handle conflicts like same name but different mass - or different ion parts
    merged.addAll(first);
    final Map<String, IonType> existingNameMap = HashMap.newHashMap(first.size());
    for (final IonType ion : first) {
      final IonType old = existingNameMap.computeIfAbsent(ion.name(), k -> ion);
      if (old != ion) {
        logger.warning(
            "Ion type was already present. This may point to a duplicate in the first ion type list: %s and %s (using the first one)".formatted(
                ion.toString(IonTypeStringFlavor.FULL_WITH_MASS),
                old.toString(IonTypeStringFlavor.FULL_WITH_MASS)));
      }
    }

    // add those from second list check for conflicts
    for (IonType ion : second) {
      final IonType potentialConflict = existingNameMap.get(ion.name());
      if (potentialConflict != null && !Objects.equals(potentialConflict, ion)) {
        // TODO conflict resolution
        boolean massDiff = Precision.equalDoubleSignificance(ion.totalMass(),
            potentialConflict.totalMass());

        logger.warning(
            "Detected conflict between two ion types: %s and %s (mass within precisions=%s). Will use the first one.".formatted(
                ion.toString(IonTypeStringFlavor.FULL_WITH_MASS),
                potentialConflict.toString(IonTypeStringFlavor.FULL_WITH_MASS), massDiff));
      } else {
        merged.add(ion);
      }
    }

    // sort by mz etc
    merged.sort(IonSorting.getIonTypeDefault().createIonTypeComparator());

    // trim
    merged.trimToSize();
    return merged;
  }

  @NotNull
  public static IonLibrary loadJson(final @NotNull File file) throws IOException {
    return new ObjectMapper().readValue(file, IonLibrary.class);
  }

  public void saveGlobalLibrary() throws IOException {
    final File file = getGlobalFile();
    saveJson(file);
    globalFileLastModified.set(file.lastModified());
    logger.fine("Saved global ion library to file: " + file.getAbsolutePath());
  }

  public void saveJson(final @NotNull File file) throws IOException {
    FileAndPathUtil.createDirectory(file.getParentFile());
    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file, this);
  }

}
