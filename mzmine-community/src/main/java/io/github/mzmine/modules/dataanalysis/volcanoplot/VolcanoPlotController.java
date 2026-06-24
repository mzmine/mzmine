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

package io.github.mzmine.modules.dataanalysis.volcanoplot;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowSelection;
import io.github.mzmine.gui.framework.fx.SelectedCompoundRowSelectionBinding;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.statistics.AbundanceDataTablePreparationConfig;
import java.awt.geom.Point2D;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VolcanoPlotController extends FxController<VolcanoPlotModel> implements
    SelectedRowsBinding, SelectedFeatureListsBinding, SelectedCompoundRowSelectionBinding {

  private final VolcanoPlotViewBuilder viewBuilder;
  private final Region view;

  public VolcanoPlotController() {
    this(null);
  }

  public VolcanoPlotController(@Nullable FeatureList flist) {
    super(new VolcanoPlotModel());

    model.setFlists(flist != null ? List.of(flist) : null);
    viewBuilder = new VolcanoPlotViewBuilder(model, this::onExtractRegionsPressed);
    view = viewBuilder.build();

    initializeListeners();
  }

  private void onExtractRegionsPressed(List<List<Point2D>> regions) {
    VolcanoPlotRegionExtractionParameters parameters = VolcanoPlotRegionExtractionParameters.create(
        model, regions);
    MZmineCore.runMZmineModule(VolcanoPlotRegionExtractionModule.class, parameters);
  }

  private void initializeListeners() {
    PropertyUtils.onChange(this::prepareDataTable, model.flistsProperty(),
        model.abundanceMeasureProperty(), model.missingValueImputationProperty(),
        model.compoundRowSelectionProperty());

    // either the data table changes or the test or other requirements
    PropertyUtils.onChange(this::computeDataset, model.featureDataTableProperty(),
        model.testProperty(), model.pValueProperty());
  }

  /**
   * Prepares the data, sets it to a property and will then trigger the computeDataset via other
   * listener
   */
  private void prepareDataTable() {
    // wait and prepare dataset
    final List<FeatureList> flists = model.getFlists();
    if (flists == null || flists.isEmpty()) {
      model.featureDataTableProperty().set(null);
      return;
    }
    // create the new dataset
    final var config = new AbundanceDataTablePreparationConfig(model.getAbundanceMeasure(),
        model.getMissingValueImputation());

    // decision: use compound rows when selection is set and compound list is available
    final FeatureList featureList = flists.getFirst();
    final CompoundRowSelection selection = model.getCompoundRowSelection();
    final ObservableList<FeatureListRow> rows;
    if (selection != null && featureList.hasCompoundList()) {
      rows = FXCollections.observableArrayList(
          featureList.getCompoundList().getRowsCopy(selection));
    } else {
      rows = featureList.getRows();
    }

    onTaskThreadDelayed(
        new FeatureDataPreparationTask(model.featureDataTableProperty(), rows,
            featureList.getRawDataFiles(), config));
  }

  private void computeDataset() {
    // wait and update
    onTaskThreadDelayed(new VolcanoPlotUpdateTask(model));
  }

  @Override
  public ObjectProperty<List<FeatureList>> selectedFeatureListsProperty() {
    return model.flistsProperty();
  }

  public Region getView() {
    return view;
  }

  @Override
  protected @NotNull FxViewBuilder<VolcanoPlotModel> getViewBuilder() {
    return viewBuilder;
  }

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }

  @Override
  public ObjectProperty<@Nullable CompoundRowSelection> compoundRowSelectionProperty() {
    return model.compoundRowSelectionProperty();
  }
}
