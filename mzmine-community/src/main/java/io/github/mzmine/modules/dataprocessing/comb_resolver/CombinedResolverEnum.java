package io.github.mzmine.modules.dataprocessing.comb_resolver;

import io.github.mzmine.modules.dataprocessing.featdet_ML.MLFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_ML.MLFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_ML.MLFeatureResolverParameters.MLSetup;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.Setup;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;

public enum CombinedResolverEnum implements ModuleOptionsEnum<FeatureResolverModule> {
  ML_RESOLVER, LOCAL_MIN;

  @java.lang.Override
  public Class<? extends FeatureResolverModule> getModuleClass() {
    return switch (this) {
      case ML_RESOLVER -> MLFeatureResolverModule.class;
      case LOCAL_MIN -> MinimumSearchFeatureResolverModule.class;
    };
  }

  @java.lang.Override
  public String getStableId() {
    return switch (this) {
      case ML_RESOLVER -> "machine_learning_resolver";
      case LOCAL_MIN -> "local_min_reolver";
    };
  }

  @Override
  public GeneralResolverParameters getModuleParameters() {
    return switch (this) {
      case LOCAL_MIN ->
          (GeneralResolverParameters) new MinimumSearchFeatureResolverParameters(Setup.INTEGRATED);
      case ML_RESOLVER ->
          (GeneralResolverParameters) new MLFeatureResolverParameters(MLSetup.INTEGRATED);
    };
  }
}
