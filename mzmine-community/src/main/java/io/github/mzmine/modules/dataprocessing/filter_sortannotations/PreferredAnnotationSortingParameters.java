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

public class PreferredAnnotationSortingParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA.getMzTolerance(),
      MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA.getPpmTolerance());

  public static final RTTolerance DEFAULT_RT_TOLERANCE = new RTTolerance(0.3f, Unit.MINUTES);
  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter(
      DEFAULT_RT_TOLERANCE);

  public static final double DEFAULT_CCS_TOLERANCE = 0.1;
  public static final PercentParameter ccsTolerance = new PercentParameter("CCS tolerance",
      "CCS tolerance used for ranking", DEFAULT_CCS_TOLERANCE, 0d, 1d);

  public static final double DEFAULT_RI_TOLERANCE = 2;
  public static final DoubleParameter riTolerance = new DoubleParameter("RI tolerance",
      "Absolute RI tolerance used for ranking", ConfigService.getGuiFormats().rtFormat(),
      DEFAULT_RI_TOLERANCE, 0d, Double.MAX_VALUE);

  public static final AnnotationSummaryOrder DEFAULT_SORT_ORDER = AnnotationSummaryOrder.MZMINE;
  public static final ComboParameter<AnnotationSummaryOrder> sorting = new ComboParameter<>(
      "Confidence sorting", "Define how annotation confidence will be ranked.",
      AnnotationSummaryOrder.values(), DEFAULT_SORT_ORDER);


  public PreferredAnnotationSortingParameters() {
    super(flists, mzTolerance, rtTolerance, ccsTolerance, riTolerance, sorting);
  }

  public AnnotationSummarySortConfig toConfig() {
    return new AnnotationSummarySortConfig(
        Objects.requireNonNullElse(getValue(mzTolerance), MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA),
        Objects.requireNonNullElse(getValue(rtTolerance), DEFAULT_RT_TOLERANCE),
        Objects.requireNonNullElse(getValue(ccsTolerance), DEFAULT_CCS_TOLERANCE),
        Objects.requireNonNullElse(getValue(riTolerance), DEFAULT_RI_TOLERANCE),
        Objects.requireNonNullElse(getValue(sorting), DEFAULT_SORT_ORDER));
  }

  public static PreferredAnnotationSortingParameters fromConfig(
      @NotNull AnnotationSummarySortConfig config) {
    ParameterSet param = new PreferredAnnotationSortingParameters().cloneParameterSet();
    param.setParameter(mzTolerance, config.mzTolerance());
    param.setParameter(rtTolerance, config.rtTolerance());
    param.setParameter(ccsTolerance, config.ccsTolerance());
    param.setParameter(riTolerance, config.riTolerance());
    param.setParameter(sorting, config.sortOrder());
    return (PreferredAnnotationSortingParameters) param;
  }
}
