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

package io.github.mzmine.datamodel.identities.global;

import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesController;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryChangedEvent.SimpleGlobalIonLibraryChangedEvent;
import io.github.mzmine.datamodel.identities.global.IonLibraryImportResult.MergePolicy;
import io.github.mzmine.datamodel.identities.io.IonLibraryPreset;
import io.github.mzmine.datamodel.identities.io.IonLibraryPresetStore;
import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonPartDefinition;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeUtils;
import io.github.mzmine.datamodel.identities.iontype.IonTypes;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.github.mzmine.util.presets.PresetTypeMismatchException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

  public void addPresetFiles(@NotNull List<File> files, MergePolicy mergePolicy) {
    List<File> brokenFiles = new ArrayList<>();
    List<File> otherPresetType = new ArrayList<>();
    List<IonLibrary> libraries = new ArrayList<>();

    for (File file : files) {
      final IonLibraryPreset loaded;
      try {
        loaded = presetStore.loadFromFileOrThrow(file);
      } catch (IOException e) {
        brokenFiles.add(file);
        continue;
      } catch (PresetTypeMismatchException e) {
        otherPresetType.add(file);
        continue;
      }
      if (loaded != null) {
        libraries.add(loaded.library());
      }
    }

    List<String> notLoadedMsg = new ArrayList<>();
    if (!brokenFiles.isEmpty()) {
      notLoadedMsg.add("Broken preset files not loaded: " + brokenFiles.stream().map(File::getName)
          .collect(Collectors.joining(", ")));
    }
    if (!otherPresetType.isEmpty()) {
      notLoadedMsg.add(
          "Preset files from other preset types not loaded: " + otherPresetType.stream()
              .map(File::getName).collect(Collectors.joining(", ")));
    }

    final IonLibraryImportResult results = addLibraries(libraries, mergePolicy);

    if (!results.skipped().isEmpty()) {
      notLoadedMsg.add("Skipped older libraries and kept existing: " + results.skipped().stream()
          .map(IonLibrary::name).collect(Collectors.joining(", ")));
    }

    if (!notLoadedMsg.isEmpty()) {
      notLoadedMsg.addFirst("Some presets files were skipped:");
      DialogLoggerUtil.showWarningNotification("Some presets skipped",
          String.join("\n", notLoadedMsg));
    }

    // something was added
    if (results.isChanged()) {
      GlobalIonLibrariesController.getInstance().updateModel();
    }
  }

  public void exportPresetsTo(File directory, @NotNull List<IonLibrary> libraries) {
    List<String> saved = new ArrayList<>();
    for (IonLibrary library : libraries) {
      File file = new File(directory, library.name());
      file = presetStore.saveToFile(file, new IonLibraryPreset(library));
      if (file != null) {
        saved.add(file.getAbsolutePath());
      }
    }

    DialogLoggerUtil.showInfoNotification("Exported ion libraries",
        "Exported to:\n" + String.join("\n", saved));
  }

  // lazy init singleton
  private static class Holder {

    private static final GlobalIonLibraryService globalLibrary = new GlobalIonLibraryService();

    static {
      globalLibrary.init();
    }
  }

  // on changes just count up the current version so that other parts like the tab can see if changes happened
  private final AtomicInteger modificationCount = new AtomicInteger(Integer.MIN_VALUE);
  private final AtomicBoolean notifyChanges = new AtomicBoolean(true);
  private final AtomicReference<LocalDateTime> lastGlobalSaved = new AtomicReference<>(null);
  private final AtomicReference<LocalDateTime> lastIonDefinitionChanged = new AtomicReference<>(
      null);

  private final List<GlobalIonLibraryChangedListener> changedListeners = new ArrayList<>();

  /**
   * Only used for parts as there are multiple data structures to update
   */
  private final CloseableReentrantReadWriteLock lock = new CloseableReentrantReadWriteLock();

  private final GlobalIonLibraryValidator validator = new GlobalIonLibraryValidator();

  /**
   * Used to save ion libraries as presets
   */
  private final IonLibraryPresetStore presetStore = new IonLibraryPresetStore();

  /**
   * Scheduled executor for periodic saving of global ion library
   */
  private final ScheduledExecutorService saveScheduler = Executors.newSingleThreadScheduledExecutor(
      r -> {
        Thread t = new Thread(r, "mzmine global ion libraries save thread");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
      });

  /**
   * Libraries are first written to this object and then to the presetStore if they were updated.
   * Use map for uniqueness
   */
  private final Map<UUID, IonLibrary> libraries = new ConcurrentHashMap<>();


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
  }

  private void init() {
    // load global ion definitions that may not be in a library already
    loadGlobalIonDefinitionLibrary();

    // add all internal libraries
    addLibraries(IonLibraries.createDefaultLibrariesModifiable(), MergePolicy.OVERWRITE_ALL);

    // below already requires globalLibrary initialized
    // load presets from disk once for global library
    PropertyUtils.onChangeListDelayed(this::presetsChanged, Duration.millis(150),
        presetStore.getCurrentPresets());
    presetStore.loadAllPresetsOrDefaults();

    // schedule periodic saving of global ion library every 5 seconds
    saveScheduler.scheduleAtFixedRate(() -> {
      try {
        if (lastGlobalSaved.get() == null || !Objects.equals(lastGlobalSaved.get(),
            lastIonDefinitionChanged.get())) {
          saveGlobalIonDefinitionLibrary();
        }
      } catch (IOException e) {
        logger.log(Level.WARNING, "Failed to save global ion library: " + e.getMessage());
      }
    }, 5, 5, TimeUnit.SECONDS);
  }

  private void presetsChanged() {
    final List<IonLibrary> presets = List.copyOf(presetStore.getCurrentPresets()).stream()
        .map(IonLibraryPreset::library).toList();
    // will automatically check IDs and last update dates and only add if new
    addLibraries(presets, MergePolicy.ASK_OLDER_OVERWRITE);
  }

  /**
   * Checks IDs and update dates and uses latest library
   *
   * @return IonLibraryImportResult with added or skipped libraries
   */
  private IonLibraryImportResult addLibraries(List<IonLibrary> libs) {
    return addLibraries(libs, MergePolicy.SKIP_OLDER);
  }

  /**
   * Checks IDs and update dates and uses latest library
   *
   * @return IonLibraryImportResult with added or skipped libraries
   */
  private IonLibraryImportResult addLibraries(List<IonLibrary> libs, MergePolicy mergePolicy) {
    final List<IonLibrary> added = new ArrayList<>();
    final List<IonLibrary> skipped = new ArrayList<>();

    applyLockedChange(() -> {
      boolean wasChanged = false;
      final Map<UUID, IonLibraryPreset> presetMap = presetStore.getPresetMapCopy();
      for (IonLibrary lib : libs) {
        final IonLibrary existingLib = libraries.get(lib.id());
        if (existingLib != null) {
          if (lib.lastUpdatedDate().isEqual(existingLib.lastUpdatedDate())) {
            continue;
          } else if (lib.lastUpdatedDate().isBefore(existingLib.lastUpdatedDate())) {
            // old libraries usually do not overwrite newer version
            // only possible with user interaction if ask is true
            boolean skip = switch (mergePolicy) {
              case SKIP_OLDER -> true;
              case OVERWRITE_ALL -> false;
              case ASK_OLDER_OVERWRITE -> !DialogLoggerUtil.showDialogYesNo(
                  "Overwriting existing newer library with an older version?", """
                      Should the older version ion library overwrite the already present newer version?
                      Incoming older version: %s (last updated %s)
                      Existing newer version: %s (last updated %s)""".formatted(existingLib,
                      existingLib.lastUpdatedDate(), lib, lib.lastUpdatedDate()));
            };

            if (skip) {
              skipped.add(lib);
              continue;
            }
          }
        }
        logger.fine("Adding library " + lib.name());
        // add library and all ion types and definitions
        added.add(lib);
        libraries.put(lib.id(), lib);
        wasChanged = true;

        final IonLibraryPreset oldPreset = presetMap.get(lib.id());
        if (!lib.isInternalLibrary() && (oldPreset == null || oldPreset.library().lastUpdatedDate()
            .isBefore(lib.lastUpdatedDate()))) {
          presetStore.addAndSavePreset(new IonLibraryPreset(lib), true);
        }

        addIonTypes(lib.ions());
        addParts(IonTypeUtils.extractUniqueParts(lib.ions()));
        addPartDefinitions(IonTypeUtils.extractUniquePartsIgnoreCounts(lib.ions(), true));
      }

      return wasChanged;
    });
    return new IonLibraryImportResult(added, skipped);
  }

  /**
   * Checks IDs and update dates and uses latest library
   *
   * @return true if model changed
   */
  private boolean removeLibraries(List<IonLibrary> libs) {
    return applyLockedChange(() -> {
      boolean wasChanged = false;
      final Map<UUID, IonLibraryPreset> presetMap = presetStore.getPresetMapCopy();
      for (IonLibrary lib : libs) {
        if (lib.isInternalLibrary()) {
          continue;
        }

        if (libraries.remove(lib.id()) != null) {
          wasChanged = true;
        }

        // remove old presets that were changed and keep track which libraries were changed and need saving
        final IonLibraryPreset old = presetMap.get(lib.id());
        if (old != null) {
          // remove old presets because new ones may have different name or content
          presetStore.removePresetsWithName(old);
        }
      }
      return wasChanged;
    });
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
  private boolean applyLockedChange(BooleanSupplier runnable) {
    boolean wasChanged = false;
    try (var _ = lock.lockWrite()) {
      final boolean oldNotify = notifyChanges.getAndSet(false);
      try {
        wasChanged = runnable.getAsBoolean();
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Failed to update global ions" + ex.getMessage(), ex);
      }
      notifyChanges.set(oldNotify);
      if (wasChanged) {
        notifyChange();
      }
    }
    return wasChanged;
  }

  /// Versioned apply: validate, check that the caller's base version is still current, and only
  /// then mutate. All three phases run inside a single write lock so a racing writer can't slip in
  /// between the version check and the mutation.
  ///
  /// Returns one of {@link ApplyResult.Applied}, {@link ApplyResult.Invalid}, or
  /// {@link ApplyResult.Conflict}. Callers must handle all three — the sealed return type makes
  /// that obligation visible at the type level.
  ///
  /// @param expectedBaseVersion           the service version the caller built {@code proposed}
  /// against; typically {@code model.getRetrievalVersion()}
  /// @param proposed                      the full desired state (libraries, types, parts, part
  /// definitions)
  /// @param applyDirectlyIgnoreValidation
  public @NotNull ApplyResult applyUpdates(int expectedBaseVersion,
      @NotNull GlobalIonLibraryDTO proposed, boolean applyDirectlyIgnoreValidation) {

    try (var _ = lock.lockWrite()) {
      // cascade libraries -> ion types -> ion parts -> definitions
      // so that we add all definitions from libraries
      proposed = proposed.cascadeIonDefinitionsFromLibraries();

      final int currentVersion = getVersion();
      final GlobalIonLibraryDTO currentDto = snapshotWithinLock(currentVersion);
      if (!applyDirectlyIgnoreValidation && currentVersion != expectedBaseVersion) {
        // base version is different so validate the changes first
        final ValidationResult vr = validator.validate(proposed, currentDto);
        if (vr.hasErrors()) {
          return new ApplyResult.Invalid(vr);
        }
      }

      // validation passed
      final boolean oldNotify = notifyChanges.getAndSet(false);
      try {
        setIonPartDefinitions(proposed.partDefinitions());
        setIonParts(proposed.parts());
        setIonTypes(proposed.types());
        setLibraries(proposed.libraries());
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Failed to apply global ion library update: " + ex.getMessage(),
            ex);
      } finally {
        notifyChanges.set(oldNotify);
      }
      notifyChange();
      return new ApplyResult.Applied(getVersion());
    }
  }

  /// Snapshot without re-acquiring the read lock (we hold the write lock already).
  private @NotNull GlobalIonLibraryDTO snapshotWithinLock(int version) {
    return new GlobalIonLibraryDTO(version, getIonLibrariesUnmodifiable(),
        getIonTypesUnmodifiable(), getIonPartsUnmodifiable(), getIonPartDefinitionsCopy());
  }

  // currently not used but maybe later to have finer report on changes
  private static @NotNull ConflictReport diffLibraries(@NotNull List<IonLibrary> proposed,
      @NotNull List<IonLibrary> current) {
    final Map<UUID, IonLibrary> proposedById = HashMap.newHashMap(proposed.size());
    for (IonLibrary lib : proposed) {
      proposedById.put(lib.id(), lib);
    }
    final Map<UUID, IonLibrary> currentById = HashMap.newHashMap(current.size());
    for (IonLibrary lib : current) {
      currentById.put(lib.id(), lib);
    }

    final Set<UUID> sameIdDifferentContent = new HashSet<>();
    final Set<UUID> onlyInProposed = new HashSet<>();
    final Set<UUID> onlyInCurrent = new HashSet<>();

    for (Map.Entry<UUID, IonLibrary> e : proposedById.entrySet()) {
      final IonLibrary other = currentById.get(e.getKey());
      if (other == null) {
        onlyInProposed.add(e.getKey());
      } else if (!e.getValue().name().equals(other.name()) || !e.getValue().equalIons(other)) {
        sameIdDifferentContent.add(e.getKey());
      }
    }
    for (UUID id : currentById.keySet()) {
      if (!proposedById.containsKey(id)) {
        onlyInCurrent.add(id);
      }
    }
    return new ConflictReport(Set.copyOf(sameIdDifferentContent), Set.copyOf(onlyInProposed),
        Set.copyOf(onlyInCurrent));
  }

  /**
   * @return true if model changed
   */
  private boolean setIonPartDefinitions(List<IonPartDefinition> definitions) {
    return applyLockedChange(() -> {
      boolean wasChanged = false;
      final Set<IonPartDefinition> toInclude = Set.copyOf(definitions);

      // remove from each list if does not exist
      for (List<IonPartDefinition> oldList : partDefinitions.values()) {
        int oldSize = oldList.size();
        oldList.removeIf(def -> !toInclude.contains(def));
        if (oldSize != oldList.size()) {
          wasChanged = true;
        }
      }
      partDefinitions.entrySet().removeIf(e -> e.getValue().isEmpty());

      // add all
      for (IonPartDefinition def : definitions) {
        if (addIonPartDefinitionInternal(def)) {
          wasChanged = true;
        }
      }

      if (!wasChanged) {
        lastIonDefinitionChanged.set(LocalDateTime.now());
      }
      return wasChanged;
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
      boolean wasChanged = false;
      final Set<IonType> toInclude = Set.copyOf(types);

      int oldSize = singletonIons.size();
      this.singletonIons.entrySet().removeIf(e -> !toInclude.contains(e.getValue()));
      if (oldSize != singletonIons.size()) {
        wasChanged = true;
      }

      if (addIonTypes(types)) {
        wasChanged = true;
      }
      return wasChanged;
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
      boolean wasChanged = false;
      final Set<IonPart> toInclude = Set.copyOf(parts);

      int oldSize = singletonParts.size();
      this.singletonParts.entrySet().removeIf(e -> !toInclude.contains(e.getValue()));
      if (oldSize != singletonParts.size()) {
        wasChanged = true;
      }

      if (addParts(parts)) {
        wasChanged = true;
      }
      return wasChanged;
    });
  }

  /**
   * Update the internal data structures to the lists of libraries, types and parts. This is
   * important when the {@link GlobalIonLibrariesController} applies changes and pushes the changes
   * to the global library.
   * <p>
   * All internal lists are cleared and exchanged for the arguments.
   *
   * @return true if model changed
   */
  private boolean setLibraries(List<IonLibrary> newLibs) {
    return applyLockedChange(() -> {
      final Set<@NotNull UUID> newIds = newLibs.stream().map(IonLibrary::id)
          .collect(Collectors.toSet());
      // remove all libraries that are not part of newLibs, they were deleted
      final List<IonLibrary> libsToRemove = libraries.values().stream()
          .filter(lib -> !lib.isInternalLibrary() && !newIds.contains(lib.id())).toList();

      boolean wasChanged = removeLibraries(libsToRemove);

      final IonLibraryImportResult results = addLibraries(newLibs, MergePolicy.OVERWRITE_ALL);
      if (results.isChanged()) {
        wasChanged = true;
      }
      return wasChanged;
    });
  }


  /**
   * @return The default mzmine libraries and all user preset libraries
   */
  public List<IonLibrary> getIonLibrariesUnmodifiable() {
    // need to add the internal mzmine default libraries here as they should not be saved as presets
    return List.copyOf(libraries.values());
  }


  /**
   * @param name ignores case
   * @return
   */
  @NotNull
  public Optional<IonLibrary> getLibraryForName(@NotNull String name) {
    final List<IonLibrary> libraries = getIonLibrariesUnmodifiable();
    for (IonLibrary lib : libraries) {
      if (lib.name().equalsIgnoreCase(name)) {
        return Optional.of(lib);
      }
    }
    return Optional.empty();
  }

  @NotNull
  public Optional<IonLibrary> getLibraryForID(@NotNull UUID id) {
    return Optional.ofNullable(libraries.get(id));
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
      addIonPartDefinitionInternal(IonPartDefinition.of(part));
      notifyChange();

      return part;
    }
  }

  /**
   * adds an ion part definition. caller should call notify changes once all changes are done
   *
   * @return true if model was changed
   */
  private boolean addIonPartDefinitionInternal(@NotNull IonPartDefinition part) {
    // so that Fe might be defined as 2+ or 3+
    if (!part.isDefinitionRequired()) {
      // name and formula are equal and no charge means this is the default behavior of the parsing.
      // no need to keep an instance of this, so skip
      // only add definitions where name is different from formula or where charge is defined
      // do not add to many to reduce number of definitions in {@link GlobalIonLibrariesController}
      return false;
    }

    final String key = part.name();
    List<IonPartDefinition> values = partDefinitions.computeIfAbsent(key, _ -> new ArrayList<>(1));

    if (values.contains(part)) {
      return false;
    }
    values.add(part);

    lastIonDefinitionChanged.set(LocalDateTime.now());
    return true;
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
   *
   * @return true if model changed
   */
  public boolean addIonTypes(List<IonType> ions) {
    return applyLockedChange(() -> {
      boolean wasChanged = false;
      for (final IonType type : ions) {
        IonType single = singletonIons.get(type);
        if (single != null) {
          continue;
        }
        deduplicateTypeAndAdd(type);
        wasChanged = true;
      }
      return wasChanged;
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
   *
   * @return true if model changed
   */
  public boolean addParts(List<IonPart> parts) {
    return applyLockedChange(() -> {
      boolean wasChanged = false;
      for (final IonPart part : parts) {
        IonPart single = singletonParts.get(part);
        if (single != null) {
          continue;
        }
        deduplicateAndAdd(part);
        wasChanged = true;
      }
      return wasChanged;
    });
  }

  /**
   * Just add part definition. will not trigger a change as {@link GlobalIonLibrariesController} and
   * this service are at the same state for the definitions.
   */
  public void addPartDefinition(IonPartDefinition partDef) {
    try (var _ = lock.lockWrite()) {
      addIonPartDefinitionInternal(partDef);
    }
  }

  /**
   * Just add part definition. will not trigger a change as {@link GlobalIonLibrariesController} and
   * this service are at the same state for the definitions.
   */
  public void addPartDefinitions(List<IonPartDefinition> partDefinitions) {
    try (var _ = lock.lockWrite()) {
      for (IonPartDefinition partDef : partDefinitions) {
        addIonPartDefinitionInternal(partDef);
      }
    }
  }

  /**
   * Just remove part definition. will not trigger a change as {@link GlobalIonLibrariesController}
   * and this service are at the same state for the definitions.
   *
   * @return true if model changed
   */
  public boolean removePartDefinition(IonPartDefinition partDef) {
    try (var _ = lock.lockWrite()) {
      final List<IonPartDefinition> definitions = partDefinitions.get(partDef.name());
      if (definitions == null) {
        return false;
      }

      // remove definition from list and remove list if empty
      final boolean removed = definitions.remove(partDef);
      if (definitions.isEmpty()) {
        partDefinitions.remove(partDef.name());
      }
      if (removed) {
        lastIonDefinitionChanged.set(LocalDateTime.now());
      }
      return removed;
    }
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
  @NotNull
  public List<IonPartDefinition> findPartsByName(String name) {
    final List<IonPartDefinition> definitions = partDefinitions.get(name);
    return definitions == null ? List.of() : definitions;
  }

  @NotNull
  public static File getGlobalFile() {
    return new File(getPresetPath(), "mzmine_global_ions_definitions.json");
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

  public static void loadGlobalIonDefinitionLibrary() {
    GlobalIonLibraryIO.loadGlobalIonDefinitionLibrary();
  }

  public void saveGlobalIonDefinitionLibrary() throws IOException {
    final LocalDateTime time = lastIonDefinitionChanged.get();
    if (GlobalIonLibraryIO.saveGlobalIonDefinitionLibrary(getGlobalFile())) {
      lastGlobalSaved.set(time);
    }
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
