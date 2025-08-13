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

package io.github.mzmine.modules.dataanalysis.statsdashboard;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.framework.fx.FxControllerBinding;
import io.github.mzmine.gui.framework.fx.SelectedAbundanceMeasureBinding;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.gui.framework.fx.SelectedMetadataColumnBinding;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.dataanalysis.pca_new.PCAController;
import io.github.mzmine.modules.dataanalysis.rowsboxplot.RowsBoxplotController;
import io.github.mzmine.modules.dataanalysis.volcanoplot.VolcanoPlotController;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableController;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;

public class StatsDashboardController extends FxController<StatsDashboardModel> implements
    SelectedFeatureListsBinding, SelectedRowsBinding, SelectedAbundanceMeasureBinding,
    SelectedMetadataColumnBinding {

  private final PCAController pcaController = new PCAController();
  private final VolcanoPlotController volcanoController = new VolcanoPlotController(null);
  private final RowsBoxplotController boxplotController = new RowsBoxplotController();
  private final StatsDashboardViewBuilder builder;
  private final FxFeatureTableController tableController;

  public StatsDashboardController() {
    super(new StatsDashboardModel());
    tableController = new FxFeatureTableController();
    builder = new StatsDashboardViewBuilder(model, tableController, pcaController,
        volcanoController, boxplotController);

    FxControllerBinding.bindExposedProperties(this, volcanoController);
    FxControllerBinding.bindExposedProperties(this, boxplotController);
    FxControllerBinding.bindExposedProperties(this, pcaController);
    pcaController.waitAndUpdate();
    // feature table bindings in view builder
  }


  @Override
  protected @NotNull FxViewBuilder<StatsDashboardModel> getViewBuilder() {
    return builder;
  }


  @Override
  public ObjectProperty<AbundanceMeasure> abundanceMeasureProperty() {
    return model.abundanceProperty();
  }

  @Override
  public Property<List<FeatureList>> selectedFeatureListsProperty() {
    return model.flistsProperty();
  }

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }

  @Override
  public ObjectProperty<MetadataColumn<?>> groupingColumnProperty() {
    return model.metadataColumnProperty();
  }
}
