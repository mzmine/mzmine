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

import io.github.mzmine.datamodel.identities.global.ValidationResult.ValidationError;
import io.github.mzmine.datamodel.identities.global.ValidationResult.ValidationWarning;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.LibraryOrigin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/// Pure validation over a proposed {@link GlobalIonLibraryDTO}. No side effects, no locking —
/// safe to call without touching the service.
///
/// Validation is split into errors (block the apply) and warnings (inform the user). Keep it that
/// way: if a rule can't be expressed as "reject vs. allow with caveat", add a new level rather
/// than blurring the line.
public final class GlobalIonLibraryValidator {

  /// Anything Jackson / the filesystem / the preset store dislikes when used as a file name.
  private static final Pattern UNSAFE_NAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");

  /// Relative tolerance for flagging near-duplicate parts: two parts with the same name differing
  /// in mass by less than this ratio are considered "close enough to be a typo."
  private static final double NEAR_DUPLICATE_MASS_TOLERANCE_PPM = 10.0;

  /// Validate a proposed state against the current service state.
  ///
  /// @param proposed the state the caller wants to apply
  /// @param current  the service state at the version the caller read (used for builtin-immutable
  ///                 checks; pass {@code null} if no baseline is available)
  public @NotNull ValidationResult validate(@NotNull GlobalIonLibraryDTO proposed,
      @NotNull GlobalIonLibraryDTO current) {
    final List<ValidationError> errors = new ArrayList<>();
    final List<ValidationWarning> warnings = new ArrayList<>();

    checkLibraryIdUniqueness(proposed.libraries(), errors);
    checkLibraryNameUniqueness(proposed.libraries(), errors);
    checkLibraryNamesFilesystemSafe(proposed.libraries(), errors);
    checkBuiltinLibrariesImmutable(proposed.libraries(), current.libraries(), errors);
    checkReferencedPartsExist(proposed, errors);
    checkReferencedTypesExist(proposed, errors);
    warnNearDuplicateParts(proposed.parts(), warnings);

    if (errors.isEmpty() && warnings.isEmpty()) {
      return ValidationResult.ok();
    }
    return new ValidationResult(List.copyOf(errors), List.copyOf(warnings));
  }

  private static void checkLibraryIdUniqueness(@NotNull List<IonLibrary> libraries,
      @NotNull List<ValidationError> errors) {
    final Set<UUID> seen = new HashSet<>(libraries.size());
    for (IonLibrary lib : libraries) {
      if (!seen.add(lib.id())) {
        errors.add(new ValidationError("library.id.duplicate",
            "Two libraries share the id %s. Library ids must be unique.".formatted(lib.id())));
      }
    }
  }

  private static void checkLibraryNameUniqueness(@NotNull List<IonLibrary> libraries,
      @NotNull List<ValidationError> errors) {
    final Map<String, UUID> byLowerName = HashMap.newHashMap(libraries.size());
    for (IonLibrary lib : libraries) {
      final String key = lib.name().toLowerCase(Locale.ROOT);
      final UUID prior = byLowerName.putIfAbsent(key, lib.id());
      if (prior != null && !prior.equals(lib.id())) {
        errors.add(new ValidationError("library.name.duplicate",
            "Two libraries share the name '%s'. Library names must be unique (case-insensitive)."
                .formatted(lib.name())));
      }
    }
  }

  private static void checkLibraryNamesFilesystemSafe(@NotNull List<IonLibrary> libraries,
      @NotNull List<ValidationError> errors) {
    for (IonLibrary lib : libraries) {
      if (lib.origin() instanceof LibraryOrigin.Builtin) {
        continue; // builtins aren't persisted as files
      }
      if (lib.name().isBlank()) {
        errors.add(new ValidationError("library.name.blank",
            "Library %s has a blank name. Give it a name before saving.".formatted(lib.id())));
        continue;
      }
      if (UNSAFE_NAME_CHARS.matcher(lib.name()).find()) {
        errors.add(new ValidationError("library.name.unsafe",
            "Library name '%s' contains characters that can't be used in file names."
                .formatted(lib.name())));
      }
    }
  }

  private static void checkBuiltinLibrariesImmutable(@NotNull List<IonLibrary> proposed,
      @NotNull List<IonLibrary> current, @NotNull List<ValidationError> errors) {
    final Map<UUID, IonLibrary> currentBuiltins = HashMap.newHashMap(current.size());
    for (IonLibrary lib : current) {
      if (lib.origin() instanceof LibraryOrigin.Builtin) {
        currentBuiltins.put(lib.id(), lib);
      }
    }
    if (currentBuiltins.isEmpty()) {
      return;
    }

    final Map<UUID, IonLibrary> proposedById = HashMap.newHashMap(proposed.size());
    for (IonLibrary lib : proposed) {
      proposedById.put(lib.id(), lib);
    }

    for (Map.Entry<UUID, IonLibrary> entry : currentBuiltins.entrySet()) {
      final IonLibrary was = entry.getValue();
      final IonLibrary now = proposedById.get(entry.getKey());
      if (now == null) {
        errors.add(new ValidationError("library.builtin.removed",
            "Builtin library '%s' cannot be removed.".formatted(was.name())));
        continue;
      }
      if (!(now.origin() instanceof LibraryOrigin.Builtin)) {
        errors.add(new ValidationError("library.builtin.origin-changed",
            "Builtin library '%s' cannot have its origin changed.".formatted(was.name())));
      }
      if (!was.name().equals(now.name())) {
        errors.add(new ValidationError("library.builtin.renamed",
            "Builtin library '%s' cannot be renamed to '%s'.".formatted(was.name(), now.name())));
      }
      if (!was.equalIons(now)) {
        errors.add(new ValidationError("library.builtin.content-changed",
            "Builtin library '%s' cannot have its ion set modified.".formatted(was.name())));
      }
    }
  }

  private static void checkReferencedPartsExist(@NotNull GlobalIonLibraryDTO proposed,
      @NotNull List<ValidationError> errors) {
    final Set<IonPart> known = new HashSet<>(proposed.parts());
    for (IonLibrary lib : proposed.libraries()) {
      for (IonType type : lib.ions()) {
        for (IonPart part : type.parts()) {
          if (!known.contains(part)) {
            errors.add(new ValidationError("library.reference.missing-part",
                "Library '%s' references an ion part that is not in the global parts list: %s"
                    .formatted(lib.name(), part.name())));
          }
        }
      }
    }
  }

  private static void checkReferencedTypesExist(@NotNull GlobalIonLibraryDTO proposed,
      @NotNull List<ValidationError> errors) {
    final Set<IonType> known = new HashSet<>(proposed.types());
    for (IonLibrary lib : proposed.libraries()) {
      if (lib.origin() instanceof LibraryOrigin.Builtin) {
        // builtins ship their own ion types; service's global types list is user-curated
        continue;
      }
      for (IonType type : lib.ions()) {
        if (!known.contains(type)) {
          errors.add(new ValidationError("library.reference.missing-type",
              "Library '%s' references an ion type that is not in the global types list."
                  .formatted(lib.name())));
          break; // one error per library is enough; the fix is the same
        }
      }
    }
  }

  private static void warnNearDuplicateParts(@NotNull List<IonPart> parts,
      @NotNull List<ValidationWarning> warnings) {
    final Map<String, List<IonPart>> byName = new HashMap<>();
    for (IonPart part : parts) {
      byName.computeIfAbsent(part.name(), _ -> new ArrayList<>(1)).add(part);
    }
    for (List<IonPart> group : byName.values()) {
      if (group.size() < 2) {
        continue;
      }
      for (int i = 0; i < group.size(); i++) {
        for (int j = i + 1; j < group.size(); j++) {
          final IonPart a = group.get(i);
          final IonPart b = group.get(j);
          if (a.singleCharge() != b.singleCharge()) {
            continue; // different charge states are a legitimate "Fe+2 vs Fe+3" case
          }
          final double base = Math.max(Math.abs(a.absSingleMass()), Math.abs(b.absSingleMass()));
          if (base == 0d) {
            continue;
          }
          final double ppmDiff = Math.abs(a.absSingleMass() - b.absSingleMass()) / base * 1e6;
          if (ppmDiff < NEAR_DUPLICATE_MASS_TOLERANCE_PPM) {
            warnings.add(new ValidationWarning("part.near-duplicate",
                "Two parts named '%s' have nearly identical mass but different formulas ('%s' vs '%s'). Is one a typo?"
                    .formatted(a.name(), a.singleFormula(), b.singleFormula())));
          }
        }
      }
    }
  }
}
