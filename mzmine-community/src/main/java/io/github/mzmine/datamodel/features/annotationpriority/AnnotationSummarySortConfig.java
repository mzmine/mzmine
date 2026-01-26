package io.github.mzmine.datamodel.features.annotationpriority;

import io.github.mzmine.modules.dataprocessing.filter_sortannotations.PreferredAnnotationSortingParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @param ccsTolerance ccs tolerance in %
 * @param riTolerance  ri tolerance in %
 */
public record AnnotationSummarySortConfig(@NotNull MZTolerance mzTolerance,
                                          @NotNull RTTolerance rtTolerance, double ccsTolerance,
                                          double riTolerance,
                                          @NotNull AnnotationSummaryOrder sortOrder) {

  public static final AnnotationSummarySortConfig DEFAULT = new AnnotationSummarySortConfig(
      MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA, PreferredAnnotationSortingParameters.DEFAULT_RT_TOLERANCE,
      PreferredAnnotationSortingParameters.DEFAULT_CCS_TOLERANCE,
      PreferredAnnotationSortingParameters.DEFAULT_RI_TOLERANCE,
      PreferredAnnotationSortingParameters.DEFAULT_SORT_ORDER);

  public PreferredAnnotationSortingParameters toParameters() {
    return PreferredAnnotationSortingParameters.fromConfig(this);
  }
}
