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

package io.github.mzmine.modules.dataanalysis.volcanoplot;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.framework.fx.mvci.FxController;
import io.github.mzmine.gui.framework.fx.mvci.FxInteractor;
import io.github.mzmine.gui.framework.fx.mvci.FxViewBuilder;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VolcanoPlotController extends FxController<VolcanoPlotModel> {

  private final VolcanoPlotInteractor interactor;
  private final VolcanoPlotViewBuilder viewBuilder;
  private final Region view;

  public VolcanoPlotController(List<FeatureList> flists) {
    super(new VolcanoPlotModel());
    final List<FeatureList> alignedLists = flists.stream()
        .filter(flist -> flist.getNumberOfRawDataFiles() > 1).toList();

    model.setFlists(FXCollections.observableArrayList(alignedLists));
    viewBuilder = new VolcanoPlotViewBuilder(model);
    interactor = new VolcanoPlotInteractor(model);
    view = viewBuilder.build();

    initializeListeners();
  }

  private void initializeListeners() {
    model.testProperty().addListener((_, _, newValue) -> {
      if (newValue != null) {
        computeDataset();
      }
    });
    model.flistsProperty().addListener(_ -> computeDataset());
    model.abundanceMeasureProperty().addListener(_ -> computeDataset());
    model.selectedFlistProperty().addListener(_ -> computeDataset());
    model.pValueProperty().addListener(_ -> computeDataset());
  }

  private void computeDataset() {
    onTaskThread("compute dataset", interactor::computeDataset, interactor::updateModel);
  }

  public void setFeatureList(FeatureList flist) {
    model.setSelectedFlist(flist);
  }

  public ObjectProperty<List<FeatureList>> featureListsProperty() {
    return model.flistsProperty();
  }

  public Region getView() {
    return view;
  }

  @Override
  protected @Nullable FxInteractor<VolcanoPlotModel> getInteractor() {
    return interactor;
  }

  @Override
  protected @NotNull FxViewBuilder<VolcanoPlotModel> getViewBuilder() {
    return viewBuilder;
  }
}
