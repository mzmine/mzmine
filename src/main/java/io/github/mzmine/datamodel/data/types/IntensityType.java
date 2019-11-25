package io.github.mzmine.datamodel.data.types;

import java.text.NumberFormat;
import io.github.mzmine.main.MZmineCore;

public class IntensityType extends NumberType<Float> {

  @Override
  public NumberFormat getFormatter() {
    return MZmineCore.getConfiguration().getIntensityFormat();
  }

}
