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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.mvci.FxInteractor;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableFilterMenu.FxFeatureTableFilterMenuModel;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.RangeUtils;
import java.text.DecimalFormat;
import org.jetbrains.annotations.Nullable;

public class FxFeatureTableInteractor extends FxInteractor<FxFeatureTableModel> {


  protected FxFeatureTableInteractor(FxFeatureTableModel model) {
    super(model);

    initActionEvents();
    initListeners();
  }

  private void initActionEvents() {
    model.setOnOpenParameterDialogAction(this::openParametersDialog);
    model.setOnQuickColumnSelectionAction(this::openQuickColumnSelection);
  }

  private void openQuickColumnSelection() {
    model.getFeatureTable().showQuickColumnSelectionContextMenu();
  }

  private void initListeners() {
    PropertyUtils.onChange(this::updateFilterPrompts, model.rowsMzRangeProperty(),
        model.rowsRetentionTimeRangeProperty());

    model.getFilterModel().combinedRowFilterProperty().subscribe(this::applyRowsFilter);
  }

  private void applyRowsFilter(@Nullable TableFeatureListRowFilter filter) {
    model.getFilteredRowItems()
        .setPredicate(item -> filter == null || filter.test(item.getValue()));
  }

  private void updateFilterPrompts() {
    final FxFeatureTableFilterMenuModel filterModel = model.getFilterModel();
    if (filterModel == null) {
      return;
    }
    if (model.getRowsMzRange() == null || model.getRowsRetentionTimeRange() == null) {
      filterModel.mzFilterPromptProperty().set(null);
      filterModel.rtFilterPromptProperty().set(null);
      return;
    }

    // reduced accuracy to not overflow size of textfield
    DecimalFormat format = new DecimalFormat("0.0");

    filterModel.mzFilterPromptProperty().set(RangeUtils.toString(model.getRowsMzRange(), format));
    filterModel.rtFilterPromptProperty()
        .set(RangeUtils.toString(model.getRowsRetentionTimeRange(), format));
  }


  public void openParametersDialog() {
    final ParameterSet param = model.getParameters();
    if (param == null) {
      return;
    }
    FxThread.runLater(() -> {
      ExitCode exitCode = param.showSetupDialog(true);
      if (exitCode == ExitCode.OK) {
        updateWindowToParameterSetValues();
        // set to module
        ConfigService.getConfiguration()
            .setModuleParameters(FeatureTableFXModule.class, param.cloneParameterSet());
      }
    });
  }

  /**
   * In case the parameters are changed in the setup dialog, they are applied to the window.
   */
  public void updateWindowToParameterSetValues() {
    final FeatureTableFX table = model.getFeatureTable();
    final ParameterSet param = model.getParameters();
    if (table == null || param == null) {
      return;
    }
    model.getFeatureTable().updateColumnsVisibilityParameters(
        param.getParameter(FeatureTableFXParameters.showRowTypeColumns).getValue(),
        param.getParameter(FeatureTableFXParameters.showFeatureTypeColumns).getValue());
  }

  @Override
  public void updateModel() {

  }
}
