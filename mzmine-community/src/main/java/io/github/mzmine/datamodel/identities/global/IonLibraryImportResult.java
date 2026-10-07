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

import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/// Outcome of importing a batch of libraries from a file or a cloud catalog.
public record IonLibraryImportResult(@NotNull List<IonLibrary> added,
                                     @NotNull List<IonLibrary> skipped) {

  public boolean isChanged() {
    return !added.isEmpty();
  }

  /// Policy for how to resolve name/id collisions during import.
  public enum MergePolicy implements UniqueIdSupplier {
    SKIP_OLDER, ASK_OLDER_OVERWRITE, OVERWRITE_ALL;

    @Override
    public @NotNull String getUniqueID() {
      return switch (this) {
        case SKIP_OLDER -> "skip_older";
        case ASK_OLDER_OVERWRITE -> "ask_older_overwrite";
        case OVERWRITE_ALL -> "overwrite_all";
      };
    }

    @Override
    public String toString() {
      return switch (this) {
        case SKIP_OLDER -> "Skip on older versions";
        case ASK_OLDER_OVERWRITE -> "Ask for older versions";
        case OVERWRITE_ALL -> "Overwrite all";
      };
    }
  }
}
