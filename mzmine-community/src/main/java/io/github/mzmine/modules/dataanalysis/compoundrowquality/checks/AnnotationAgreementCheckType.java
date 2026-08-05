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

package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * Controls which {@code FeatureAnnotation}s of every member row are compared by
 * {@link AnnotationAgreementCheck}. PREFERRED_ANNOTATION is the conservative default — it asks
 * whether the rows' currently-chosen "best" annotation agree. The other two options surface
 * disagreements that the preferred-annotation view hides.
 */
public enum AnnotationAgreementCheckType implements UniqueIdSupplier {

  /// Only the {@code row.getPreferredAnnotation()} of every member row.
  PREFERRED_ANNOTATION,
  /// One annotation per annotation method (DataType), per member row, taken as the type's top
  /// match. Analog spectral matches are filtered out.
  BEST_PER_ANNOTATION_METHOD,
  /// Every annotation across all annotation methods on every member row.
  ALL_ANNOTATIONS;

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case PREFERRED_ANNOTATION -> "preferred_annotation";
      case BEST_PER_ANNOTATION_METHOD -> "best_per_annotation_method";
      case ALL_ANNOTATIONS -> "all_annotations";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case PREFERRED_ANNOTATION -> "Preferred annotation";
      case BEST_PER_ANNOTATION_METHOD -> "Best per annotation method";
      case ALL_ANNOTATIONS -> "All annotations";
    };
  }
}
