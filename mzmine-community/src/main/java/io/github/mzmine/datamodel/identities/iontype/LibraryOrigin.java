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

package io.github.mzmine.datamodel.identities.iontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;

/// Describes where an {@link IonLibrary} came from. Drives UI affordances (lock icons on builtins,
/// "update from cloud" actions) and save logic (builtins are not persisted as presets).
///
/// v1 supports pull-only cloud import via {@link Cloud}. Sharing / bidirectional sync in a future
/// release would add a new permitted variant (e.g. `Shared`) without breaking existing persistence.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({ //
    @JsonSubTypes.Type(value = LibraryOrigin.Builtin.class, name = "builtin"), //
    @JsonSubTypes.Type(value = LibraryOrigin.Local.class, name = "local"), //
    @JsonSubTypes.Type(value = LibraryOrigin.Cloud.class, name = "cloud")})
public sealed interface LibraryOrigin {

  Builtin BUILTIN = new Builtin();
  Local LOCAL = new Local();

  default boolean isCloud() {
    return this instanceof Cloud;
  }

  /// Bundled with mzmine. Immutable; never persisted as a user preset.
  record Builtin() implements LibraryOrigin {

  }

  /// Created or edited by the user on this machine.
  record Local() implements LibraryOrigin {

  }

  /// Pulled from a remote catalog. `remoteId` identifies the catalog entry; `fetchedAt` is when
  /// the library was last downloaded.
  record Cloud(@NotNull String remoteId, @NotNull Instant fetchedAt) implements LibraryOrigin {

  }
}
