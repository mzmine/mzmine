package io.github.mzmine.modules.tools.siriusapi;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.FeatureUtils;
import io.sirius.ms.sdk.model.AlignedFeature;

public class SiriusFeatureDoesNotMatchMzmineFeatureException extends RuntimeException {

  public SiriusFeatureDoesNotMatchMzmineFeatureException(String projectId,
      AlignedFeature alignedFeature, FeatureListRow row) {

    super(
        "Error while importing Sirius results for row %s. Sirius feature %s in project %s does not match mzmine feature. Is the correct project selected?.".formatted(
            FeatureUtils.rowToString(row), SiriusToMzmine.alignedFeatureToString(alignedFeature),
            projectId));
  }


}
