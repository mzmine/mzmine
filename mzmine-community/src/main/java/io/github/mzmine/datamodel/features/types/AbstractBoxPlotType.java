/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.types;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.fx.ColumnID;
import io.github.mzmine.datamodel.features.types.fx.ColumnType;
import io.github.mzmine.datamodel.features.types.fx.MetadataHeaderColumn;
import io.github.mzmine.datamodel.features.types.graphicalnodes.AbundanceBoxPlotCell;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.MinSamplesRequirement;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.statsdashboard.StatsDashboardTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import java.util.Map;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractBoxPlotType extends LinkedGraphicalType implements
    MinSamplesRequirement {

  private final AbundanceMeasure abundanceMeasure;

  protected AbstractBoxPlotType(AbundanceMeasure abundanceMeasure) {
    this.abundanceMeasure = abundanceMeasure;
  }

  @Override
  public double getColumnWidth() {
    return GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH;
  }

  @Override
  public @Nullable Node createCellContent(@NotNull ModularFeatureListRow row, Boolean cellData,
      @Nullable RawDataFile raw, AtomicDouble progress) {

    throw new IllegalStateException("Statement should be unreachable due to custom cell factory.");
  }

  @Override
  public @Nullable TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType, int subColumnIndex) {

    final MetadataHeaderColumn<ModularFeatureListRow, Object> col = new MetadataHeaderColumn<>(this,
        ProjectService.getMetadata().getSampleTypeColumn());

    // define observable
    col.setCellFactory(c -> (TreeTableCell) new AbundanceBoxPlotCell(col.selectedColumnProperty(),
        abundanceMeasure));
//    col.setCellValueFactory(new DataTypeCellValueFactory(raw, this, parentType, subColumnIndex));
    col.setCellValueFactory(cdf -> new ReadOnlyObjectWrapper<>(cdf.getValue().getValue()));
    return col;
  }

  @Override
  public @Nullable Runnable getDoubleClickAction(@Nullable FeatureTableFX table,
      @NotNull ModularFeatureListRow row, @NotNull List<RawDataFile> file,
      @Nullable DataType<?> superType, @Nullable Object value) {
    return () -> {
      if (table == null || table.getFeatureList() == null) {
        return;
      }
      FxThread.runLater(() -> {
        final StatsDashboardTab tab = new StatsDashboardTab();
        tab.onFeatureListSelectionChanged(List.of(table.getFeatureList()));
        tab.getController().selectedRowsProperty().set(List.of(row));

        final ColumnID colId = new ColumnID(this, ColumnType.ROW_TYPE, null, -1);
        final Map<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> map = table.getNewColumnMap();
        final MetadataColumn<?> selectedColumn = map.entrySet().stream()
            .filter(entry -> entry.getValue().getUniqueIdString().equals(colId.getUniqueIdString()))
            .findFirst().map(entry -> ((MetadataHeaderColumn) entry.getKey()).getSelectedColumn())
            .orElse(ProjectService.getMetadata().getSampleTypeColumn());

        tab.getController().groupingColumnProperty().set(selectedColumn);
        tab.getController().abundanceMeasureProperty().set(abundanceMeasure);
        MZmineCore.getDesktop().addTab(tab);
      });
    };
  }
}
