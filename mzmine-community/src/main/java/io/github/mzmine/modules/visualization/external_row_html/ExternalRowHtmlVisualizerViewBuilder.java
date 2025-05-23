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

package io.github.mzmine.modules.visualization.external_row_html;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.io.File;
import java.net.MalformedURLException;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.jetbrains.annotations.NotNull;

public class ExternalRowHtmlVisualizerViewBuilder extends
    FxViewBuilder<ExternalRowHtmlVisualizerModel> {

  public ExternalRowHtmlVisualizerViewBuilder(final @NotNull ExternalRowHtmlVisualizerModel model) {
    super(model);
  }

  @Override
  public Region build() {
    var web = new WebView();
    model.masstFileProperty().subscribe(file -> {
      try {
        if (file != null) {
          web.getEngine().load(new File(file).toURI().toURL().toExternalForm());
        }
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    });
    var main = FxLayout.newBorderPane(web);
    main.setTop(FxLayout.newHBox(
        FxTextFields.newTextField(30, model.masstFileProperty(), "Local MASST file"),
        FxButtons.createButton("Select", "", () -> {
          var file = new FileChooser().showOpenDialog(main.getScene().getWindow());
          if (file != null) {
            model.setMasstFile(file.getAbsolutePath());
          }
        })));
    return main;
  }
}
