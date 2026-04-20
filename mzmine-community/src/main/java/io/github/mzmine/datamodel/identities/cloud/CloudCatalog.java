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

package io.github.mzmine.datamodel.identities.cloud;

import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Read-only view of a remote catalog of ion libraries. Pull-only for v1: users browse the list,
/// fetch individual libraries, and import them into their local store.
///
/// v2 will add `push(IonLibrary)` and `status(UUID)` for bidirectional sync and sharing. That
/// extension is additive — existing v1 callers will keep working.
///
/// Implementations must be safe to call off the FX thread; the import pipeline runs on a
/// background task and applies the result via {@link
/// io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService#applyUpdates(int,
/// io.github.mzmine.datamodel.identities.global.GlobalIonLibraryDTO) GlobalIonLibraryService}.
public interface CloudCatalog {

  /// List the libraries the remote catalog currently offers. May be filtered server-side.
  @NotNull List<RemoteLibraryRef> list();

  /// Fetch one library's full content from the catalog. The returned library carries {@link
  /// io.github.mzmine.datamodel.identities.iontype.LibraryOrigin.Cloud} origin and a stable id
  /// derived from {@link RemoteLibraryRef#remoteId()}.
  @NotNull IonLibrary fetch(@NotNull RemoteLibraryRef ref);

  /// Catalog entry metadata — enough to present a browse UI without downloading the whole library.
  ///
  /// @param remoteId      catalog-side stable identifier
  /// @param name          human-readable name as curated by the catalog
  /// @param description   optional longer description
  /// @param ionCount      how many ions the library contains
  /// @param publishedAt   when the catalog last updated this entry
  record RemoteLibraryRef(@NotNull String remoteId, @NotNull String name,
                          @Nullable String description, int ionCount, @NotNull Instant publishedAt) {

  }
}
