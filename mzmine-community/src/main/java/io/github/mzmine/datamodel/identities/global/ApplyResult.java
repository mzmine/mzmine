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

import org.jetbrains.annotations.NotNull;

/// The three possible outcomes of {@link GlobalIonLibraryService#applyUpdates(int, GlobalIonLibraryDTO)}: the update went
/// through, the caller's base version was stale, or the proposed state failed validation.
///
/// Using a sealed return type instead of exceptions keeps the happy path and the conflict path
/// at equal prominence — the UI has to handle both, and neither is exceptional.
public sealed interface ApplyResult {

  /// The proposed state is now the current state.
  record Applied(int newVersion) implements ApplyResult {

  }

  /// The service moved on since the caller read its version. `currentVersion` is the version the
  /// caller should rebase onto; `report` describes which libraries changed on each side.
  record Conflict(int currentVersion, @NotNull ConflictReport report) implements ApplyResult {

  }

  /// Validation failed. Errors block the apply; warnings are informational.
  record Invalid(@NotNull ValidationResult validation) implements ApplyResult {

  }
}
