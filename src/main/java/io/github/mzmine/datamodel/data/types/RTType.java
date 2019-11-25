package io.github.mzmine.datamodel.data.types;

import java.text.NumberFormat;
import io.github.mzmine.main.MZmineCore;

public class RTType extends NumberType<Float> {

  @Override
  public NumberFormat getFormatter() {
    return MZmineCore.getConfiguration().getRTFormat();
  }

}
