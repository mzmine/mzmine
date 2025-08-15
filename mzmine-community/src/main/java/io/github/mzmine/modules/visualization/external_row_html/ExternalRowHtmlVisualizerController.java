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

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.framework.fx.FeatureRowInterfaceFx;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.util.StringUtils;
import java.io.File;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.NotNull;

public class ExternalRowHtmlVisualizerController extends
    FxController<ExternalRowHtmlVisualizerModel> implements FeatureRowInterfaceFx,
    SelectedRowsBinding {

  private static final Logger logger = Logger.getLogger(
      ExternalRowHtmlVisualizerController.class.getName());
  private final ExternalRowHtmlVisualizerViewBuilder viewBuilder;

  public ExternalRowHtmlVisualizerController() {
    super(new ExternalRowHtmlVisualizerModel());
    viewBuilder = new ExternalRowHtmlVisualizerViewBuilder(model);

    PropertyUtils.onChange(this::updateSelection, model.selectedRowFullIdProperty(),
        model.externalFolderProperty());
    PropertyUtils.onChange(this::bindFullHtmlPath, model.selectedRowFullIdProperty(),
        model.externalFolderProperty(), model.selectedHtmlProperty());
  }

  private void bindFullHtmlPath() {
    final String folder = model.getExternalFolder();
    final String rowid = model.selectedRowFullIdProperty().getValue();
    final String selected = model.getSelectedHtml();
    if (StringUtils.hasValues(folder, rowid, selected)) {
      File full = Path.of(folder, rowid + selected).toFile();
      model.selectedFullHtmlProperty().setValue(full);
    } else {
      model.selectedFullHtmlProperty().setValue(null);
    }
  }

  private void updateSelection() {
    final String folder = model.getExternalFolder();
    final String rowid = model.selectedRowFullIdProperty().getValue();
    String selected = model.getSelectedHtml();

    List<String> choices = new ArrayList<>();
    if (StringUtils.hasValue(folder) && StringUtils.hasValue(rowid)) {
      try (Stream<Path> paths = Files.walk(Path.of(folder), 1, FileVisitOption.FOLLOW_LINKS)) {
        final List<String> externalResourcesSimple = paths.filter(Files::isRegularFile)
            .map(p -> p.getFileName().toString()).filter(name -> {
              final String lowerName = name.toLowerCase();
              return lowerName.startsWith(rowid) && lowerName.matches(".*\\.(html|htm)");
            })
            // remove row ID prefix for choices box
            .map(name -> name.substring(rowid.length())).toList();
        choices.addAll(externalResourcesSimple);
      } catch (Exception e) {
        logger.log(Level.WARNING, e.getMessage(), e);
      }
    }

    model.getHtmlChoices().setAll(choices);
    // set different selected if not in choices
    if (StringUtils.isBlank(selected) || !choices.contains(selected)) {
      selected = choices.isEmpty() ? null : choices.getFirst();
      model.selectedHtmlProperty().set(selected);
    }
  }

  @Override
  protected @NotNull FxViewBuilder<ExternalRowHtmlVisualizerModel> getViewBuilder() {
    return viewBuilder;
  }

  @Override
  public boolean hasContent() {
    return true;
  }

  @Override
  public void setFeatureRows(final @NotNull List<? extends FeatureListRow> selectedRows) {
    model.setSelectedRows(selectedRows);
  }

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }
}
