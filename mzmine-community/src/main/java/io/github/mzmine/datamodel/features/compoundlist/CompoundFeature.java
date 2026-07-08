package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import org.jetbrains.annotations.NotNull;

/**
 * A synthetic feature that aggregates per-{@link io.github.mzmine.datamodel.RawDataFile} intensity
 * values onto a {@link ModularCompoundRow}. Distinct from a regular
 * {@link io.github.mzmine.datamodel.features.ModularFeature} because it holds back-references to
 * the owning {@link CompoundList} and {@link ModularCompoundRow}, and falls back to the
 * representative source row's feature for any
 * {@link io.github.mzmine.datamodel.features.types.DataType} not explicitly set on the compound
 * feature.
 */
public interface CompoundFeature extends Feature {

  @NotNull CompoundList getCompoundList();

  /**
   * @return the one {@link CompoundRow} the feature belongs to
   */
  @NotNull ModularCompoundRow getCompoundRow();

  /**
   * @return the representative row within the {@link CompoundRow#getPreferredRow()}
   */
  @NotNull ModularFeatureListRow getPreferredRow();
}
