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

package io.github.mzmine.modules.visualization.feat_histogram;

import static io.github.mzmine.javafx.components.factories.FxComboBox.newSearchableComboBox;
import static io.github.mzmine.javafx.components.util.FxLayout.newFlowPane;

import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.visualization.scan_histogram.chart.HistogramPanel;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

public class FeatureHistogramPlotViewBuilder extends FxViewBuilder<FeatureHistogramPlotModel> {

  public FeatureHistogramPlotViewBuilder(FeatureHistogramPlotModel model) {
    super(model);
  }

  @Override
  public Region build() {
    final HistogramPanel histo = new HistogramPanel();
    final BorderPane mainPane = new BorderPane(histo);
    mainPane.setTop(createDataTypeBox());
    model.datasetProperty().subscribe(data -> histo.setData(data, 0, true));

    model.selectedTypeProperty().subscribe(selectedType -> {
      histo.setDomainLabel(selectedType != null ? selectedType.getHeaderString() : "");
    });
    return mainPane;
  }

  @NotNull
  private Pane createDataTypeBox() {
    return newFlowPane(new Label("Data type: "),
        newSearchableComboBox("Feature data type to select data source", model.getTypeChoices(),
            model.selectedTypeProperty()));
  }

}
