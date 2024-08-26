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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.previewpane.AbstractPreviewPane;
import io.github.mzmine.parameters.dialogs.previewpane.FeaturePreviewPane;
import java.util.function.Function;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

public class ParameterDialogWithPreviewPanes extends ParameterSetupDialogWithPreview {

  private final ScrollPane scroll = new ScrollPane();
  private final VBox vbox = FxLayout.newVBox(Pos.TOP_CENTER, FxLayout.DEFAULT_PADDING_INSETS);
  @NotNull
  private final Function<ParameterSet, AbstractPreviewPane<?>> createNewPreview;

  public ParameterDialogWithPreviewPanes(boolean valueCheckRequired, ParameterSet parameters,
      Region message, @NotNull Function<ParameterSet, AbstractPreviewPane<?>> createNewPreview) {
    super(valueCheckRequired, parameters, message);
    this.createNewPreview = createNewPreview;
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(true);
    scroll.setContent(vbox);
    vbox.setFillWidth(true);
    previewWrapperPane.setBottom(
        FxButtons.createButton("Add preview", FxIcons.PLUS, "Add another preview",
            () -> vbox.getChildren().add(createNewPreview.apply(parameters))));
    previewWrapperPane.setCenter(scroll);
    vbox.getChildren().add(createNewPreview.apply(parameters));
  }

  public ParameterDialogWithPreviewPanes(boolean valueCheckRequired, ParameterSet parameters,
      @NotNull Function<ParameterSet, AbstractPreviewPane<?>> createNewPreview) {
    this(valueCheckRequired, parameters, null, createNewPreview);
  }

  @Override
  protected void parametersChanged() {
    super.parametersChanged();
    updatePreview();
  }

  /**
   * Called if the parameters or the selected feature change.
   */
  private void updatePreview() {
    updateParameterSetFromComponents();
    vbox.getChildren().stream().filter(n -> n instanceof FeaturePreviewPane)
        .map(n -> (FeaturePreviewPane) n).forEach(FeaturePreviewPane::updatePreview);
  }


}
