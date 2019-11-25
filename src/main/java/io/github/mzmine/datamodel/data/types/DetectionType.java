package io.github.mzmine.datamodel.data.types;

import io.github.mzmine.datamodel.Feature.FeatureStatus;

public class DetectionType extends DataType<FeatureStatus> {

  @Override
  public String getFormattedString() {
    return value.toString();
  }


}
