package io.github.mzmine.datamodel.data;

import org.apache.poi.ss.formula.functions.T;
import io.github.mzmine.datamodel.data.types.DataType;

public class WrongTypeException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public WrongTypeException(FeatureData key, DataType<T> data) {
    super(
        "Wrong type for feature data. This is a programming error in this module. Please report with full log. Key: "
            + key.toString() + "; value: " + data.getClass().descriptorString());
  }
}
