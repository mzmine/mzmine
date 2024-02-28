package io.github.mzmine.modules.dataprocessing.featdet_imsexpander;

import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;

/**
 * Represents a new trace with all computations done, just the need to create the new row.
 *
 * @param series
 * @param oldRow
 * @param oldFeature
 */
public record ExpandedTrace(IonMobilogramTimeSeries series, ModularFeatureListRow oldRow,
                            ModularFeature oldFeature) {

}
