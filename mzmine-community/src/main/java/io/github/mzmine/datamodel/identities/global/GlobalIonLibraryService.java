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

import io.github.mzmine.datamodel.identities.IonLibraries;
import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.datamodel.identities.IonPart;
import io.github.mzmine.datamodel.identities.IonPart.IonPartStringFlavor;
import io.github.mzmine.datamodel.identities.IonPartDefinition;
import io.github.mzmine.datamodel.identities.IonPartSorting;
import io.github.mzmine.datamodel.identities.IonParts;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.IonType.IonTypeStringFlavor;
import io.github.mzmine.datamodel.identities.IonTypeSorting;
import io.github.mzmine.datamodel.identities.IonTypes;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesController;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryChangedEvent.SimpleGlobalIonLibraryChangedEvent;
import io.github.mzmine.datamodel.identities.io.IonLibraryPreset;
import io.github.mzmine.datamodel.identities.io.IonLibraryPresetStore;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.github.mzmine.util.maths.Precision;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Duration;
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

  // on changes just count up the current version so that other parts like the tab can see if changes happened
  private final AtomicInteger modificationCount = new AtomicInteger(Integer.MIN_VALUE);
  private final AtomicBoolean notifyChanges = new AtomicBoolean(true);

  private final List<GlobalIonLibraryChangedListener> changedListeners = new ArrayList<>();

  /**
   * Only used for parts as there are multiple data structures to update
   */
  private final CloseableReentrantReadWriteLock lock = new CloseableReentrantReadWriteLock();

  /**
   * Used to save ion libraries as presets
   */
  private final IonLibraryPresetStore presetStore = new IonLibraryPresetStore();

  /// maps the short name often used to the ion part without count: like Fe will match Fe+3 and Fe+2
  /// We only store definitions for ion parts where the name is unequal to the formula. For example,
  /// to capture alternative names of chemicals or to use a different formula order.
  ///
  /// Examples:
  /// - Fe could also be presented as Fe(II)
  /// - MeOH for methanol
  /// - NH3 (different order) instead of default: H3N
  private final Map<String, List<IonPartDefinition>> partDefinitions;

  // used to deduplciate
  private final Map<IonPart, IonPart> singletonParts;
  private final Map<IonType, IonType> singletonIons;


  /**
   * Initializes with the defaults in {@link IonTypes} and {@link IonParts}
   */
  GlobalIonLibraryService() {
    final int numParts = IonParts.PREDEFINED_PARTS.size();
    this.partDefinitions = new ConcurrentHashMap<>(numParts);
    this.singletonParts = new ConcurrentHashMap<>(numParts);
    final int numIonTypes = IonTypes.values().length;
    this.singletonIons = new ConcurrentHashMap<>(numIonTypes);

    // first parts then types
    addParts(IonParts.PREDEFINED_PARTS);
    addIonTypes(IonTypes.valuesAsIonType());

    // load presets from disk once for global library
    presetStore.loadAllPresetsOrDefaults();
    // the preset store has an observable list and is only updated on the fx thread
    // this means that there is already a delay between calling the change and the change happining on fx thread
    // accumulate multiple changes here to avoid each addition/removal to trigger a change
    PropertyUtils.onChangeListDelayed(this::notifyChange, Duration.millis(30),
        presetStore.getCurrentPresets());
  }

  public synchronized void addChangeListener(GlobalIonLibraryChangedListener listener) {
    changedListeners.add(listener);
  }

  /**
   * One static image of the global ion library at a specific version. Used to access all variables
   * within a read lock. All unmodifiable.
   */
  public GlobalIonLibraryDTO getCurrentGlobalLibrary() {
    try (var _ = lock.lockRead()) {
      return new GlobalIonLibraryDTO(getVersion(), getIonLibrariesUnmodifiable(),
          getIonTypesUnmodifiable(), getIonPartsUnmodifiable(), getIonPartDefinitionsCopy());
    }
  }

  /**
   * The current version, tracks every modification and is thread-safe
   */
  public int getVersion() {
    return modificationCount.get();
  }

  /**
   * Changes the modification count and calls listeners. Can be turned off to accumulate changes by
   * {@link #notifyChanges)}
   */
  public void notifyChange() {
    if (!notifyChanges.get()) {
      return;
    }

    final int version = modificationCount.incrementAndGet();
    final var event = new SimpleGlobalIonLibraryChangedEvent(version);
    // notify listeners if needed
    changedListeners.forEach(l -> l.globalIonsChanged(event));
  }

  /**
   * Applies write lock and accumulates multiple changes into a single change event
   */
  private void applyLockedChange(Runnable runnable) {
    try (var _ = lock.lockWrite()) {
      final boolean oldNotify = notifyChanges.getAndSet(false);
      try {
        runnable.run();
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Failed to update global ions" + ex.getMessage(), ex);
      }
      notifyChanges.set(oldNotify);
      notifyChange();
    }
  }

  /**
   * Update the internal data structures to the lists of libraries, types and parts. This is
   * important when the {@link GlobalIonLibrariesController} applies changes and pushes the changes
   * to the global library.
   * <p>
   * All internal lists are cleared and exchanged for the arguments.
   */
  public void applyUpdates(List<IonLibrary> libraries, List<IonType> types, List<IonPart> parts,
      List<IonPartDefinition> partDefinitions) {
    applyLockedChange(() -> {
      setIonPartDefinitions(partDefinitions);
      setIonParts(parts);
      setIonTypes(types);
      setLibraries(libraries);
    });
  }

  private void setIonPartDefinitions(List<IonPartDefinition> definitions) {
    applyLockedChange(() -> {
      this.partDefinitions.clear();
      for (IonPartDefinition def : definitions) {
        addIonPartDefinition(def);
      }
    });
  }

  /**
   * Update the internal data structures to the lists of libraries, types and parts. This is
   * important when the {@link GlobalIonLibrariesController} applies changes and pushes the changes
   * to the global library.
   * <p>
   * All internal lists are cleared and exchanged for the arguments.
   */
  private void setIonTypes(List<IonType> types) {
    applyLockedChange(() -> {
      this.singletonIons.clear();
      addIonTypes(types);
    });
  }

  /**
   * Update the internal data structures to the lists of libraries, types and parts. This is
   * important when the {@link GlobalIonLibrariesController} applies changes and pushes the changes
   * to the global library.
   * <p>
   * All internal lists are cleared and exchanged for the arguments.
   */
  private void setIonParts(List<IonPart> parts) {
    applyLockedChange(() -> {
      this.singletonParts.clear();
      addParts(parts);
    });
  }

  /**
   * Update the internal data structures to the lists of libraries, types and parts. This is
   * important when the {@link GlobalIonLibrariesController} applies changes and pushes the changes
   * to the global library.
   * <p>
   * All internal lists are cleared and exchanged for the arguments.
   */
  private void setLibraries(List<IonLibrary> libraries) {
    applyLockedChange(() -> {
      Map<IonLibrary, Boolean> saveNewLibraries = new HashMap<>();
      // Add all libraries with true value
      for (IonLibrary lib : libraries) {
        saveNewLibraries.put(lib, true);
      }

      final List<IonLibraryPreset> oldPresets = List.copyOf(presetStore.getCurrentPresets());

      // remove old presets that were changed and keep track which libraries were changed and need saving
      for (IonLibraryPreset old : oldPresets) {
        final boolean isUnchanged = saveNewLibraries.containsKey(old.library());
        if (isUnchanged) {
          saveNewLibraries.put(old.library(), false);
        } else {
          // remove old presets because new ones may have different name or content
          presetStore.removePresetsWithName(old);
        }
      }

      // save changed libraries
      saveNewLibraries.forEach((lib, save) -> {
        if (save) {
          presetStore.addAndSavePreset(new IonLibraryPreset(lib), true);
        }
      });
    });
  }

  /**
   * @return The default mzmine libraries and all user preset libraries
   */
  public List<IonLibrary> getIonLibrariesUnmodifiable() {
    // need to add the internal mzmine default libraries here as they should not be saved as presets
    final List<IonLibrary> allLibraries = IonLibraries.createDefaultLibrariesModifiable();
    final List<IonLibraryPreset> presets = List.copyOf(presetStore.getCurrentPresets());
    for (IonLibraryPreset preset : presets) {
      allLibraries.add(preset.library());
    }
    return allLibraries;
  }


  /**
   * Deduplicates {@link IonPart} instances and adds new parts to the global library
   * <p>
   * IonPart is unmodifiable and we can use the same instance in all places for something like +2H
   * or another for +1H
   *
   * @return the single instance
   */
  public IonPart deduplicateAndAdd(IonPart part) {
    IonPart single = singletonParts.get(part);
    if (single != null) {
      return single;
    }
    // modifiyng multiple data structures, so we need to lock and double check
    try (var _ = lock.lockWrite()) {
      // check again if part was added in the meantime
      single = singletonParts.get(part);
      if (single != null) {
        return single;
      }

      // finally add new part
      singletonParts.put(part, part);
      addIonPartDefinition(IonPartDefinition.of(part));
      notifyChange();

      return part;
    }
  }

  /**
   * adds an ion part definition. caller should call notify changes once all changes are done
   */
  private void addIonPartDefinition(@NotNull IonPartDefinition part) {
    // so that Fe might be defined as 2+ or 3+
    if (part.name().equals(part.formula()) && part.isNeutralModification()) {
      // name and formula are equal and no charge means this is the default behavior of the parsing.
      // no need to keep an instance of this, so skip
      // only add definitions where name is different from formula or where charge is defined
      // do not add to many to reduce number of definitions in {@link GlobalIonLibrariesController}
      return;
    }

    final String key = part.name();
    List<IonPartDefinition> values = partDefinitions.computeIfAbsent(key, _ -> new ArrayList<>(1));
    values.add(part);
  }

  /**
   * Deduplicates the {@link IonPart} instances but only if they are captured in the global lirbary.
   * Otherwise the original instance is used.
   * <p>
   * IonPart is unmodifiable and we can use the same instance in all places for something like +2H
   * or another for +1H
   *
   * @return the single instance or the part itself if there is no singleton available
   */
  public IonPart deduplicateAvailable(IonPart part) {
    return singletonParts.getOrDefault(part, part);
  }


  /**
   * Adds all new types to the global list of singletons (checks for existing)
   */
  public void addIonTypes(List<IonType> ions) {
    applyLockedChange(() -> {
      for (final IonType type : ions) {
        deduplicateTypeAndAdd(type);
      }
    });
  }

  /**
   * Deduplicates the IonParts used in all IonTypes - creates a new list
   *
   * @param addToGlobal true: add to global list, false: to not add, will keep unknown instances as
   *                    no singletons
   * @return creates a new list of IonTypes
   */
  public List<IonType> deduplicateIonTypes(List<IonType> ionTypes, boolean addToGlobal) {
    List<IonType> newTypes = new ArrayList<>();
    for (final IonType type : ionTypes) {
      var nt = addToGlobal ? deduplicateTypeAndAdd(type) : deduplicateAvailableType(type);
      newTypes.add(nt);
    }

    return newTypes;
  }

  /**
   * Deduplicate ion type with global instance, only if available, otherwise just return the
   * instance. This might be preferred over {@link #deduplicateTypeAndAdd(IonType)} for underdefined
   * ion types.
   *
   * @return the single instance or itself if not in global library.
   */
  public @NotNull IonType deduplicateAvailableType(IonType type) {
    return singletonIons.getOrDefault(type, type);
  }

  /**
   * Deduplicate ion type with global instance or add to global list
   *
   * @return the single instance
   */
  public @NotNull IonType deduplicateTypeAndAdd(IonType type) {
    IonType single = singletonIons.get(type);
    if (single != null) {
      return single;
    }

    // double lock
    try (var _ = lock.lockWrite()) {
      single = singletonIons.get(type);
      if (single != null) {
        return single;
      }

      // deduplicate parts and add new ion type to global
      final IonType dedup = IonType.create(deduplicateIonParts(type.parts(), true),
          type.molecules());
      singletonIons.put(dedup, dedup);
      notifyChange();

      return dedup;
    }
  }


  /**
   * Add all parts to the global list
   */
  public void addParts(List<IonPart> parts) {
    applyLockedChange(() -> {
      for (final IonPart part : parts) {
        deduplicateAndAdd(part);
      }
    });
  }

  /**
   *
   * @param addToGlobal true: add to global list, false: do not add and may return non signleton
   *                    instances for missing parts
   * @return a new list of deduplicated parts, same order
   */
  public @NotNull List<IonPart> deduplicateIonParts(final List<@NotNull IonPart> parts,
      boolean addToGlobal) {
    List<IonPart> newParts = new ArrayList<>(parts.size());
    for (IonPart part : parts) {
      final IonPart single = addToGlobal ? deduplicateAndAdd(part) : deduplicateAvailable(part);
      newParts.add(single);
    }
    return newParts;
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
  public List<IonPartDefinition> findPartsByName(String name) {
    return partDefinitions.get(name);
  }

  @NotNull
  public static File getGlobalFile() {
    return new File(getPresetPath(), "libraries/mzmine_global_ions.json");
  }

  public static @NotNull File getPresetPath() {
    return getGlobalLibrary().presetStore.getPresetStoreFilePath();
  }

  private int numIonTypes() {
    return singletonIons.size();
  }

  private int numIonParts() {
    return singletonParts.size();
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
  public List<IonPart> getIonPartsUnmodifiable() {
    return List.copyOf(singletonParts.values());
  }

  /**
   * @return modifiable copy
   */
  public List<IonPartDefinition> getIonPartDefinitionsCopy() {
    final List<List<IonPartDefinition>> current = List.copyOf(partDefinitions.values());
    final List<IonPartDefinition> definitions = new ArrayList<>(current.size());
    for (List<IonPartDefinition> def : current) {
      definitions.addAll(def);
    }

    return definitions;
  }

  /**
   * @return unmodifiable
   */
  public List<IonType> getIonTypesUnmodifiable() {
    return List.copyOf(singletonIons.values());
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
        this.singletonParts, that.singletonParts) && Objects.equals(this.singletonIons,
        that.singletonIons);
  }

  @Override
  public int hashCode() {
    return Objects.hash(partDefinitions, singletonParts, singletonIons);
  }

  @Override
  public String toString() {
    return "GlobalIonLibrary[" + "partDefinitions=" + partDefinitions + ", " + "singletonParts="
        + singletonParts + ", " + "ionTypes=" + singletonIons + ']';
  }


}
