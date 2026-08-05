package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelector;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelectorModule;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

public class AnnotatedFirstRepresentativeSelectorModule implements
    CompoundRepresentativeSelectorModule {

  @Override
  public @NotNull String getName() {
    return CompoundRepresentativeSelectorOption.PREFER_ANNOTATED.toString();
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return AnnotatedFirstRepresentativeSelectorParameters.class;
  }

  @Override
  public @NotNull CompoundRepresentativeSelector createSelector(
      @NotNull final ParameterSet parameters) {
    return new AnnotatedFirstRepresentativeSelector();
  }
}
