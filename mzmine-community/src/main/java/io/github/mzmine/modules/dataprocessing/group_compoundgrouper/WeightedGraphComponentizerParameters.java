package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.text.DecimalFormat;

/**
 * Parameters for {@link WeightedGraphComponentizer}. Evidence weights scale each evidence term when
 * combining structural edges and scoring loose-row assignments.
 */
public class WeightedGraphComponentizerParameters extends SimpleParameterSet {

  private static final DecimalFormat DEC2 = new DecimalFormat("0.00");

  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter(
      ToleranceType.INTRA_SAMPLE,
      "Tolerance for matching isotopologue m/z spacings when building isotope edges.",
      MZTolerance.NARROW_5_PPM_OR_1_MDA);

  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum retention time difference for two rows to be considered RT-coherent (gates RT "
          + "edges and isotopologue detection).", new RTTolerance(0.08f, Unit.MINUTES));

  public static final DoubleParameter W_RT = new DoubleParameter("Weight: RT coherence",
      "Edge/score weight for two rows being within RT tolerance. Highest by default.", DEC2, 2.0,
      0d, 10d);

  public static final DoubleParameter W_SHAPE = new DoubleParameter("Weight: shape correlation",
      "Edge/score weight applied to the MS1 feature-shape correlation score (avg Pearson r).", DEC2,
      1.0, 0d, 10d);

  public static final DoubleParameter W_IIN = new DoubleParameter("Weight: ion identity",
      "Edge/score weight for two rows sharing an ion identity network.", DEC2, 0.5, 0d, 10d);

  public static final DoubleParameter W_ISOTOPE = new DoubleParameter("Weight: isotope",
      "Edge/score weight for an isotopologue m/z spacing match.", DEC2, 0.5, 0d, 10d);

  public static final DoubleParameter W_ANNOTATION = new DoubleParameter("Weight: annotation",
      "Score boost when a loose row's annotation agrees with the core representative, and penalty "
          + "when it contradicts.", DEC2, 0.5, 0d, 10d);

  public static final DoubleParameter MIN_CORE_DENSITY = new DoubleParameter("Min core density",
      "Minimum edge density [0..1] for a residual correlation cluster to be accepted as a core. "
          + "Lower values accept looser communities; failing nodes are peeled off.", DEC2, 0.3, 0d,
      1d);

  public static final DoubleParameter CORE_MERGE_OVERLAP = new DoubleParameter("Core merge overlap",
      "Jaccard overlap [0..1] above which a newly detected correlation core is merged into an "
          + "existing one instead of emitting a near-duplicate compound.", DEC2, 0.5, 0d, 1d);

  public static final DoubleParameter ASSIGNMENT_THRESHOLD = new DoubleParameter(
      "Assignment confidence",
      "Normalized posterior [0..1] above which a loose row is confidently assigned to its single "
          + "best core. Below this, a genuine near-tie (see margin) yields dual membership.", DEC2,
      0.6, 0d, 1d);

  public static final DoubleParameter NEAR_TIE_MARGIN = new DoubleParameter("Near-tie margin",
      "A loose row also joins its runner-up core when that core's posterior is within this margin "
          + "of the best core (and the row has no forcing annotation / ion identity).", DEC2, 0.1,
      0d, 1d);

  public static final IntegerParameter SIZE_PENALTY_THRESHOLD = new IntegerParameter(
      "Size penalty threshold",
      "Cores larger than this member count get their assignment scores down-weighted.", 10, 1,
      100000);

  public static final DoubleParameter SIZE_PENALTY_ALPHA = new DoubleParameter("Size penalty",
      "Fractional down-weight [0..1] applied to oversized cores' assignment scores.", DEC2, 0.3, 0d,
      1d);

  public static final DoubleParameter RT_SPREAD_THRESHOLD = new DoubleParameter(
      "RT spread threshold (min)",
      "If member retention times span more than this, an RT-spread contradiction is recorded.",
      new DecimalFormat("0.000"), 0.05, 0d, 100d);

  public static final IntegerParameter MP_ITERATIONS = new IntegerParameter("Message-passing rounds",
      "Number of smoothing rounds that let neighboring rows pull each other toward consistent core "
          + "assignments. 0 disables smoothing.", 3, 0, 100);

  public static final DoubleParameter MP_DAMPING = new DoubleParameter("Message-passing damping",
      "Fraction [0..1] of a loose row's score that comes from its neighbors during smoothing.",
      DEC2, 0.5, 0d, 1d);

  public static final BooleanParameter SPLIT_ON_ANNOTATION_CONFLICT = new BooleanParameter(
      "Split on annotation conflict",
      "Split a compound into per-structure groups when its members carry MS2 library matches to "
          + "different structures.", true);

  public WeightedGraphComponentizerParameters() {
    super(MZ_TOLERANCE, RT_TOLERANCE, W_RT, W_SHAPE, W_IIN, W_ISOTOPE, W_ANNOTATION, MIN_CORE_DENSITY,
        CORE_MERGE_OVERLAP, ASSIGNMENT_THRESHOLD, NEAR_TIE_MARGIN, SIZE_PENALTY_THRESHOLD,
        SIZE_PENALTY_ALPHA, RT_SPREAD_THRESHOLD, MP_ITERATIONS, MP_DAMPING,
        SPLIT_ON_ANNOTATION_CONFLICT);
  }
}
