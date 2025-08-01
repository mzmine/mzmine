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

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.modules.dataanalysis.rowsboxplot.RowsBoxplotController;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeTableCell;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AbundanceBoxPlotCell extends
    TreeTableCell<ModularFeatureListRow, ModularFeatureListRow> {

  private final RowsBoxplotController boxPlot;
  private final Region view;

  public AbundanceBoxPlotCell(@NotNull ObjectProperty<@Nullable MetadataColumn<?>> groupingColumn,
      AbundanceMeasure abundanceMeasure) {
    super();

    boxPlot = new RowsBoxplotController();
    setMinHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
    boxPlot.abundanceMeasureProperty().set(abundanceMeasure);
    boxPlot.groupingColumnProperty().bindBidirectional(groupingColumn);

    boxPlot.showCategoryAxisLabelProperty().set(false);
    boxPlot.showTitleProperty().set(false);
    boxPlot.showColumnAxisLabelsProperty().set(false);
    view = boxPlot.buildView();

    PropertyUtils.onChange(() -> {
      final ModularFeatureListRow row = itemProperty().get();
      if (row != null && !isEmpty()) {
        boxPlot.selectedRowsProperty().set(List.of(row));
        setGraphic(view);
      } else {
        boxPlot.selectedRowsProperty().set(List.of());
        setGraphic(null);
      }
    }, itemProperty(), emptyProperty());

    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
  }
}
