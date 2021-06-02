package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;

/**
 * row to row correlation (2 rows) Intensity profile and peak shape correlation
 *
 * @author Robin Schmid
 */
public abstract class R2RCorrelationData implements RowsRelationship {

  // correlation of a to b
  // id A < id B
  private final FeatureListRow a;
  private final FeatureListRow b;

  public R2RCorrelationData(FeatureListRow a, FeatureListRow b) {
    if (a.getID() < b.getID()) {
      this.a = a;
      this.b = b;
    } else {
      this.b = a;
      this.a = b;
    }
  }

  @Override
  public FeatureListRow getRowB() {
    return b;
  }

  @Override
  public FeatureListRow getRowA() {
    return a;
  }

}
