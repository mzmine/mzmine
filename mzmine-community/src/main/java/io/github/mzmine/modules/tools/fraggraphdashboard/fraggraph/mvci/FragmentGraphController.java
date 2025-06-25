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

package io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.mvci;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.tools.fraggraphdashboard.FragmentGraphCalcParameters;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SignalFormulaeModel;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SubFormulaEdge;
import io.github.mzmine.parameters.ParameterSet;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyMapProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class FragmentGraphController extends FxController<FragmentGraphModel> {

  private final FxViewBuilder<FragmentGraphModel> builder;

  private final ParameterSet parameters;

  public FragmentGraphController(ParameterSet parameters) {
    super(new FragmentGraphModel());
    this.parameters = parameters;
    builder = new FragmentGraphBuilder(model);

    model.ms2SpectrumProperty().addListener((_, _, _) -> calculateNewGraph());
    model.precursorFormulaProperty().addListener((_, _, _) -> calculateNewGraph());
  }

  @Override
  protected @NotNull FxViewBuilder<FragmentGraphModel> getViewBuilder() {
    return builder;
  }

  public void update(MassSpectrum spectrum, IMolecularFormula precursorFormula) {
    model.setMs2Spectrum(spectrum);
    model.setPrecursorFormula(precursorFormula);
  }

  private void calculateNewGraph() {
    onTaskThreadDelayed(new FormulaChangedUpdateTask("Calculate new fragment graph", model,
        parameters.getValue(FragmentGraphCalcParameters.ms2Tolerance),
        parameters.getValue(FragmentGraphCalcParameters.ms2SignalFilter).createFilter()));
  }

  public ObjectProperty<IMolecularFormula> precursorFormulaProperty() {
    return model.precursorFormulaProperty();
  }

  public ObjectProperty<MassSpectrum> spectrumProperty() {
    return model.ms2SpectrumProperty();
  }

  public ReadOnlyMapProperty<String, SignalFormulaeModel> allNodesMapProperty() {
    return model.allNodesMapProperty();
  }

  public ReadOnlyMapProperty<String, SubFormulaEdge> allEdgesMapProperty() {
    return model.allEdgesMapProperty();
  }

  public ListProperty<SignalFormulaeModel> allNodesProperty() {
    return model.allNodesProperty();
  }

  public ListProperty<SubFormulaEdge> allEdgesProperty() {
    return model.allEdgesProperty();
  }

  public ListProperty<SignalFormulaeModel> selectedNodesProperty() {
    return model.selectedNodesProperty();
  }

  public ListProperty<SubFormulaEdge> selectedEdgesProperty() {
    return model.selectedEdgesProperty();
  }

  public ObjectProperty<@Nullable Double> measuredPrecursorMzProperty() {
    return model.measuredPrecursorMzProperty();
  }
}
