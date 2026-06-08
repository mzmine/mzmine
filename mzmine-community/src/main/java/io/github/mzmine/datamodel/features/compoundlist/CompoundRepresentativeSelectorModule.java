package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

/**
 * Base module type for representative-row selection strategies. Each implementation owns its own
 * {@link ParameterSet} and constructs a {@link CompoundRepresentativeSelector} from it.
 */
public interface CompoundRepresentativeSelectorModule extends MZmineModule {

  /**
   * Build a selector instance configured from the given parameter set. The parameter set must be
   * of the type returned by {@link #getParameterSetClass()}.
   */
  @NotNull CompoundRepresentativeSelector createSelector(@NotNull ParameterSet parameters);
}
