package io.github.mzmine.modules.dataprocessing.comb_resolver;

import io.github.mzmine.modules.dataprocessing.featdet_ML.MLFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;

public enum CombinedResolverEnum implements ModuleOptionsEnum<FeatureResolverModule> {
    MLRESOLVER, LOCALMIN;

    @java.lang.Override
    public Class<? extends FeatureResolverModule> getModuleClass() {
        return switch (this) {
            case MLRESOLVER -> MLFeatureResolverModule.class;
            case LOCALMIN -> MinimumSearchFeatureResolverModule.class;
        };
    }

    @java.lang.Override
    public String getStableId() {
        return switch (this){
            case MLRESOLVER -> "machine_learning_resolver";
            case LOCALMIN -> "local_min_reolver";
    };
    }
}
