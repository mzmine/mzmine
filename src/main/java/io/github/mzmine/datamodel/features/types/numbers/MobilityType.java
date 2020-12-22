package io.github.mzmine.datamodel.features.types.numbers;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.ExpandableType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.main.MZmineCore;

public class MobilityType extends FloatType implements ExpandableType {

  public MobilityType() {
    super(new DecimalFormat("0.00"));
  }

  @Override
  public NumberFormat getFormatter() {
    try {
      return MZmineCore.getConfiguration().getRTFormat();
    } catch (NullPointerException e) {
      // only happens if types are used without initializing the MZmineCore
      return DEFAULT_FORMAT;
    }
  }

  @Override
  public String getHeaderString() {
    return "Mobility";
  }

  @Nonnull
  @Override
  public Class<? extends DataType<?>> getExpandedTypeClass() {
    return RTRangeType.class;
  }

  @Nonnull
  @Override
  public Class<? extends DataType<?>> getHiddenTypeClass() {
    return getClass();
  }
}
