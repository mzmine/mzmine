/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataanalysis.rowsboxplot;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.framework.fx.SelectedAbundanceMeasureBinding;
import io.github.mzmine.gui.framework.fx.SelectedMetadataColumnBinding;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RowsBoxplotController extends FxController<RowsBoxplotModel> implements
    SelectedRowsBinding, SelectedMetadataColumnBinding, SelectedAbundanceMeasureBinding {

  private final RowsBoxplotViewBuilder builder;

  public RowsBoxplotController() {
    super(new RowsBoxplotModel());
    builder = new RowsBoxplotViewBuilder(model);

    model.selectedRowsProperty().addListener((_, _, n) -> updateDataset(n));
    model.abundanceMeasureProperty()
        .addListener((_, _, n) -> updateDataset(model.getSelectedRows()));
    model.groupingColumnProperty().addListener((_, _, n) -> updateDataset(model.getSelectedRows()));
  }

  private void updateDataset(List<FeatureListRow> n) {
    onGuiThread(() -> {
      if (n == null || n.isEmpty()) {
        model.setDataset(null);
        return;
      }
      model.setDataset(
          new RowBoxPlotDataset(n.getFirst(), model.getGroupingColumn(), model.getAbundanceMeasure()));
    });
  }

  @Override
  protected @NotNull FxViewBuilder<RowsBoxplotModel> getViewBuilder() {
    return builder;
  }

  @Override
  public ObjectProperty<AbundanceMeasure> abundanceMeasureProperty() {
    return model.abundanceMeasureProperty();
  }

  @Override
  public ObjectProperty<@Nullable MetadataColumn<?>> groupingColumnProperty() {
    return model.groupingColumnProperty();
  }

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }

  public BooleanProperty showCategoryAxisLabelProperty() {
    return model.showCategoryAxisLabelProperty();
  }

  public BooleanProperty showTitleProperty() {
    return model.showTitleProperty();
  }

  public BooleanProperty showColumnAxisLabelsProperty() {
    return model.showCategoryAxisColumnLabelsProperty();
  }
}
