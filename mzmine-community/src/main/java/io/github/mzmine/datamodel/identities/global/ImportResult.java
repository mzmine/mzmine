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

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/// Outcome of importing a batch of libraries from a file or a cloud catalog.
/// <p>
/// {@link #added} and {@link #updated} are library ids that made it in; {@link #skipped} lists
/// the ones that collided with an existing library and were left alone per policy; {@link
/// #renamed} records collisions that were resolved by rename (original id → new library name).
/// {@link #applyResult} is the outcome of the final service-level apply — typically {@link
/// ApplyResult.Applied}, but may be {@link ApplyResult.Invalid} or {@link ApplyResult.Conflict}
/// if the import raced with another writer.
public record ImportResult(@NotNull List<UUID> added, @NotNull List<UUID> updated,
                           @NotNull List<UUID> skipped, @NotNull List<RenamedImport> renamed,
                           @NotNull ApplyResult applyResult) {

  public record RenamedImport(@NotNull UUID id, @NotNull String originalName,
                              @NotNull String newName) {

  }

  /// Policy for how to resolve name/id collisions during import.
  public enum MergePolicy {
    /// Keep the existing library untouched; log the incoming one as skipped.
    SKIP_EXISTING,
    /// Replace the local library with the incoming one when ids match.
    OVERWRITE_BY_ID,
    /// Append a suffix to the incoming library's name so it coexists with the local one.
    RENAME_ON_COLLISION
  }
}
