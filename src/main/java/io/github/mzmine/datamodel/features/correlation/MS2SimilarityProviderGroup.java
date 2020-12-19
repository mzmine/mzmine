package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.RowGroup;

import java.util.List;

public abstract class MS2SimilarityProviderGroup extends RowGroup {

  public MS2SimilarityProviderGroup(List<RawDataFile> raw, int groupID) {
    super(raw, groupID);
  }

  /**
   * A map for row-2-row MS2 similarity
   * 
   * @return
   */
  public abstract R2RMap<R2RMS2Similarity> getMS2SimilarityMap();

  /**
   * Similarity map for row-2-row MS2 comparison
   * 
   * @param map
   * @return
   */
  public abstract void setMS2SimilarityMap(R2RMap<R2RMS2Similarity> map);
}
