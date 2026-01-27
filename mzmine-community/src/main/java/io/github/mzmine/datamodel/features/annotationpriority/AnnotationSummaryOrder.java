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

package io.github.mzmine.datamodel.features.annotationpriority;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Comparator.reverseOrder;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum AnnotationSummaryOrder implements UniqueIdSupplier {

  MZMINE, SCHYMANSKI, MSI;

  private static final Comparator<@NotNull AnnotationSummary> SCORES_LOW_TO_HIGH = Comparator.comparing(
          AnnotationSummary::annotationTypeScore, nullsFirst(naturalOrder()))
      .thenComparing(AnnotationSummary::ms2Score, nullsFirst(naturalOrder()))
      .thenComparing(AnnotationSummary::isotopeScore, nullsFirst(naturalOrder()))
      .thenComparing(AnnotationSummary::mzScore, nullsFirst(naturalOrder()))
      .thenComparing(AnnotationSummary::rtScore, nullsFirst(naturalOrder()))
      .thenComparing(AnnotationSummary::riScore, nullsFirst(naturalOrder()))
      .thenComparing(AnnotationSummary::ccsScore, nullsFirst(naturalOrder()));

  private static final Comparator<@NotNull AnnotationSummary> SCHYMANSKI_LOW_TO_HIGH_NON_NULL = Comparator.comparing(
          AnnotationSummary::deriveSchymanskiLevel, nullsFirst(reverseOrder()))
      .thenComparing(SCORES_LOW_TO_HIGH);
  private static final Comparator<@Nullable AnnotationSummary> SCHYMANSKI_LOW_TO_HIGH = Comparator.comparing(
      Function.identity(), nullsFirst(SCHYMANSKI_LOW_TO_HIGH_NON_NULL));

  private static final Comparator<@Nullable AnnotationSummary> DEFAULT_LOW_TO_HIGH = Comparator.comparing(
          Function.identity(),
          // according to Schymanski scale, lipid annotations would be 2b, so always below library matches.
          // change that here for the default.
          nullsFirst(Comparator.comparing(AnnotationSummary::annotationTypeScore, naturalOrder())))
      .thenComparing(SCHYMANSKI_LOW_TO_HIGH);

  private static final Comparator<@NotNull AnnotationSummary> MSI_LOW_TO_HIGH_NON_NULL = Comparator.comparing(
      AnnotationSummary::deriveMsiLevel, nullsFirst(reverseOrder()));
  private static final Comparator<@Nullable AnnotationSummary> MSI_LOW_TO_HIGH = Comparator.comparing(
      Function.identity(), nullsFirst(MSI_LOW_TO_HIGH_NON_NULL).thenComparing(SCORES_LOW_TO_HIGH));

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case MZMINE -> "mzmine_default";
      case SCHYMANSKI -> "schymanski";
      case MSI -> "msi";
    };
  }


  public @NotNull String getDescription() {
    return switch (this) {
      case MZMINE -> "Default annotation sorting configuration for mzmine.";
      case SCHYMANSKI ->
          "Annotation sorting configuration based on Schymanski et al annotation levels.";
      case MSI ->
          "Annotation sorting configuration according to MSI (Metabolomics Standards Initiative).";
    };
  }

  @NotNull
  public static String getDescriptions() {
    return Arrays.stream(values()).map(v -> v + ": " + v.getDescription())
        .collect(Collectors.joining("\n"));
  }

  /**
   *
   * @return A comparator that sorts annotations by confidence level in ascending order.
   */
  public Comparator<@Nullable AnnotationSummary> getComparatorLowFirst() {
    return switch (this) {
      case MSI -> MSI_LOW_TO_HIGH;
      case MZMINE -> DEFAULT_LOW_TO_HIGH;
      case SCHYMANSKI -> SCHYMANSKI_LOW_TO_HIGH;
    };
  }

  /**
   *
   * @return A comparator that sorts annotations by confidence level in descending order
   */
  public Comparator<@Nullable AnnotationSummary> getComparatorHighFirst() {
    return switch (this) {
      case MZMINE -> DEFAULT_LOW_TO_HIGH.reversed();
      case MSI -> MSI_LOW_TO_HIGH.reversed();
      case SCHYMANSKI -> SCHYMANSKI_LOW_TO_HIGH.reversed();
    };
  }

  public static AnnotationSummaryOrder getDefault() {
    return MZMINE;
  }

  @Override
  public String toString() {
    return switch (this) {
      case MZMINE -> "mzmine";
      case SCHYMANSKI -> "Schymanski et. al.";
      case MSI -> "MSI";
    };
  }
}
