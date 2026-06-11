package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.features.compoundlist.CompoundComponentizerModule;
import io.github.mzmine.datamodel.features.compoundlist.CompoundComponentizerStrategy;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelector;
import io.github.mzmine.modules.dataprocessing.group_compoundgrouper.WeightedGraphComponentizer.Config;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

/**
 * Provides the {@link WeightedGraphComponentizer}: multi-evidence weighted-graph compound grouping
 * with dense-core seeding + de-duplication, single-best assignment (near-tie dual membership) via
 * bounded message passing, and annotation-contradiction detection.
 */
public class WeightedGraphComponentizerModule implements CompoundComponentizerModule {

  @Override
  public @NotNull String getName() {
    return CompoundComponentizerType.WeightedGraph.toString();
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return WeightedGraphComponentizerParameters.class;
  }

  @Override
  public @NotNull CompoundComponentizerStrategy createStrategy(@NotNull final ParameterSet p,
      @NotNull final CompoundRepresentativeSelector representativeSelector) {
    final Config config = new Config(
        p.getValue(WeightedGraphComponentizerParameters.MZ_TOLERANCE),
        p.getValue(WeightedGraphComponentizerParameters.RT_TOLERANCE),
        p.getValue(WeightedGraphComponentizerParameters.W_RT),
        p.getValue(WeightedGraphComponentizerParameters.W_SHAPE),
        p.getValue(WeightedGraphComponentizerParameters.W_IIN),
        p.getValue(WeightedGraphComponentizerParameters.W_ISOTOPE),
        p.getValue(WeightedGraphComponentizerParameters.W_ANNOTATION),
        p.getValue(WeightedGraphComponentizerParameters.MIN_CORE_DENSITY),
        p.getValue(WeightedGraphComponentizerParameters.CORE_MERGE_OVERLAP),
        p.getValue(WeightedGraphComponentizerParameters.ASSIGNMENT_THRESHOLD),
        p.getValue(WeightedGraphComponentizerParameters.NEAR_TIE_MARGIN),
        p.getValue(WeightedGraphComponentizerParameters.SIZE_PENALTY_THRESHOLD),
        p.getValue(WeightedGraphComponentizerParameters.SIZE_PENALTY_ALPHA),
        p.getValue(WeightedGraphComponentizerParameters.RT_SPREAD_THRESHOLD),
        p.getValue(WeightedGraphComponentizerParameters.MP_ITERATIONS),
        p.getValue(WeightedGraphComponentizerParameters.MP_DAMPING),
        p.getValue(WeightedGraphComponentizerParameters.SPLIT_ON_ANNOTATION_CONFLICT));
    return new WeightedGraphComponentizer(config, representativeSelector);
  }
}
