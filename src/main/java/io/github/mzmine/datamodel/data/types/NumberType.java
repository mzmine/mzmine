package io.github.mzmine.datamodel.data.types;

import java.text.NumberFormat;

public abstract class NumberType<T extends Number> extends DataType<T> {

  @Override
  public String getFormattedString() {
    return getFormatter().format(value);
  }

  public abstract NumberFormat getFormatter();

}
