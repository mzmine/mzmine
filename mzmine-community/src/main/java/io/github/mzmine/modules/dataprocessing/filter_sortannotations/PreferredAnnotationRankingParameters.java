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

package io.github.mzmine.modules.dataprocessing.filter_sortannotations;

import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummaryOrder;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummarySortConfig;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class PreferredAnnotationRankingParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA.getMzTolerance(),
      MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA.getPpmTolerance());

  public static final RTTolerance DEFAULT_RT_TOLERANCE = new RTTolerance(0.3f, Unit.MINUTES);
  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter(
      DEFAULT_RT_TOLERANCE);

  public static final double DEFAULT_CCS_TOLERANCE = 0.1;
  public static final PercentParameter ccsTolerance = new PercentParameter("CCS tolerance",
      "CCS tolerance used for ranking.", DEFAULT_CCS_TOLERANCE, 0d, 1d);

  public static final double DEFAULT_RI_TOLERANCE = 2;
  public static final DoubleParameter riTolerance = new DoubleParameter("RI tolerance",
      "Absolute RI tolerance used for ranking.", ConfigService.getGuiFormats().rtFormat(),
      DEFAULT_RI_TOLERANCE, 0d, null);

  public static final AnnotationSummaryOrder DEFAULT_SORT_ORDER = AnnotationSummaryOrder.getDefault();
  public static final ComboParameter<AnnotationSummaryOrder> sorting = new ComboParameter<>(
      "Confidence sorting", """
      Define how annotation confidence will be ranked.
      %s
      See documentation for further details.""".formatted(AnnotationSummaryOrder.getDescriptions()),
      AnnotationSummaryOrder.values(), DEFAULT_SORT_ORDER);


  public PreferredAnnotationRankingParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/terminology/annotations.html#preferred-annotation",
        flists, mzTolerance, rtTolerance, ccsTolerance, riTolerance, sorting);
  }

  public AnnotationSummarySortConfig toConfig() {
    return new AnnotationSummarySortConfig(
        Objects.requireNonNullElse(getValue(mzTolerance), MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA),
        Objects.requireNonNullElse(getValue(rtTolerance), DEFAULT_RT_TOLERANCE),
        Objects.requireNonNullElse(getValue(ccsTolerance), DEFAULT_CCS_TOLERANCE),
        Objects.requireNonNullElse(getValue(riTolerance), DEFAULT_RI_TOLERANCE),
        Objects.requireNonNullElse(getValue(sorting), DEFAULT_SORT_ORDER));
  }

  public static PreferredAnnotationRankingParameters fromConfig(
      @NotNull AnnotationSummarySortConfig config) {
    ParameterSet param = new PreferredAnnotationRankingParameters().cloneParameterSet();
    param.setParameter(mzTolerance, config.mzTolerance());
    param.setParameter(rtTolerance, config.rtTolerance());
    param.setParameter(ccsTolerance, config.ccsTolerance());
    param.setParameter(riTolerance, config.riTolerance());
    param.setParameter(sorting, config.sortOrder());
    return (PreferredAnnotationRankingParameters) param;
  }
}
