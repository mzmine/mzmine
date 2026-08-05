package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.graphicalnodes.CompoundIdTreeCell;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableOwner;
import java.util.List;
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

    column.setCellFactory(_ -> new CompoundIdTreeCell());

    return column;
  }

  @Override
  public double getPrefColumnWidth() {
    return super.getPrefColumnWidth() + 25;
  }

  @Override
  public @Nullable Runnable getDoubleClickAction(@Nullable FeatureTableFX table,
      @NotNull ModularFeatureListRow row, @NotNull List<RawDataFile> file,
      @Nullable DataType<?> superType, @Nullable Object value) {
    return () -> {
      if (table == null || table.getFeatureList() == null
          || table.getTableOwner() == FeatureTableOwner.COMPOUND_DASHBOARD) {
        return;
      }

      final ModularFeatureList flist = table.getFeatureList();
      if (flist == null || flist.getCompoundList() == null) {
        return;
      }

      MZmineCore.getDesktop().addTab(new CompoundDashboardTab(flist));
    };
  }
}
