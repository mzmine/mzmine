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

package io.github.mzmine.modules.tools.id_fraggraph.graphstream;

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.MathUtils;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class SubFormulaEdge {

  private final SignalFormulaeModel a;
  private final SignalFormulaeModel b;
  private final String id;

  private final List<Graph> graphs = new ArrayList<>();
  private final ReadOnlyBooleanWrapper valid = new ReadOnlyBooleanWrapper(false);
  private final BooleanProperty visible = new SimpleBooleanProperty(true);
  private final ReadOnlyBooleanWrapper visibleAndValid = new ReadOnlyBooleanWrapper(false);
  private final ReadOnlyObjectWrapper<@Nullable IMolecularFormula> lossFormula = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyStringWrapper lossFormulaString = new ReadOnlyStringWrapper();
  private final ReadOnlyDoubleWrapper measuredMassDiff = new ReadOnlyDoubleWrapper(0);
  private final ReadOnlyDoubleWrapper computedMassDiff = new ReadOnlyDoubleWrapper(0);
  private final ReadOnlyDoubleWrapper massErrorAbs = new ReadOnlyDoubleWrapper(0);
  private final ReadOnlyDoubleWrapper massErrorPpm = new ReadOnlyDoubleWrapper(0);

  public SubFormulaEdge(SignalFormulaeModel a, SignalFormulaeModel b,
      NumberFormat nodeNameFormatter) {
    if (a.getPeakWithFormulae().peak().getMZ() < b.getPeakWithFormulae().peak().getMZ()) {
      this.a = a;
      this.b = b;
    } else {
      this.a = b;
      this.b = a;
    }

    id = STR."\{nodeNameFormatter.format(
        a.getPeakWithFormulae().peak().getMZ())}-\{nodeNameFormatter.format(
        b.getPeakWithFormulae().peak().getMZ())}";

//    if (FormulaUtils.isSubFormula(a.getSelectedFormulaWithMz(), b.getSelectedFormulaWithMz())) {
//      valid.set(true);
//      visible.set(true);
//    }

    valid.bind(Bindings.createBooleanBinding(
        () -> FormulaUtils.isSubFormula(a.getSelectedFormulaWithMz(), b.getSelectedFormulaWithMz()),
        a.selectedFormulaWithMzProperty(), b.selectedFormulaWithMzProperty()));
    visibleAndValid.bind(valid.and(visible));

    lossFormula.bind(
        Bindings.createObjectBinding(this::computeLossFormula, a.selectedFormulaWithMzProperty(),
            b.selectedFormulaWithMzProperty()));
    lossFormulaString.bind(Bindings.createStringBinding(
        () -> lossFormula.get() != null ? MolecularFormulaManipulator.getString(lossFormula.get())
            : null));
    measuredMassDiff.set(
        a.getPeakWithFormulae().peak().getMZ() - b.getPeakWithFormulae().peak().getMZ());
    computedMassDiff.bind(Bindings.createDoubleBinding(
        () -> a.getSelectedFormulaWithMz().mz() - b.getSelectedFormulaWithMz().mz(),
        a.selectedFormulaWithMzProperty(), b.selectedFormulaWithMzProperty()));
    massErrorAbs.bind(
        Bindings.createDoubleBinding(() -> measuredMassDiff.get() - computedMassDiff.get(),
            computedMassDiff, measuredMassDiff));
    massErrorPpm.bind(Bindings.createDoubleBinding(
        () -> MathUtils.getPpmDiff(computedMassDiff.get(), measuredMassDiff.get()),
        computedMassDiff, measuredMassDiff));

    PropertyUtils.onChange(this::applyToGraphs, visibleAndValid, lossFormulaString,
        measuredMassDiff, computedMassDiff, massErrorAbs, massErrorPpm);
  }

  /**
   * Applies this edge settings to the edge in a graph. This is updated automatically and must only
   * be applied manually when initially setting up the graph.
   */
  public void applyToGraphs() {
    FxThread.runLater(() -> {
      for (Graph graph : graphs) {
        applyToGraph(graph);
      }
    });
  }

  /**
   * Applies this edge settings to the edge in a graph. This is updated automatically and must only
   * be applied manually when initially setting up the graph.
   */
  public void applyToGraph(Graph graph) {
    final Edge edge = graph.getEdge(getId());
    if (edge == null) {
      return;
    }
    FragEdgeAttr.applyToEdge(this, edge);

    if (!isVisibleAndValid()) {
      edge.setAttribute("ui.hide");
    } else {
      edge.removeAttribute("ui.hide");
    }

    if (!isValid()) {
      edge.setAttribute("ui.label", "INVALID");
    }
  }

  public SignalFormulaeModel smaller() {
    return a;
  }

  public SignalFormulaeModel larger() {
    return b;
  }

  public @Nullable IMolecularFormula computeLossFormula() {
    if (!FormulaUtils.isSubFormula(a.getSelectedFormulaWithMz(), b.getSelectedFormulaWithMz())) {
      return null;
    }
    final IMolecularFormula formula = b.getSelectedFormulaWithMz().formula();
    final IMolecularFormula loss = FormulaUtils.subtractFormula(FormulaUtils.cloneFormula(formula),
        a.getSelectedFormulaWithMz().formula());
    return loss;
  }

  public boolean isValid() {
    return valid.get();
  }

  public ReadOnlyBooleanProperty validProperty() {
    return valid.getReadOnlyProperty();
  }

  public boolean isVisible() {
    return visible.get();
  }

  public BooleanProperty visibleProperty() {
    return visible;
  }

  public boolean isVisibleAndValid() {
    return visibleAndValid.get();
  }

  public ReadOnlyBooleanProperty visibleAndValidProperty() {
    return visibleAndValid.getReadOnlyProperty();
  }

  public void setVisible(boolean visible) {
    this.visible.set(visible);
  }

  public String getLossFormulaString() {
    return lossFormulaString.get();
  }

  public ReadOnlyStringProperty lossFormulaStringProperty() {
    return lossFormulaString.getReadOnlyProperty();
  }

  public double getMeasuredMassDiff() {
    return measuredMassDiff.get();
  }

  public ReadOnlyDoubleProperty measuredMassDiffProperty() {
    return measuredMassDiff.getReadOnlyProperty();
  }

  public double getComputedMassDiff() {
    return computedMassDiff.get();
  }

  public ReadOnlyDoubleProperty computedMassDiffProperty() {
    return computedMassDiff.getReadOnlyProperty();
  }

  public double getMassErrorAbs() {
    return massErrorAbs.get();
  }

  public ReadOnlyDoubleProperty massErrorAbsProperty() {
    return massErrorAbs.getReadOnlyProperty();
  }

  public double getMassErrorPpm() {
    return massErrorPpm.get();
  }

  public ReadOnlyDoubleProperty massErrorPpmProperty() {
    return massErrorPpm.getReadOnlyProperty();
  }

  public String getId() {
    return id;
  }

  public void addGraph(Graph graph, boolean applyProperties) {
    if (!graphs.contains(graph)) {
      graphs.add(graph);

      if (applyProperties) {
        applyToGraph(graph);
      }
    }
  }

  public void removeGraph(Graph graph) {
    graphs.remove(graph);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SubFormulaEdge that)) {
      return false;
    }

    if (!a.equals(that.a)) {
      return false;
    }
    return b.equals(that.b);
  }

  @Override
  public int hashCode() {
    int result = a.hashCode();
    result = 31 * result + b.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return STR."SubFormulaEdge{id='\{id}\{'\''}\{'}'}";
  }
}
