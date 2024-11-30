package io.github.mzmine.modules.dataprocessing.comb_resolver;

import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverModule;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CombinedResolverModule extends FeatureResolverModule {

  public static final String NAME = "Combined Resolver";

  @Override
  public @NotNull String getDescription() {
    return "Creates a new resolver that combines the results from other resolvers";
  }

  @Override
  public @NotNull String getName() {
    return NAME;
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return CombinedResolverParameters.class;
  }
}
