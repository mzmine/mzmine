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

package io.github.mzmine.modules.tools.fraggraphdashboard;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable.SpectrumPlotTableController;
import io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable.SpectrumPlotTableViewBuilder.Layout;
import io.github.mzmine.modules.tools.id_fraggraph.FragmentGraphCalcModule;
import io.github.mzmine.modules.tools.id_fraggraph.mvci.FragmentGraphController;
import io.github.mzmine.parameters.ParameterSet;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

public class FragDashboardController extends FxController<FragDashboardModel> {

  private final FragDashboardBuilder fragDashboardBuilder;
  private final FragmentGraphController fragmentGraphController;

  private final ParameterSet parameters;

  public FragDashboardController() {
    this(ConfigService.getConfiguration().getModuleParameters(FragmentGraphCalcModule.class));
  }

  public FragDashboardController(ParameterSet parameters) {
    super(new FragDashboardModel());
    this.parameters = parameters;
    fragmentGraphController = new FragmentGraphController(parameters);

//    model.precursorFormulaProperty()
//        .bindBidirectional(fragmentGraphController.precursorFormulaProperty());
//    model.spectrumProperty().bindBidirectional(fragmentGraphController.spectrumProperty());
    model.allEdgesProperty().bindContentBidirectional(fragmentGraphController.allEdgesProperty());
    model.allNodesProperty().bindContentBidirectional(fragmentGraphController.allNodesProperty());
    model.selectedEdgesProperty()
        .bindContentBidirectional(fragmentGraphController.selectedEdgesProperty());
    model.selectedNodesProperty()
        .bindContentBidirectional(fragmentGraphController.selectedNodesProperty());

    SpectrumPlotTableController ms2Controller = new SpectrumPlotTableController(Layout.HORIZONTAL);
    SpectrumPlotTableController isotopeController = new SpectrumPlotTableController(
        Layout.HORIZONTAL);

    model.spectrumProperty().bindBidirectional(ms2Controller.spectrumProperty());
    model.isotopePatternProperty().bindBidirectional(isotopeController.spectrumProperty());

    fragDashboardBuilder = new FragDashboardBuilder(model, fragmentGraphController.buildView(),
        ms2Controller.buildView(), isotopeController.buildView(), this::updateFragmentGraph,
        this::startFormulaCalculation, parameters);
  }

  @Override
  protected @NotNull FxViewBuilder<FragDashboardModel> getViewBuilder() {
    return fragDashboardBuilder;
  }

  public void updateFragmentGraph() {
    fragmentGraphController.precursorFormulaProperty().set(model.getPrecursorFormula());
    fragmentGraphController.spectrumProperty().set(model.getSpectrum());
  }

  public void startFormulaCalculation() {
    onTaskThreadDelayed(new FragGraphPrecursorFormulaTask(model, parameters), new Duration(200));
  }

  public void setInput(double precursorMz, @NotNull MassSpectrum ms2Spectrum,
      @NotNull MassSpectrum isotopePattern) {
    model.setPrecursorMz(precursorMz);
    model.setSpectrum(ms2Spectrum);
    model.setIsotopePattern(isotopePattern);
  }
}
