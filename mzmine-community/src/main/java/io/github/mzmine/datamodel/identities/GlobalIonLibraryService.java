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

import io.github.mzmine.datamodel.identities.IonPart.IonPartStringFlavor;
import io.github.mzmine.datamodel.identities.IonType.IonTypeStringFlavor;
import io.github.mzmine.datamodel.identities.io.IonLibraryPresetStore;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.maths.Precision;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/// The global ion library that is used to create new ion libraries, types, and parts.
///
/// The global ions are constructed from:
/// 1. the internal mzmine defaults in {@link IonTypes} and {@link IonParts}
/// 2. by loading a json file ({@link GlobalIonLibraryIO}) from the presets folder (not a preset but
/// all the global ion definitions)
///
/// {@link IonLibrary} are loaded as presets but they may contain outdated ion definitions
/// (different mass or different formula/name) so they are currently not added to the global ions.
///
/// The global ion library contains:
/// - an {@link IonLibraryPresetStore} to store ion libraries as presets. This also acts as a list
/// of {@link IonLibrary} that are saved as individual files so that they can be shared
/// - a list of {@link IonType} of all uniquely defined ion types so that they may be reused in all
/// libraries
/// - a map of {@link IonPart} to find the singleton instance of a specific ion part.
public final class GlobalIonLibraryService {

  private static final Logger logger = Logger.getLogger(GlobalIonLibraryService.class.getName());


  // lazy init singleton
  private static class Holder {

    private static final GlobalIonLibraryService globalLibrary = new GlobalIonLibraryService();
  }

  private final CloseableReentrantReadWriteLock lock = new CloseableReentrantReadWriteLock();
  /**
   * Used to save ion libraries as presets
   */
  private final IonLibraryPresetStore presetStore = new IonLibraryPresetStore();

  // maps the short name often used to the ion part without count: like Fe will match Fe+3 and Fe+2
  private final Map<String, List<IonPartNoCount>> partDefinitions;
  // used to deduplciate
  private final Map<IonPart, IonPart> singletonParts;
  private final List<IonType> ionTypes;

  /**
   * Initialzies with the defaults in {@link IonTypes} and {@link IonParts}
   */
  public GlobalIonLibraryService() {
    final int numParts = IonParts.PREDEFINED_PARTS.size();
    this.partDefinitions = HashMap.newHashMap(numParts);
    this.singletonParts = HashMap.newHashMap(numParts);

    addParts(IonParts.PREDEFINED_PARTS);

    // ion types
    final int numIonTypes = IonTypes.values().length;
    this.ionTypes = new ArrayList<>(numIonTypes);
    addIonTypes(IonTypes.valuesAsIonType());
  }

  private void addIonTypes(List<IonType> ions) {
    try (var _ = lock.lockWrite()) {
      for (final IonType type : ions) {
        addIonType(type);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void addIonType(IonType type) {
    try (var _ = lock.lockWrite()) {
      type = deduplicateIonType(type);
      ionTypes.add(type);
      for (final IonPart part : type.parts()) {
        addPart(part);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void addParts(List<IonPart> parts) {
    try (var _ = lock.lockWrite()) {
      for (IonPart part : parts) {
        addPart(part);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void addPart(IonPart part) {
    try (var _ = lock.lockWrite()) {
      // adds to map
      final IonPart single = deduplicate(part);

      final String key = single.toString(IonPartStringFlavor.SIMPLE_NO_CHARGE);
      List<IonPartNoCount> values = partDefinitions.computeIfAbsent(key, _ -> new ArrayList<>(1));
      values.add(IonPartNoCount.of(single));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
      var nt = deduplicateIonType(type);
      newTypes.add(nt);
    }

    return newTypes;
  }

  private @NotNull IonType deduplicateIonType(IonType type) {
    return IonType.create(deduplicateIonParts(type.parts()), type.molecules());
  }

  private @NotNull List<IonPart> deduplicateIonParts(final List<@NotNull IonPart> parts) {
    return parts.stream().map(this::deduplicate).toList();
  }


  /**
   * Makes sure that the global library is read from file, also if the file has since changed.
   *
   * @return global ion library
   */
  public static @NotNull GlobalIonLibraryService getGlobalLibrary() {
    return Holder.globalLibrary;
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

  public File getPresetPath() {
    return presetStore.getPresetStoreFilePath();
  }

  public File getGlobalLibraryFile() {
    final File path = getGlobalFile();
    FileAndPathUtil.createDirectory(path);
    return new File(path, "mzmine_global_ions.json");
  }

  private int numIonTypes() {
    return ionTypes.size();
  }

  private int numIonParts() {
    return partDefinitions.size();
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

  public static void loadGlobalLibrary() {
    GlobalIonLibraryIO.loadGlobalIonLibrary();
  }

  public void saveGlobalLibrary() throws IOException {
    GlobalIonLibraryIO.saveGlobalIonLibrary();
  }

  /**
   * @return unmodifiable
   */
  public Map<String, List<IonPartNoCount>> partDefinitions() {
    return Collections.unmodifiableMap(partDefinitions);
  }

  /**
   * @return unmodifiable
   */
  public Map<IonPart, IonPart> singletonParts() {
    return Collections.unmodifiableMap(singletonParts);
  }

  /**
   * @return unmodifiable
   */
  public List<IonType> ionTypes() {
    return Collections.unmodifiableList(ionTypes);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (GlobalIonLibraryService) obj;
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
