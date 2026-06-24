package io.github.mzmine.modules.dataprocessing.align_common;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public interface FeatureAlignmentPostProcessor {

  void handlePostAlignment(ModularFeatureList flist);
}
