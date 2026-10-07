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

import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/// Two-way diff between a caller's proposed state and the service's current state, by library id.
///
/// Without a stored common ancestor we can't tell whether an id that is "only in proposed" was
/// added by the caller or removed externally — the two cases look identical. The names below
/// describe what is observable, not intent.
///
/// {@link #sameIdDifferentContent} is the clean "conflict" signal: the library exists on both
/// sides but the content differs, so the user's edits and the external edits collide and
/// someone has to choose.
public record ConflictReport(@NotNull Set<UUID> sameIdDifferentContent,
                             @NotNull Set<UUID> onlyInProposed,
                             @NotNull Set<UUID> onlyInCurrent) {

  public boolean isEmpty() {
    return sameIdDifferentContent.isEmpty() && onlyInProposed.isEmpty()
        && onlyInCurrent.isEmpty();
  }
}
