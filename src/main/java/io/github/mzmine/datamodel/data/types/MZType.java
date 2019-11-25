package io.github.mzmine.datamodel.data.types;

import java.text.NumberFormat;
import io.github.mzmine.main.MZmineCore;

public class MZType extends NumberType<Double> {

  @Override
  public NumberFormat getFormatter() {
    return MZmineCore.getConfiguration().getMZFormat();
  }

}
