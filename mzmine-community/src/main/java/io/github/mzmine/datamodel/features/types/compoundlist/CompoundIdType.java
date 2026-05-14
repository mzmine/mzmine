package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompoundIdType extends IntegerType {

  @Override
  public @NotNull String getUniqueID() {
    return "compound_id";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Compound ID";
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }

  @Override
  public @Nullable Runnable getDoubleClickAction(@Nullable FeatureTableFX table,
      @NotNull ModularFeatureListRow row, @NotNull List<RawDataFile> file,
      @Nullable DataType<?> superType, @Nullable Object value) {
    return super.getDoubleClickAction(table, row, file, superType, value);
  }
}
