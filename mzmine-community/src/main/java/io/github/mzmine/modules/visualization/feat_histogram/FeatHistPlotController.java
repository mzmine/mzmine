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

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatHistPlotController extends FxController<FeatHistPlotModel> implements
    SelectedRowsBinding, SelectedFeatureListsBinding {

  private final FeatHistPlotViewBuilder viewBuilder;
  private final Region view;

  public FeatHistPlotController() {
    this(null);
  }

  public FeatHistPlotController(@Nullable FeatureList flist) {
    super(new FeatHistPlotModel());

    model.setFlists(flist != null ? List.of(flist) : null);
    model.getTypeChoices().setAll(flist.getFeatureTypes().stream()
        .filter(NumberType.class::isInstance).map(NumberType.class::cast).toList());
    viewBuilder = new FeatHistPlotViewBuilder(model);
    view = viewBuilder.build();

    initializeListeners();
  }

  private void initializeListeners() {
//    PropertyUtils.onChange(this::computeDataset, model.testProperty(), model.flistsProperty(),
//        model.abundanceMeasureProperty(), model.pValueProperty());
    PropertyUtils.onChange(this::computeDataset, model.dataTypeProperty(), model.flistsProperty());
    model.flistsProperty().subscribe((featureLists) -> {
      if (featureLists == null) {
        //todo remove all choices from model
        return;
      }
      List<NumberType> types = featureLists.stream()
          .flatMap(featureList -> featureList.getFeatureTypes().stream())
          .filter(NumberType.class::isInstance).map(NumberType.class::cast).toList();
      model.getTypeChoices().setAll(types);
    });
  }

  private void computeDataset() {
    // wait and update
    onTaskThreadDelayed(new FeatHistPlotUpdateTask(model, model.getDataType()));
  }

  @Override
  public ObjectProperty<List<FeatureList>> selectedFeatureListsProperty() {
    return model.flistsProperty();
  }

  public Region getView() {
    return view;
  }

  @Override
  protected @NotNull FxViewBuilder<FeatHistPlotModel> getViewBuilder() {
    return viewBuilder;
  }

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }
}
