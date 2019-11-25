package io.github.mzmine.datamodel.data;

import java.util.EnumMap;
import io.github.mzmine.datamodel.Feature.FeatureStatus;
import io.github.mzmine.datamodel.data.types.DataType;

/**
 * Map of all feature related data.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class FeatureDataMap {
  private static final long serialVersionUID = 1L;

  private EnumMap<FeatureData, DataType> map = new EnumMap<>(FeatureData.class);

  public DataType get(FeatureData key) {
    return map.get(key);
  }

  public DataType set(FeatureData key, DataType data) {
    if (key.checkType(data))
      return map.put(key, data);
    // wrong data type. Check code that supplied this data
    throw new WrongTypeException(key, data);
  }


  public FeatureStatus getFeatureStatus() {
    DataType data = get(FeatureData.DETECTION_STATUS);
    return data == null ? FeatureStatus.UNKNOWN : (FeatureStatus) data.getValue();
  }

  public double getMZ() {
    DataType data = get(FeatureData.MZ);
    return data == null ? 0d : (Double) data.getValue();
  }

  public double getRT() {
    DataType data = get(FeatureData.RT);
    return data == null ? 0d : (Float) data.getValue();
  }

  public double getHeight() {
    DataType data = get(FeatureData.INTENSITY);
    return data == null ? 0d : (Float) data.getValue();
  }

  public double getArea() {
    DataType data = get(FeatureData.AREA);
    return data == null ? 0d : (Float) data.getValue();
  }

}
