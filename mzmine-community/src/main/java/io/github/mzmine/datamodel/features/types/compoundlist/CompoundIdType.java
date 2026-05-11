package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.graphicalnodes.CompoundIdTreeCell;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompoundIdType extends IntegerType {

  @Override
  public @NotNull String getUniqueID() {
    return "compound_id";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Comp ID";
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }

  @Override
  public @Nullable TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType, int subColumnIndex) {
    final TreeTableColumn<ModularFeatureListRow, Object> column = super.createColumn(raw,
        parentType, subColumnIndex);

    column.getStyleClass().add("full-height-cell");
    column.setCellFactory(param -> new CompoundIdTreeCell());

    return column;
  }

  @Override
  public double getPrefColumnWidth() {
    return super.getPrefColumnWidth()+25;
  }
}
