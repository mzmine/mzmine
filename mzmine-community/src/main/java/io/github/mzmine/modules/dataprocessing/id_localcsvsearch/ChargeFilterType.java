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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ChargeFilterType implements UniqueIdSupplier {
  NO_FILTER, STRICT_BOTH_CHARGED, MATCH_UNCHARGED_ANNOTATIONS;


  @Override
  public @NotNull String toString() {
    return switch (this) {
      case NO_FILTER -> "No filter";
      case STRICT_BOTH_CHARGED -> "Strict, row and annotation require charge";
      case MATCH_UNCHARGED_ANNOTATIONS -> "Match uncharged annotations";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case NO_FILTER -> "no_filter";
      case STRICT_BOTH_CHARGED -> "strict_both_charged";
      case MATCH_UNCHARGED_ANNOTATIONS -> "match_uncharged_annotations";
    };
  }

  public @NotNull String getDescription() {
    return switch (this) {
      case NO_FILTER -> "Do not filter for the row's charge to match the charge of the annotation.";
      case STRICT_BOTH_CHARGED ->
          "Strict, row and annotation require same charge. Missing charge in row or annotation will not be matched.";
      case MATCH_UNCHARGED_ANNOTATIONS ->
          "If an annotation does not provide a charge, also match to rows with any or no charge state.";
    };
  }

  public boolean matches(@NotNull FeatureListRow row, @NotNull FeatureAnnotation annotation) {
    final @Nullable PolarityType rowPolarity = row.getRepresentativePolarity();
    final IonType annotationAdduct = annotation.getAdductType();

    return switch (this) {
      case NO_FILTER -> true;
      case MATCH_UNCHARGED_ANNOTATIONS -> {
        if (annotationAdduct == null) {
          yield true;
        }
        final int absCharge = annotationAdduct.getAbsCharge();
        final int rowCharge = Math.abs(row.getRowCharge());
        yield annotationAdduct.getPolarity() == rowPolarity && absCharge == rowCharge;
      }
      case STRICT_BOTH_CHARGED -> {
        if (annotationAdduct == null) {
          yield false;
        }
        final int absCharge = annotationAdduct.getAbsCharge();
        final int rowCharge = Math.abs(row.getRowCharge());
        yield annotationAdduct.getPolarity() == rowPolarity && absCharge == rowCharge;
      }
    };
  }
}
