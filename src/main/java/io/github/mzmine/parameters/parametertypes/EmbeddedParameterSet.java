package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;

/**
 * In case a parameter embeds a parameter set, this interface shall be implemented. This is required
 * because embedded parameter sets might have a {@link io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter}
 * which needs to be set. With this interface, it can be done via {@link
 * io.github.mzmine.modules.batchmode.BatchTask#setBatchlastFeatureListsToParamSet(MZmineProcessingModule,
 * ParameterSet)}
 */
public interface EmbeddedParameterSet {

  ParameterSet getEmbeddedParameters();

}
