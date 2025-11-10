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
import io.github.mzmine.datamodel.identities.fx.sub.IonPartSorting;
import io.github.mzmine.datamodel.identities.fx.sub.IonTypeSorting;
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
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public final class GlobalIonLibrary {

  private static final Logger logger = Logger.getLogger(GlobalIonLibrary.class.getName());


  /**
   * Track the last modified state of the loaded global library to see if changes need to be loaded
   */
  private static GlobalIonLibrary globalLibrary;
  private static final AtomicLong globalFileLastModified = new AtomicLong(-1);


  // maps the short name often used to the ion part without count: like Fe will match Fe+3 and Fe+2
  private final Map<String, List<IonPartNoCount>> partDefinitions;
  // used to deduplciate
  private final Map<IonPart, IonPart> singletonParts;
  private final List<IonType> ionTypes;

  /**
   * @param partDefinitions key: simple name like +Fe might have multiple options like Fe+2 and
   *                        Fe+3, which are put into a list of potential ion parts resolved for this
   *                        simple name in definitions like [M+Fe-H]+ to find the correct charge
   *                        state
   * @param ionTypes
   */
  public GlobalIonLibrary(Map<String, List<IonPartNoCount>> partDefinitions,
      List<IonType> ionTypes) {
    this.partDefinitions = partDefinitions;
    this.ionTypes = ionTypes;
    this.singletonParts = new HashMap<>();

    for (final IonType type : ionTypes) {
      for (final IonPart part : type.parts()) {
        deduplicate(part);
      }
    }
  }

  public GlobalIonLibrary(List<IonPart> parts, List<IonType> ionTypes) {
    final Map<String, List<IonPartNoCount>> map = HashMap.newHashMap(parts.size());
    for (final IonPart part : parts) {
      final String key = part.toString(IonPartStringFlavor.SIMPLE_NO_CHARGE);
      List<IonPartNoCount> values = map.computeIfAbsent(key, p -> new ArrayList<>(1));
      values.add(IonPartNoCount.of(part));
    }
    this(map, ionTypes);
  }

  /**
   * IonPart is unmodifiable and we can use the same instance in all places for something like +2H
   * or another for +1H
   *
   * @return the single instance
   */
  public IonPart deduplicate(IonPart part) {
    return singletonParts.computeIfAbsent(part, Function.identity());
  }

  /**
   * Deduplciates the IonParts used in all IonTypes - creates a new list
   *
   * @param ionTypes
   * @return creates a new list of IonTypes
   */
  public List<IonType> deduplicateIonTypes(List<IonType> ionTypes) {
    List<IonType> newTypes = new ArrayList<>();
    for (final IonType type : ionTypes) {
      var nt = IonType.create(deduplicateIonParts(type.parts()), type.molecules());
      newTypes.add(nt);
    }

    return newTypes;
  }

  private @NotNull List<IonPart> deduplicateIonParts(final List<@NotNull IonPart> parts) {
    return parts.stream().map(this::deduplicate).toList();
  }


  /**
   * Makes sure that the global library is read from file, also if the file has since changed.
   *
   * @return global ion library
   */
  public static @NotNull GlobalIonLibrary getGlobalLibrary() {
    File file = getGlobalFile();
    if (globalLibrary == null || (file.exists()
        && file.lastModified() != globalFileLastModified.get())) {
      globalLibrary = loadGlobalIonLibrary();
    }

    return globalLibrary;
  }

  /**
   * @param name the name without count, like Fe for +2Fe
   * @return
   */
  public List<IonPartNoCount> findPartsByName(String name) {
    return partDefinitions.get(name);
  }


  public static File getGlobalFile() {
    return FileAndPathUtil.resolveInMzmineDir("libraries/mzmine_global_ions.json");
  }

  /**
   * Loads the global library from file, also if the library has changed since the last import
   *
   * @return a global ion library
   */
  private static synchronized GlobalIonLibrary loadGlobalIonLibrary() {
    // maybe already initialized - then no need to add mzmine internal ions
    // just check if reload of file is needed
    boolean alreadyInitialized = globalFileLastModified.get() != -1;

    File file = getGlobalFile();
    GlobalIonLibrary global = null;
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
    final GlobalIonLibrary internalLibrary;
    if (alreadyInitialized) {
      internalLibrary = globalLibrary;
    } else {
      // might also have new internal ion types defined in mzmine - combine these into a new library
      var types = Arrays.stream(IonTypes.values()).map(IonTypes::asIonType)
          .collect(Collectors.toCollection(ArrayList::new));
      internalLibrary = new GlobalIonLibrary(new ArrayList<>(IonParts.PREDEFINED_PARTS), types);
    }

    // merge both libraries: from file and internal
    GlobalIonLibrary merged = GlobalIonLibrary.merge(global, internalLibrary);

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
    return partDefinitions.size();
  }

  /**
   * merge two libraries
   *
   * @return merges two non-null libraries into a new instance. Or null if both are null or returns
   * the original instance that is non-null
   */
  public static GlobalIonLibrary merge(final @Nullable GlobalIonLibrary first,
      final @Nullable GlobalIonLibrary second) {
    if (first == null && second == null) {
      return null;
    }
    if (first == null) {
      return second;
    }
    if (second == null) {
      return first;
    }

    // TODO load simplified.
//    List<IonPart> mergedParts = mergeParts(first.parts(), second.parts());
    List<IonType> ionTypes = mergeTypes(first.ionTypes(), second.ionTypes());

    return new GlobalIonLibrary(List.of(), ionTypes);
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
      if (potentialConflict != null) {
        if (!Objects.equals(potentialConflict, part)) {
          // TODO conflict resolution
          boolean massDiff = Precision.equalDoubleSignificance(part.totalMass(),
              potentialConflict.totalMass());

          logger.warning(
              "Detected conflict between two ion parts: %s and %s (mass within precisions=%s). Will use the first one.".formatted(
                  part.toString(IonPartStringFlavor.FULL_WITH_MASS),
                  potentialConflict.toString(IonPartStringFlavor.FULL_WITH_MASS), massDiff));
        }
        // otherwise its equal and nothing to do then
      } else {
        merged.add(part);
      }
    }

    // sort
    merged.sort(IonPartSorting.DEFAULT_NEUTRAL_THEN_LOSSES_THEN_ADDED.getComparator());
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
      if (potentialConflict != null) {
        // otherwise its equal, nothing to do then
        if (!Objects.equals(potentialConflict, ion)) {
          // TODO conflict resolution
          boolean massDiff = Precision.equalDoubleSignificance(ion.totalMass(),
              potentialConflict.totalMass());

          logger.warning(
              "Detected conflict between two ion types: %s and %s (mass within precisions=%s). Will use the first one.".formatted(
                  ion.toString(IonTypeStringFlavor.FULL_WITH_MASS),
                  potentialConflict.toString(IonTypeStringFlavor.FULL_WITH_MASS), massDiff));
        }
      } else {
        merged.add(ion);
      }
    }

    // sort by mz etc
    merged.sort(IonTypeSorting.getIonTypeDefault().getComparator());

    // trim
    merged.trimToSize();
    return merged;
  }

  @NotNull
  public static GlobalIonLibrary loadJson(final @NotNull File file) throws IOException {
    return new ObjectMapper().readValue(file, GlobalIonLibrary.class);
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

  public Map<String, List<IonPartNoCount>> partDefinitions() {
    return partDefinitions;
  }

  public Map<IonPart, IonPart> singletonParts() {
    return singletonParts;
  }

  public List<IonType> ionTypes() {
    return ionTypes;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (GlobalIonLibrary) obj;
    return Objects.equals(this.partDefinitions, that.partDefinitions) && Objects.equals(
        this.singletonParts, that.singletonParts) && Objects.equals(this.ionTypes, that.ionTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(partDefinitions, singletonParts, ionTypes);
  }

  @Override
  public String toString() {
    return "GlobalIonLibrary[" + "partDefinitions=" + partDefinitions + ", " + "singletonParts="
        + singletonParts + ", " + "ionTypes=" + ionTypes + ']';
  }


}
