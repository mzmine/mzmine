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
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.FragmentUtils;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.SignalWithFormulae;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.FragmentGraphGenerator;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SignalFormulaeModel;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SubFormulaEdge;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.FormulaWithExactMz;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.Comparator;
import java.util.List;
import org.graphstream.graph.implementations.MultiGraph;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaChangedUpdateTask extends FxUpdateTask<FragmentGraphModel> {

  private final SpectralSignalFilter signalFilter;

  private final MZTolerance formulaTolerance;
  private MultiGraph graph;
  private List<SignalFormulaeModel> allNodeModels;
  private List<SubFormulaEdge> edges;

  public FormulaChangedUpdateTask(@NotNull String taskName, FragmentGraphModel model,
      MZTolerance fragmentFormulaTolerance, SpectralSignalFilter signalFilter) {
    super(taskName, model);
    formulaTolerance = fragmentFormulaTolerance;
    this.signalFilter = signalFilter;
  }

  @Override
  public boolean checkPreConditions() {
    return model.getPrecursorFormula() != null && model.getMs2Spectrum() != null
        && model.getMs2Spectrum().getNumberOfDataPoints() > 1;
  }

  @Override
  protected void process() {
    final IMolecularFormula newFormula = model.getPrecursorFormula();
    final MassSpectrum ms2 = model.getMs2Spectrum();
    final List<SignalWithFormulae> peaksWithFormulae = FragmentUtils.getPeaksWithFormulae(
        newFormula, ms2, signalFilter, formulaTolerance);

    // make sure we have a root signal
    SignalWithFormulae root = generateRootSignal(newFormula, ms2);
    final FragmentGraphGenerator graphGenerator = new FragmentGraphGenerator(
        "Fragment graph for " + MolecularFormulaManipulator.getString(newFormula),
        peaksWithFormulae, root);
    graph = graphGenerator.getGraph();

    allNodeModels = graphGenerator.getNodeModelMap().values().stream()
        .sorted(Comparator.comparingDouble(sfm -> sfm.getPeakWithFormulae().peak().getMZ()))
        .toList();
    edges = graphGenerator.getEdges().stream()
        .sorted(Comparator.comparingDouble(e -> e.smaller().getPeakWithFormulae().peak().getMZ()))
        .toList();
  }

  @NotNull
  private SignalWithFormulae generateRootSignal(IMolecularFormula newFormula, MassSpectrum ms2) {
    SignalWithFormulae root;
    final double searchMz = model.getMeasuredPrecursorMz() != null ? model.getMeasuredPrecursorMz()
        : FormulaUtils.calculateMzRatio(newFormula);
    final int precursorSignalIndex = ms2.binarySearch(searchMz, DefaultTo.CLOSEST_VALUE);
    if (formulaTolerance.checkWithinTolerance(searchMz, ms2.getMzValue(precursorSignalIndex))) {
      root = new SignalWithFormulae(new SimpleDataPoint(ms2.getMzValue(precursorSignalIndex),
          ms2.getIntensityValue(precursorSignalIndex)),
          List.of(new FormulaWithExactMz(newFormula, searchMz)));
    } else {
      root = new SignalWithFormulae(new SimpleDataPoint(searchMz, 0),
          List.of(new FormulaWithExactMz(newFormula, searchMz)));
    }
    return root;
  }

  @Override
  protected void updateGuiModel() {
    model.getSelectedEdges().clear();
    model.getSelectedNodes().clear();
    model.allNodesProperty().setAll(allNodeModels);
    model.allEdgesProperty().setAll(edges);
    model.setGraph(graph);
  }

  @Override
  public String getTaskDescription() {
    return "Calculating fragment graph for " + (model.getPrecursorFormula() != null
        ? MolecularFormulaManipulator.getString(model.getPrecursorFormula()) : "");
  }

  @Override
  public double getFinishedPercentage() {
    return 0.5;
  }
}
