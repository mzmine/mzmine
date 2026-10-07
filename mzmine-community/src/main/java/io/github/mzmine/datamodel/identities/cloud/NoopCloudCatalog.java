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
import java.util.List;
import org.jetbrains.annotations.NotNull;

/// Placeholder {@link CloudCatalog} used until an HTTP-backed implementation ships. Returns an
/// empty catalog and throws on fetch so missing wiring is loud rather than silent.
public final class NoopCloudCatalog implements CloudCatalog {

  @Override
  public @NotNull List<RemoteLibraryRef> list() {
    return List.of();
  }

  @Override
  public @NotNull IonLibrary fetch(@NotNull RemoteLibraryRef ref) {
    throw new UnsupportedOperationException(
        "Cloud catalog not configured. Install a catalog implementation before fetching '%s'."
            .formatted(ref.remoteId()));
  }
}
