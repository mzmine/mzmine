package io.github.mzmine.modules.dataprocessing.comb_resolver;

import java.time.Instant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.impl.TaskPerFeatureListModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;

public class CombinedResolverModule extends TaskPerFeatureListModule {
    public final String name;
    public final String description;

    public CombinedResolverModule(final @NotNull String name, final @NotNull CombinedResolverParameters.class paramterSetClass, final @NotNull MZmineModuleCategory  moduleCategory, final boolean requiresMemoryMapping, final @NotNull String description){
        this.name = name;
        this.description = description;
        }


    public createTask(final @NotNull MZmineProject project, final @NotNull ParameterSet parameters, final @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage, @NotNull final FeatureList featureList){
        return new Task();
    }


}
