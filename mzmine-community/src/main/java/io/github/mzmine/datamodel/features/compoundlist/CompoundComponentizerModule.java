package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

/**
 * Base module type for compound componentizer strategies. Each implementation owns its own
 * {@link ParameterSet} and constructs a {@link CompoundComponentizerStrategy} from it. The
 * representative-row selector is supplied externally (it is a top-level grouper parameter, shared
 * across componentizers).
 */
public interface CompoundComponentizerModule extends MZmineModule {

  /**
   * Build a strategy instance configured from the given parameter set and representative selector.
   * The parameter set must be of the type returned by {@link #getParameterSetClass()}.
   */
  @NotNull CompoundComponentizerStrategy createStrategy(@NotNull ParameterSet parameters,
      @NotNull CompoundRepresentativeSelector representativeSelector);
}
