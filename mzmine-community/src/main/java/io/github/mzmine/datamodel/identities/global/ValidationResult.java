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
import org.jetbrains.annotations.NotNull;

/// Outcome of validating a proposed {@link GlobalIonLibraryDTO} against the current service state.
/// Errors block the apply; warnings surface in the UI but do not.
public record ValidationResult(@NotNull List<ValidationError> errors,
                               @NotNull List<ValidationWarning> warnings) {

  private static final ValidationResult OK = new ValidationResult(List.of(), List.of());

  public static @NotNull ValidationResult ok() {
    return OK;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /// Blocking validation failure. `code` is a stable identifier the UI can key off for i18n or
  /// iconography; `message` is the human-readable description.
  public record ValidationError(@NotNull String code, @NotNull String message) {

  }

  /// Non-blocking concern the user should be aware of (e.g. near-duplicate parts).
  public record ValidationWarning(@NotNull String code, @NotNull String message) {

  }
}
