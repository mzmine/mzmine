/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.dash_lipidqc;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickOutlierPopupController;
import io.github.mzmine.modules.visualization.dash_lipidqc.quality.AnnotationQualityController;
import io.github.mzmine.util.FeatureTableFXUtil;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MVCI controller for the lipid annotation quality control dashboard. Wires the
 * {@link LipidAnnotationQCDashboardModel} to the view and forwards feature-list selection changes
 * to the model.
 */
public class LipidAnnotationQCDashboardController extends
    FxController<LipidAnnotationQCDashboardModel> {

  private final AnnotationQualityController qualityController = new AnnotationQualityController();
  private final KendrickOutlierPopupController outlierPopupController = new KendrickOutlierPopupController();

  public LipidAnnotationQCDashboardController() {
    super(new LipidAnnotationQCDashboardModel());
    new LipidAnnotationQCDashboardInteractor(model);
    model.featureListProperty().subscribe(flist -> {
      model.getFeatureTableFx().setFeatureList(flist);
      model.getPaneGroup().featureTableFXProperty().set(model.getFeatureTableFx());
    });
    // Bidirectional bindings keep both models in sync in both directions
    model.featureListProperty().bindBidirectional(qualityController.featureListProperty());
    model.rowProperty().bindBidirectional(qualityController.rowProperty());
    model.retentionTimeAnalysisEnabledProperty()
        .bindBidirectional(qualityController.retentionTimeAnalysisEnabledProperty());
    // Callbacks for side effects that cannot be expressed as property bindings
    qualityController.setOnReselectRow(row -> {
      if (row != null) {
        FeatureTableFXUtil.selectAndScrollTo(row, model.getFeatureTableFx());
      }
    });
    qualityController.setOnFeatureTableRefresh(() -> model.getFeatureTableFx().refresh());
  }

  @Override
  protected @NotNull FxViewBuilder<LipidAnnotationQCDashboardModel> getViewBuilder() {
    final Region qualityView = qualityController.buildView();
    return new LipidAnnotationQCDashboardViewBuilder(model, qualityView, outlierPopupController,
        qualityController::setKendrickReviewMode, qualityController::setOnAnnotationsChanged,
        qualityController::requestUpdate);
  }

  public void setFeatureList(@Nullable ModularFeatureList flist) {
    if (flist == null) {
      return;
    }
    model.setFeatureList(flist);
  }

  public void dispose() {
    model.getPaneGroup().disposeListeners();
    outlierPopupController.closeStage();
  }
}
