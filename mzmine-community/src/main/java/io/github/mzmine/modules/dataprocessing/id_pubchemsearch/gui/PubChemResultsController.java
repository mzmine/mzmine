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

package io.github.mzmine.modules.dataprocessing.id_pubchemsearch.gui;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.preferences.Themes;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.gui.PubChemSearchTask.SearchType;
import io.github.mzmine.util.FeatureUtils;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

public class PubChemResultsController extends FxController<PubChemResultsModel> {

  private Stage stage = null;
  private final StringProperty title = new SimpleStringProperty("PubChem search");

  public PubChemResultsController(@NotNull FeatureListRow row, @NotNull IonType ionType,
      @NotNull String formula) {
    super(new PubChemResultsModel());
    model.selectedRowProperty().set(row);
    model.ionTypeProperty().set(ionType);
    model.formulaToSearchProperty().set(formula);
//    onFormulaSearchPressed();
    initTitle();
  }

  public PubChemResultsController(@NotNull FeatureListRow row, @NotNull IonType ionType,
      double neutralMass) {
    super(new PubChemResultsModel());
    model.selectedRowProperty().set(row);
    model.ionTypeProperty().set(ionType);
    model.massToSearchProperty().set(neutralMass);
//    onMassSearchPressed();
    initTitle();
  }

  private void initTitle() {
    PropertyUtils.onChange(() -> title.set("PubChem search for %s (%d results)".formatted(
            FeatureUtils.rowToString(model.getSelectedRow()), model.getCompounds().size())),
        model.selectedRowProperty(), model.compoundsProperty());
  }

  @Override
  protected @NotNull FxViewBuilder<PubChemResultsModel> getViewBuilder() {
    return new PubChemResultsViewBuilder(model, this::onAddAnnotationsPressed, this::onClosePressed,
        this::onFormulaSearchPressed, this::onMassSearchPressed);
  }

  private void onMassSearchPressed() {
    model.compoundsProperty().clear();
    super.onTaskThread(new PubChemSearchTask("Searching PubChem", model, SearchType.MASS));
  }

  private void onFormulaSearchPressed() {
    model.compoundsProperty().clear();
    super.onTaskThread(new PubChemSearchTask("Searching PubChem", model, SearchType.FORMULA));
  }

  private void onClosePressed() {
    if (stage != null) {
      stage.close();
      super.cancelTasks();
    }
  }

  private void onAddAnnotationsPressed(List<CompoundDBAnnotation> compoundData) {
    final FeatureListRow row = model.getSelectedRow();
    if (row == null) {
      return;
    }

    compoundData.forEach(row::addCompoundAnnotation);
  }

  public void showInWindow() {
    final Region region = buildView();
    final Stage stage = new Stage();
    final Scene scene = new Scene(region);
    final Themes theme = ConfigService.getConfiguration().getTheme();
    theme.apply(scene.getStylesheets());
    stage.setScene(scene);
    stage.initOwner(((MZmineGUI) DesktopService.getDesktop()).getMainWindow());
    stage.titleProperty().bind(title);
    stage.show();
    this.stage = stage;
  }
}
