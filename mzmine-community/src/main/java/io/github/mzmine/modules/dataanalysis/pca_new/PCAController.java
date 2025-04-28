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

package io.github.mzmine.modules.dataanalysis.pca_new;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.framework.fx.SelectedAbundanceMeasureBinding;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.gui.framework.fx.SelectedMetadataColumnBinding;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.awt.geom.Point2D;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;

public class PCAController extends FxController<PCAModel> implements SelectedRowsBinding,
    SelectedFeatureListsBinding, SelectedMetadataColumnBinding, SelectedAbundanceMeasureBinding {

  private final FxViewBuilder<PCAModel> builder;

  public PCAController() {
    super(new PCAModel());
    builder = new PCAViewBuilder(model, this::onExtractRegionsPressed);
    //update on changes of these properties
    PropertyUtils.onChange(this::waitAndUpdate, model.flistsProperty(), model.domainPcProperty(),
        model.rangePcProperty(), model.abundanceProperty(), model.metadataColumnProperty(),
        model.scalingFunctionProperty(), model.imputationFunctionProperty(),
        model.sampleTypeFilterProperty());
  }

  private void onExtractRegionsPressed(List<List<Point2D>> regions) {
    final var param = PCALoadingsExtractionParameters.fromPcaModel(this.model, regions);
    MZmineCore.runMZmineModule(PCALoadingsExtractionModule.class, param);
  }

  @Override
  protected @NotNull FxViewBuilder<PCAModel> getViewBuilder() {
    return builder;
  }

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }

  /**
   * Accumulates update calls by waiting for some time
   */
  public void waitAndUpdate() {
    onTaskThreadDelayed(new PCAUpdateTask("update full dataset", model));
  }

  @Override
  public Property<List<FeatureList>> selectedFeatureListsProperty() {
    return model.flistsProperty();
  }

  @Override
  public ObjectProperty<MetadataColumn<?>> groupingColumnProperty() {
    return model.metadataColumnProperty();
  }

  @Override
  public ObjectProperty<AbundanceMeasure> abundanceMeasureProperty() {
    return model.abundanceProperty();
  }
}
