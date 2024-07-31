package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.loess.LoessBaselineCorrector;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;

public enum BaselineCorrectors implements ModuleOptionsEnum {
  LOESS;

  @Override
  public Class<? extends MZmineModule> getModuleClass() {
    return switch (this) {
      case LOESS -> LoessBaselineCorrector.class;
    };
  }

  @Override
  public String getStableId() {
    return switch (this) {
      case LOESS -> "loess_baseline_corrector";
    };
  }
}
