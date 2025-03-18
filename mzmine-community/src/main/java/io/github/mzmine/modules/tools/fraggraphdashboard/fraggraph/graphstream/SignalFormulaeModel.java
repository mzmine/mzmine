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

package io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream;

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.SignalWithFormulae;
import io.github.mzmine.util.FormulaWithExactMz;
import io.github.mzmine.util.GraphStreamUtils;
import io.github.mzmine.util.MathUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleObjectProperty;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

/**
 * Maps the results of the formula prediction to a node in a fragment graph. Contains all available
 * formulae and the selected formulae and its mz delta. Also contains a reference to the node to
 * update the node in the gui if the formula changes.
 * <p>
 * TODO: add a reference to the filtered node.
 */
public class SignalFormulaeModel {

  private static final Logger logger = Logger.getLogger(SignalFormulaeModel.class.getName());

  private final Node unfilteredNode;
  private final List<Graph> passThroughGraphs = new ArrayList<>();
  private final SignalWithFormulae signalWithFormulae;
  private final ObjectProperty<FormulaWithExactMz> selectedFormulaWithMz = new SimpleObjectProperty<>();
  private final ReadOnlyDoubleWrapper calculatedMz = new ReadOnlyDoubleWrapper(0);
  private final ReadOnlyDoubleWrapper deltaMzAbs = new ReadOnlyDoubleWrapper(0);
  private final ReadOnlyDoubleWrapper deltaMzPpm = new ReadOnlyDoubleWrapper(0);

  public SignalFormulaeModel(Node unfilteredNode, SignalWithFormulae formulae) {
    this.unfilteredNode = unfilteredNode;
    this.signalWithFormulae = formulae;
    final NumberFormats formats = ConfigService.getGuiFormats();

    deltaMzAbs.bind(Bindings.createDoubleBinding(
        () -> selectedFormulaWithMz.get() != null ? formulae.peak().getMZ()
            - selectedFormulaWithMz.get().mz() : 0d, selectedFormulaWithMz));

    deltaMzPpm.bind(Bindings.createDoubleBinding(
        () -> selectedFormulaWithMz.get() != null ? MathUtils.getPpmDiff(
            selectedFormulaWithMz.get().mz(), formulae.peak().getMZ()) : 0d,
        selectedFormulaWithMz));

    calculatedMz.bind(Bindings.createDoubleBinding(
        () -> selectedFormulaWithMz.get() != null ? selectedFormulaWithMz.get().mz() : 0d,
        selectedFormulaWithMz));

    selectedFormulaWithMz.addListener((_, _, n) -> {
      for (FragNodeAttr value : FragNodeAttr.values()) {
        value.setToNode(unfilteredNode, this);
      }

      unfilteredNode.setAttribute("ui.label", """
          %s
          %s
          %s, %s ppm
          """.formatted(unfilteredNode.getAttribute(FragNodeAttr.MZ.name()),
          unfilteredNode.getAttribute(FragNodeAttr.FORMULA.name()), formats.mz(deltaMzAbs.get()),
          formats.ppm(deltaMzPpm.get())));
    });

    final FormulaWithExactMz formulaWithSmallestMzError = formulae.formulae().stream().min(
        Comparator.comparingDouble(formulaWithExactMz -> Math.abs(
            formulaWithExactMz.mz() - signalWithFormulae.peak().getMZ()))).get();
    selectedFormulaWithMz.set(formulaWithSmallestMzError);

    // todo does this work every time? are the listeners above triggered before or after this binding?
    //  should be ok if it is added last?
    PropertyUtils.onChange(this::applyToPassThroughGraphs, deltaMzAbs, selectedFormulaWithMz,
        deltaMzPpm, calculatedMz);
  }

  private void applyToPassThroughGraphs() {
    final String nodeId = unfilteredNode.getId();

    passThroughGraphs.forEach(g -> {
      final Node node = g.getNode(nodeId);
      if (node == null) {
        return;
      }
      GraphStreamUtils.copyAttributes(unfilteredNode, node);
    });
  }

  public Node getUnfilteredNode() {
    return unfilteredNode;
  }

  public SignalWithFormulae getPeakWithFormulae() {
    return signalWithFormulae;
  }

  public FormulaWithExactMz getSelectedFormulaWithMz() {
    return selectedFormulaWithMz.get();
  }

  public void setSelectedFormulaWithMz(FormulaWithExactMz selectedFormulaWithMz) {
    this.selectedFormulaWithMz.set(selectedFormulaWithMz);
  }

  public ObjectProperty<FormulaWithExactMz> selectedFormulaWithMzProperty() {
    return selectedFormulaWithMz;
  }

  public double getDeltaMzAbs() {
    return deltaMzAbs.get();
  }

  public ReadOnlyDoubleProperty deltaMzAbsProperty() {
    return deltaMzAbs.getReadOnlyProperty();
  }

  @Override
  public boolean equals(Object o) {
    // todo: write test to ensure stability if some records get refactored to classes
    if (this == o) {
      return true;
    }
    if (!(o instanceof SignalFormulaeModel model)) {
      return false;
    }

    if (getUnfilteredNode() != null ? !getUnfilteredNode().equals(model.getUnfilteredNode())
        : model.getUnfilteredNode() != null) {
      return false;
    }
    if (!getPeakWithFormulae().equals(model.getPeakWithFormulae())) {
      return false;
    }
    return getSelectedFormulaWithMz().equals(model.getSelectedFormulaWithMz());
  }

  @Override
  public int hashCode() {
    int result = getUnfilteredNode() != null ? getUnfilteredNode().hashCode() : 0;
    result = 31 * result + getPeakWithFormulae().hashCode();
    result = 31 * result + getSelectedFormulaWithMz().hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "PeakFormulaeModel{node=%s, selectedFormulaWithMz=%s, deltaMz=%s}".formatted(
        unfilteredNode, selectedFormulaWithMz, deltaMzAbs);
  }

  public String getId() {
    return getUnfilteredNode().getId();
  }

  public void addPassThroughGraph(Graph g) {
    if (g == null) {
      return;
    }
    passThroughGraphs.add(g);
  }

  public void removePassThroughGraph(Graph g) {
    passThroughGraphs.remove(g);
  }

  public void clearPassThroughGraphs() {
    passThroughGraphs.clear();
  }

  public double getDeltaMzPpm() {
    return deltaMzPpm.get();
  }

  public ReadOnlyDoubleProperty deltaMzPpmProperty() {
    return deltaMzPpm.getReadOnlyProperty();
  }

  public double getCalculatedMz() {
    return calculatedMz.get();
  }

  public ReadOnlyDoubleProperty calculatedMzProperty() {
    return calculatedMz.getReadOnlyProperty();
  }

}
