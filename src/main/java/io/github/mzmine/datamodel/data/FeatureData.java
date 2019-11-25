package io.github.mzmine.datamodel.data;

import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.IntensityType;
import io.github.mzmine.datamodel.data.types.MZType;
import io.github.mzmine.datamodel.data.types.RTType;

/**
 * Defines all feature related data (keys)
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public enum FeatureData {
  MZ(MZType.class), RT(RTType.class), //
  INTENSITY(IntensityType.class), AREA(IntensityType.class), //
  DETECTION_STATUS(DetectionType.class);


  // data type
  private Class type;

  FeatureData(Class type) {
    this.type = type;
  }

  public Class getType() {
    return type;
  }

  public boolean checkType(Object o) {
    return type.isInstance(o);
  }
}
