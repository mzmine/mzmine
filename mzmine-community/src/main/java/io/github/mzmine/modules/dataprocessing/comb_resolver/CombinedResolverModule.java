package io.github.mzmine.modules.dataprocessing.comb_resolver;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverModule;
import io.github.mzmine.modules.impl.TaskPerFeatureListModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;

import java.time.Instant;

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
