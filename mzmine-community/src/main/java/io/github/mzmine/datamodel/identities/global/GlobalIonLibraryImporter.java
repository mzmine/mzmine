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

import io.github.mzmine.datamodel.identities.cloud.CloudCatalog;
import io.github.mzmine.datamodel.identities.cloud.CloudCatalog.RemoteLibraryRef;
import io.github.mzmine.datamodel.identities.global.IonLibraryImportResult.MergePolicy;
import io.github.mzmine.datamodel.identities.global.IonLibraryImportResult.RenamedImport;
import io.github.mzmine.datamodel.identities.io.IonLibraryIO;
import io.github.mzmine.datamodel.identities.io.LoadedIonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.LibraryOrigin;
import io.github.mzmine.datamodel.identities.iontype.UnmodifiableIonLibrary;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/// One pipeline for getting libraries into the global service regardless of where they came
/// from. File import today, cloud catalog fetch tomorrow — both end up calling
/// {@link #importLibraries(List, MergePolicy)} with a list of {@link IonLibrary} instances.
///
/// The importer merges the incoming libraries into the current service state per
/// {@link MergePolicy}, unions the parts/types/definitions, and submits the result via
/// {@link GlobalIonLibraryService#applyUpdates(int, GlobalIonLibraryDTO)}. The final apply is
/// version-guarded, so if another writer slips in between the current-snapshot and the apply
/// the caller sees an {@link ApplyResult.Conflict} in the returned {@link IonLibraryImportResult}.
public final class GlobalIonLibraryImporter {

  private final @NotNull GlobalIonLibraryService service;

  public GlobalIonLibraryImporter(@NotNull GlobalIonLibraryService service) {
    this.service = service;
  }

  /// Read a JSON file and import the library it contains.
  public @NotNull IonLibraryImportResult importFromFile(@NotNull File file, @NotNull MergePolicy policy) {
    final LoadedIonLibrary loaded = IonLibraryIO.loadFromJsonFile(file);
    return importLibraries(List.of(loaded.library()), policy);
  }

  /// Fetch a library from a cloud catalog and import it. Catalog calls happen off-lock; the final
  /// apply is version-guarded.
  public @NotNull IonLibraryImportResult importFromCloud(@NotNull CloudCatalog catalog,
      @NotNull RemoteLibraryRef ref, @NotNull MergePolicy policy) {
    final IonLibrary remote = catalog.fetch(ref);
    return importLibraries(List.of(remote), policy);
  }

  /// Core entry point: merge `incoming` into the current service state per `policy` and submit.
  public @NotNull IonLibraryImportResult importLibraries(@NotNull List<IonLibrary> incoming,
      @NotNull MergePolicy policy) {
    final GlobalIonLibraryDTO current = service.getCurrentGlobalLibrary();

    // library index, keyed by id and (lowercased) name
    final Map<UUID, IonLibrary> byId = HashMap.newHashMap(current.libraries().size());
    final Map<String, IonLibrary> byLowerName = HashMap.newHashMap(current.libraries().size());
    for (IonLibrary lib : current.libraries()) {
      byId.put(lib.id(), lib);
      byLowerName.put(lib.name().toLowerCase(Locale.ROOT), lib);
    }

    final List<UUID> added = new ArrayList<>();
    final List<UUID> updated = new ArrayList<>();
    final List<UUID> skipped = new ArrayList<>();
    final List<RenamedImport> renamed = new ArrayList<>();

    // result map built from current libraries, mutated per policy
    final Map<UUID, IonLibrary> merged = new HashMap<>(byId);

    for (IonLibrary lib : incoming) {
      if (IonLibraries.isInternalLibrary(lib)) {
        // never overwrite builtins via import — always rename or skip
        skipped.add(lib.id());
        continue;
      }

      final IonLibrary byIdMatch = byId.get(lib.id());
      if (byIdMatch != null) {
        switch (policy) {
          case SKIP_EXISTING -> skipped.add(lib.id());
          case OVERWRITE_BY_ID -> {
            merged.put(lib.id(), lib);
            updated.add(lib.id());
          }
          case RENAME_ON_COLLISION -> addRenamed(merged, byLowerName, lib, renamed, added);
        }
        continue;
      }

      final IonLibrary byNameMatch = byLowerName.get(lib.name().toLowerCase(Locale.ROOT));
      if (byNameMatch != null) {
        switch (policy) {
          case SKIP_EXISTING -> skipped.add(lib.id());
          case OVERWRITE_BY_ID -> // id didn't match — we won't overwrite a differently-identified
              // library silently; fall back to rename so both coexist
              addRenamed(merged, byLowerName, lib, renamed, added);
          case RENAME_ON_COLLISION -> addRenamed(merged, byLowerName, lib, renamed, added);
        }
        continue;
      }

      // no collision
      merged.put(lib.id(), lib);
      byLowerName.put(lib.name().toLowerCase(Locale.ROOT), lib);
      added.add(lib.id());
    }

    final List<IonLibrary> mergedLibraries = List.copyOf(merged.values());
    final List<IonPart> mergedParts = unionParts(current.parts(), incoming);
    final List<IonType> mergedTypes = unionTypes(current.types(), incoming);
    final GlobalIonLibraryDTO proposed = new GlobalIonLibraryDTO(current.version(),
        mergedLibraries, mergedTypes, mergedParts, List.copyOf(current.partDefinitions()));

    final ApplyResult applyResult = service.applyUpdates(current.version(), proposed, false);
    return new IonLibraryImportResult(List.copyOf(added), List.copyOf(updated), List.copyOf(skipped),
        List.copyOf(renamed), applyResult);
  }

  private static void addRenamed(@NotNull Map<UUID, IonLibrary> merged,
      @NotNull Map<String, IonLibrary> byLowerName, @NotNull IonLibrary incoming,
      @NotNull List<RenamedImport> renamed, @NotNull List<UUID> added) {
    final String newName = uniqueImportedName(incoming.name(), byLowerName);
    final UUID newId = UUID.randomUUID();
    final IonLibrary relabeled = new UnmodifiableIonLibrary(newId, preserveOrigin(incoming.origin()),
        newName, incoming.ions());
    merged.put(newId, relabeled);
    byLowerName.put(newName.toLowerCase(Locale.ROOT), relabeled);
    renamed.add(new RenamedImport(newId, incoming.name(), newName));
    added.add(newId);
  }

  /// Preserve origin metadata (a Cloud import that gets renamed is still a cloud-sourced library
  /// — the user just chose a different local name). Builtins should never reach this method, but
  /// demote defensively.
  private static @NotNull LibraryOrigin preserveOrigin(@NotNull LibraryOrigin origin) {
    return origin instanceof LibraryOrigin.Builtin ? LibraryOrigin.LOCAL : origin;
  }

  private static @NotNull String uniqueImportedName(@NotNull String base,
      @NotNull Map<String, IonLibrary> byLowerName) {
    String candidate = base + " (imported)";
    int suffix = 2;
    while (byLowerName.containsKey(candidate.toLowerCase(Locale.ROOT))) {
      candidate = "%s (imported %d)".formatted(base, suffix++);
    }
    return candidate;
  }

  private static @NotNull List<IonPart> unionParts(@NotNull List<IonPart> existing,
      @NotNull List<IonLibrary> incoming) {
    final Set<IonPart> union = new LinkedHashSet<>(existing);
    for (IonLibrary lib : incoming) {
      for (IonType type : lib.ions()) {
        union.addAll(type.parts());
      }
    }
    return List.copyOf(union);
  }

  private static @NotNull List<IonType> unionTypes(@NotNull List<IonType> existing,
      @NotNull List<IonLibrary> incoming) {
    final Set<IonType> union = new LinkedHashSet<>(existing);
    for (IonLibrary lib : incoming) {
      union.addAll(lib.ions());
    }
    return List.copyOf(union);
  }
}
