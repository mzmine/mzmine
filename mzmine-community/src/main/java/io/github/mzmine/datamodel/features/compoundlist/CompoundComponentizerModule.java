package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

/**
 * Base module type for compound componentizer strategies. Each implementation owns its own
 * {@link ParameterSet} and constructs a {@link CompoundComponentizerStrategy} from it.
 */
public interface CompoundComponentizerModule extends MZmineModule {

  /**
   * Build a strategy instance configured from the given parameter set. The parameter set must be
   * of the type returned by {@link #getParameterSetClass()}.
   */
  @NotNull CompoundComponentizerStrategy createStrategy(@NotNull ParameterSet parameters);
}
