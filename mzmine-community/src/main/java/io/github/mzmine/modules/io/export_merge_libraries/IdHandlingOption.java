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

package io.github.mzmine.modules.io.export_merge_libraries;

import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.Set;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum IdHandlingOption implements UniqueIdSupplier {
  KEEP_ALL, NEW_ID_WITH_FILENAME, NEW_ID_WITH_DATASET_ID, NEW_ID_WITH_OLD_LIBRARY_NAME, AVOID_DUPLICATES;

  private static final String remappingPattern = "%s_%s";

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case KEEP_ALL -> "keep_all";
      case AVOID_DUPLICATES -> "avoid_duplicates";
      case NEW_ID_WITH_OLD_LIBRARY_NAME -> "new_id_with_library_name";
      case NEW_ID_WITH_DATASET_ID -> "new_id_with_dataset_id";
      case NEW_ID_WITH_FILENAME -> "new_id_with_filename";
    };
  }

  public @NotNull String getDescription() {
    return this + ": " + switch (this) {
      case KEEP_ALL ->
          "Keeps all existing IDs, may lead to duplicates. All other options avoid duplicates.";
      case NEW_ID_WITH_FILENAME -> "Create new IDs; Pattern: <new library filename>_<nubmer>_id. ";
      case NEW_ID_WITH_DATASET_ID -> "Create new IDs; Pattern: <entry.DATASET_ID>_<nubmer>_id";
      case AVOID_DUPLICATES ->
          "Keeps existing IDs at their first appearance, then avoids duplicate IDs by replacing them with the pattern defined in: "
              + NEW_ID_WITH_OLD_LIBRARY_NAME;
      case NEW_ID_WITH_OLD_LIBRARY_NAME ->
          "Create new IDs; Pattern: <old library file name>_<number>_id";
    };
  }

  /**
   * @param libraryName the new library name (usually filename without extension)
   * @param usedIds     all IDs already used in the library
   * @param fallbackId  a fallback id in case {@link DBEntryField#ENTRY_ID} is empty
   * @return the new id
   */
  public @NotNull String getNewEntryId(@NotNull String libraryName,
      @NotNull SpectralLibraryEntry entry, Set<String> usedIds,
      @NotNull final Supplier<@NotNull String> fallbackId) {
    // library is nullable so fallback to a default name
    final String oldLibName = requireNonNullElse(entry.getLibraryName(), "speclib");
    final @Nullable String originalID = entry.getAsString(DBEntryField.ENTRY_ID).orElse(null);

    return switch (this) {
      case KEEP_ALL -> requireNonNullElseGet(originalID, fallbackId);
      case AVOID_DUPLICATES -> {
        // first try with the originalID or fallback
        String entryID = requireNonNullElseGet(originalID, fallbackId);
        String fullId = remappingPattern.formatted(oldLibName, entryID);
        while (usedIds.contains(fullId)) {
          // keep incrementing the fallbackId
          entryID = fallbackId.get();
          fullId = remappingPattern.formatted(oldLibName, entryID);
        }
        yield fullId;
      }
      case NEW_ID_WITH_OLD_LIBRARY_NAME -> remappingPattern.formatted(oldLibName, fallbackId.get());
      case NEW_ID_WITH_DATASET_ID -> {
        // useful to add the DATASET_ID of the original entry
        final String libName = entry.getAsString(DBEntryField.DATASET_ID).orElse("speclib");
        yield remappingPattern.formatted(libName, fallbackId.get());
      }
      // most useful for libraries that want the same ID pattern with a common filename prefix and then the scan number from fallbackID
      case NEW_ID_WITH_FILENAME -> remappingPattern.formatted(libraryName, fallbackId.get());
    };
  }


  @Override
  public String toString() {
    return switch (this) {
      case KEEP_ALL -> "Keep existing IDs";
      case NEW_ID_WITH_FILENAME -> "New IDs with filename";
      case NEW_ID_WITH_DATASET_ID -> "New IDs with dataset IDs";
      case AVOID_DUPLICATES -> "Avoid duplicates";
      case NEW_ID_WITH_OLD_LIBRARY_NAME -> "New IDs with old library name";
    };
  }
}
