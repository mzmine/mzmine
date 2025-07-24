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

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public enum IdHandlingOption implements UniqueIdSupplier {
  KEEP_ALL, RENUMBERED_WITH_FILENAME, RENUMBER_WITH_DATASET_ID, AVOID_DUPLICATES, NEW_ID_WITH_LIBRARY_NAME;

  private static final String remappingPattern = "%s_%s";

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case KEEP_ALL -> "keep_all";
      case AVOID_DUPLICATES -> "avoid_duplicates";
      case NEW_ID_WITH_LIBRARY_NAME -> "new_id_with_library_name";
      case RENUMBER_WITH_DATASET_ID -> "renumber_with_dataset_id";
      case RENUMBERED_WITH_FILENAME -> "renumbered_with_filename";
    };
  }

  /**
   * @param libraryName the new library name (usually filename without extension)
   * @param isDuplicate if the entry is a duplicate (id appears in another library)
   * @param fallbackId  a fallback id in case {@link DBEntryField#ENTRY_ID} is empty
   * @return the new id
   */
  public @NotNull String getNewEntryId(@NotNull String libraryName,
      @NotNull SpectralLibraryEntry entry, boolean isDuplicate,
      @NotNull final Supplier<@NotNull String> fallbackId) {
    // library is nullable so fallback to a default name
    final String oldLibName = requireNonNullElse(entry.getLibraryName(), "speclib");
    return switch (this) {
      case KEEP_ALL -> entry.getAsString(DBEntryField.ENTRY_ID).orElseGet(fallbackId);
      case AVOID_DUPLICATES -> {
        if (isDuplicate) {
          yield remappingPattern.formatted(oldLibName, fallbackId);
        } else {
          yield entry.getAsString(DBEntryField.ENTRY_ID).orElseGet(fallbackId);
        }
      }
      case NEW_ID_WITH_LIBRARY_NAME -> remappingPattern.formatted(oldLibName, fallbackId);
      case RENUMBER_WITH_DATASET_ID -> {
        // useful to add the DATASET_ID of the original entry
        final String libName = entry.getAsString(DBEntryField.DATASET_ID).orElse("speclib");
        yield remappingPattern.formatted(libName, fallbackId);
      }
      // most useful for libraries that want the same ID pattern with a common filename prefix and then the scan number from fallbackID
      case RENUMBERED_WITH_FILENAME -> remappingPattern.formatted(libraryName, fallbackId);
    };
  }


  @Override
  public String toString() {
    return switch (this) {
      case KEEP_ALL -> "Keep existing IDs";
      case RENUMBERED_WITH_FILENAME -> "Renumbered with filename";
      case RENUMBER_WITH_DATASET_ID -> "Renumbered with dataset IDs";
      case AVOID_DUPLICATES -> "Avoid duplicates";
      case NEW_ID_WITH_LIBRARY_NAME -> "New IDs with old library name";
    };
  }
}
