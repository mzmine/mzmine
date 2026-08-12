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

package io.github.mzmine.modules.visualization.dash_lipidqc.kendrick;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.MZmineCore;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MVCI controller for the Kendrick outlier review popup. Implements {@link SelectedRowsBinding} so
 * the dashboard can bind the popup's row selection to its own selection state. The popup stage is
 * shown when the Kendrick review mode changes from {@link KendrickReviewMode#NONE} to a review mode
 * and hidden when it returns to {@link KendrickReviewMode#NONE}. The stage is always on top but
 * non-modal so it does not block interaction with the main window.
 */
public class KendrickOutlierPopupController extends
    FxController<KendrickOutlierPopupModel> implements SelectedRowsBinding {

  private @Nullable Stage stage;

  public KendrickOutlierPopupController() {
    super(new KendrickOutlierPopupModel());
  }

  @Override
  public @NotNull ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }

  /**
   * Updates the list of outlier rows displayed in the popup. Safe to call from any thread.
   */
  public void setOutlierRows(final @NotNull List<FeatureListRow> rows) {
    onGuiThread(() -> model.getOutlierRows().setAll(rows));
  }

  /**
   * Updates the current review mode, which is reflected in the popup title.
   */
  public void setReviewMode(final @NotNull KendrickReviewMode mode) {
    onGuiThread(() -> model.setReviewMode(mode));
  }

  /**
   * Shows the outlier review popup stage. Creates it lazily on first call. The stage is always on
   * top and non-modal so the user can still interact with the main window. Safe to call from any
   * thread.
   */
  public void showStage() {
    onGuiThread(() -> {
      if (stage == null) {
        stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.setAlwaysOnTop(true);
        final Scene scene = new Scene(buildView());
        final Window mainWindow = MZmineCore.getDesktop().getMainWindow();
        if (mainWindow != null) {
          scene.getStylesheets().addAll(mainWindow.getScene().getStylesheets());
        }
        stage.setScene(scene);
        // assumption: title stays in sync with the review mode via binding
        stage.titleProperty().bind(model.reviewModeProperty().map(KendrickReviewMode::toString));
      }
      if (!stage.isShowing()) {
        stage.show();
      }
      stage.toFront();
    });
  }

  /**
   * Hides the popup stage without destroying it. Safe to call from any thread.
   */
  public void hideStage() {
    onGuiThread(() -> {
      if (stage != null && stage.isShowing()) {
        stage.hide();
      }
    });
  }

  /**
   * Closes and disposes the popup stage. Called when the owning dashboard tab is closed. Safe to
   * call from any thread.
   */
  public void closeStage() {
    onGuiThread(() -> {
      if (stage != null) {
        stage.close();
        stage = null;
      }
    });
  }

  @Override
  protected @NotNull FxViewBuilder<KendrickOutlierPopupModel> getViewBuilder() {
    return new KendrickOutlierPopupViewBuilder(model);
  }
}
