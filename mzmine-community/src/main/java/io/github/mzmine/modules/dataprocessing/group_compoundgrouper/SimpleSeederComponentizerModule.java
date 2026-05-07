package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.features.compoundlist.CompoundComponentizerModule;
import io.github.mzmine.datamodel.features.compoundlist.CompoundComponentizerStrategy;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import org.jetbrains.annotations.NotNull;

/**
 * Provides the {@link SimpleSeederComponentizer} strategy: IIN-seeded compounds with
 * correlation-aware expansion (dual-membership for bridge rows) and density-first community
 * detection on the residual (no-IIN) correlation subgraph.
 */
public class SimpleSeederComponentizerModule implements CompoundComponentizerModule {

  @Override
  public @NotNull String getName() {
    return CompoundComponentizerType.SimpleSeeder.toString();
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return SimpleSeederComponentizerParameters.class;
  }

  @Override
  public @NotNull CompoundComponentizerStrategy createStrategy(
      @NotNull final ParameterSet parameters) {
    final MZTolerance mzTol = parameters.getValue(SimpleSeederComponentizerParameters.MZ_TOLERANCE);
    final RTTolerance rtTol = parameters.getValue(SimpleSeederComponentizerParameters.RT_TOLERANCE);
    final double minDensity = parameters.getValue(
        SimpleSeederComponentizerParameters.MIN_DENSITY);
    return new SimpleSeederComponentizer(mzTol, rtTol, minDensity);
  }
}
