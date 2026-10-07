/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_lipidannotationcleanup;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * Defines how the remaining annotations of a row are treated when one of its lipid annotations is
 * removed during the multi-row cleanup operation.
 */
public enum MultiRowAnnotationCleanupRowHandlingMode implements UniqueIdSupplier {
  DISCARD_LOWER_THAN_REMOVED(
      "Discard all annotations with lower score than removed annotation"), DISCARD_ALL_IF_ANY_REMOVED(
      "Discard all annotations if any annotation of the row is removed"), SELECT_REMAINING_HIGHEST_SCORE(
      "Select remaining annotation with highest score, regardless of what was removed");

  private final @NotNull String label;

  MultiRowAnnotationCleanupRowHandlingMode(final @NotNull String label) {
    this.label = label;
  }

  @Override
  public @NotNull String toString() {
    return label;
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case DISCARD_LOWER_THAN_REMOVED -> "discard_lower_than_removed";
      case DISCARD_ALL_IF_ANY_REMOVED -> "discard_all_if_any_removed";
      case SELECT_REMAINING_HIGHEST_SCORE -> "select_remaining_highest_score";
    };
  }
}
