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

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public enum IdHandlingOption implements UniqueIdSupplier {
  KEEP_ALL, AVOID_DUPLICATES, NEW_ID_WITH_LIBRARY_NAME;

  private static final String remappingPattern = "%s_%s";

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case KEEP_ALL -> "keep_all";
      case AVOID_DUPLICATES -> "avoid_duplicates";
      case NEW_ID_WITH_LIBRARY_NAME -> "new_id_with_library_name";
    };
  }

  /**
   *
   * @param isDuplicate if the entry is a duplicate (id appears in another library)
   * @param fallbackId a fallback id in case {@link DBEntryField#ENTRY_ID} is empty
   * @return the new id
   */
  public @NotNull String getNewEntryId(@NotNull SpectralLibraryEntry entry, boolean isDuplicate,
      @NotNull final  Supplier<@NotNull String> fallbackId) {
    return switch (this) {
      case KEEP_ALL -> entry.getAsString(DBEntryField.ENTRY_ID).orElseGet(fallbackId);
      case AVOID_DUPLICATES -> {
        if (isDuplicate) {
          yield remappingPattern.formatted(entry.getLibrary().getName(),
              entry.getAsString(DBEntryField.ENTRY_ID).orElseGet(fallbackId));
        } else {
          yield entry.getAsString(DBEntryField.ENTRY_ID).orElseGet(fallbackId);
        }
      }
      case NEW_ID_WITH_LIBRARY_NAME -> remappingPattern.formatted(entry.getLibrary().getPath().getName(),
          entry.getAsString(DBEntryField.ENTRY_ID).orElseGet(fallbackId));
    };
  }


  @Override
  public String toString() {
    return switch (this) {
      case KEEP_ALL -> "Keep existing IDs";
      case AVOID_DUPLICATES -> "Avoid duplicates";
      case NEW_ID_WITH_LIBRARY_NAME -> "New IDs with old library name";
    };
  }
}
