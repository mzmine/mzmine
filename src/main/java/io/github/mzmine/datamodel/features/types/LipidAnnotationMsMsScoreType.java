package io.github.mzmine.datamodel.features.types;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import io.github.mzmine.datamodel.features.types.numbers.abstr.DoubleType;

public class LipidAnnotationMsMsScoreType extends DoubleType {

  public LipidAnnotationMsMsScoreType() {
    super(new DecimalFormat("0.0"));
  }

  @Override
  public NumberFormat getFormatter() {
    // only happens if types are used without initializing the MZmineCore
    return DEFAULT_FORMAT;
  }

  @Override
  public String getHeaderString() {
    return "MS/MS Score";
  }

}
