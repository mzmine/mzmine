package io.github.mzmine.datamodel.features.types;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.features.types.modifiers.ExpandableType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.DoubleType;

public class LipidMsOneErrorType extends DoubleType implements ExpandableType {

  public LipidMsOneErrorType() {
    super(new DecimalFormat("0.0"));
  }

  @Override
  public NumberFormat getFormatter() {
    return DEFAULT_FORMAT;
  }

  @Override
  public String getHeaderString() {
    return "\u0394 ppm";
  }

  @Nonnull
  @Override
  public Class<? extends DataType<?>> getExpandedTypeClass() {
    return LipidMsOneErrorType.class;
  }

  @Nonnull
  @Override
  public Class<? extends DataType<?>> getHiddenTypeClass() {
    return getClass();
  }

  @Nonnull
  @Override
  public List<RowBinding> createDefaultRowBindings() {
    return List.of(new SimpleRowBinding(this, BindingsType.AVERAGE));
  }
}
