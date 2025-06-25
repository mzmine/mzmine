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
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.util.FeatureUtils;
import java.io.File;
import java.net.MalformedURLException;
import java.util.logging.Logger;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import org.jetbrains.annotations.NotNull;

public class ExternalRowHtmlVisualizerViewBuilder extends
    FxViewBuilder<ExternalRowHtmlVisualizerModel> {

  private static final Logger logger = Logger.getLogger(
      ExternalRowHtmlVisualizerViewBuilder.class.getName());

  public ExternalRowHtmlVisualizerViewBuilder(final @NotNull ExternalRowHtmlVisualizerModel model) {
    super(model);
  }

  @Override
  public Region build() {
    var web = new WebView();
    model.selectedFullHtmlProperty().subscribe(file -> {
      try {
        if (file != null) {
          logger.finest("Trying to open external file: " + file.getAbsolutePath());
          web.getEngine().load(file.toURI().toURL().toExternalForm());
          web.setVisible(true);
        } else {
          web.setVisible(false);
        }
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    });
    var main = FxLayout.newBorderPane(web);
    main.setTop(createTopMenu(main));

    // there may be multiple
    return main;
  }

  private @NotNull FlowPane createTopMenu(BorderPane main) {
    final String tooltip = """
        Select a directory that contains external HTML files with the following naming pattern which points to a specific feature row ID and resource:
        %s""".formatted(FeatureUtils.rowToFullIdDescription());

    return FxLayout.newFlowPane(
        FxTextFields.newTextField(30, model.externalFolderProperty(), tooltip),
        FxButtons.createButton("Select directory", tooltip, () -> {
          final DirectoryChooser chooser = new DirectoryChooser();
          chooser.setTitle("Select external directory with HTML resources");
          final File externalFolder = model.getExternalFolderAsFile();
          if (externalFolder != null) {
            chooser.setInitialDirectory(externalFolder);
          }
          var file = chooser.showDialog(main.getScene().getWindow());
          if (file != null) {
            model.setExternalFolder(file.getAbsolutePath());
          }
        }),
        // might have multiple html resources for each feature
        FxComboBox.newSearchableComboBox(
            "Select the external HTML resource for the selected feature. Depends on the correct folder.",
            model.getHtmlChoices(), model.selectedHtmlProperty()));
  }
}
