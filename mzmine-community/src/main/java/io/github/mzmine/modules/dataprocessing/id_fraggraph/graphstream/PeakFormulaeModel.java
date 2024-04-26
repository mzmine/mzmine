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

package io.github.mzmine.modules.dataprocessing.id_fraggraph.graphstream;

import io.github.mzmine.modules.dataprocessing.id_fraggraph.PeakWithFormulae;
import io.github.mzmine.util.FormulaWithExactMz;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.graphstream.graph.Node;

/**
 * Maps the results of the formula prediction to a node in a fragment graph.
 */
public class PeakFormulaeModel {

  private final Node node;
  private final PeakWithFormulae peakWithFormulae;
  private final ObjectProperty<FormulaWithExactMz> selectedFormulaWithMz = new SimpleObjectProperty<>();
  private final DoubleProperty deltaMz = new SimpleDoubleProperty(0);

  public PeakFormulaeModel(Node node, PeakWithFormulae formulae) {
    this.node = node;
    this.peakWithFormulae = formulae;

    deltaMz.bind(Bindings.createDoubleBinding(
        () -> formulae.peak().getMZ() - selectedFormulaWithMz.get().mz(), selectedFormulaWithMz));

    selectedFormulaWithMz.set(formulae.formulae().getFirst());
    selectedFormulaWithMz.addListener((_, _, n) -> {
      for (FragNodeAttr value : FragNodeAttr.values()) {
        value.setToNode(node, formulae);
        value.setToNode(node, n);
      }

      node.setAttribute("ui.label",
          STR."\{node.getAttribute(FragNodeAttr.MZ.name())}\n\{node.getAttribute(
              FragNodeAttr.FORMULA.name())}");

      // todo: can we even trigger edge updates from here?
    });
  }

  public Node getNode() {
    return node;
  }

  public PeakWithFormulae getPeakWithFormulae() {
    return peakWithFormulae;
  }

  public FormulaWithExactMz getSelectedFormulaWithMz() {
    return selectedFormulaWithMz.get();
  }

  public ObjectProperty<FormulaWithExactMz> selectedFormulaWithMzProperty() {
    return selectedFormulaWithMz;
  }

  public void setSelectedFormulaWithMz(FormulaWithExactMz selectedFormulaWithMz) {
    this.selectedFormulaWithMz.set(selectedFormulaWithMz);
  }

  public double getDeltaMz() {
    return deltaMz.get();
  }

  public DoubleProperty deltaMzProperty() {
    return deltaMz;
  }

  public void setDeltaMz(double deltaMz) {
    this.deltaMz.set(deltaMz);
  }


  @Override
  public boolean equals(Object o) {
    // todo: write test to ensure stability if some records get refactored to classes
    if (this == o) {
      return true;
    }
    if (!(o instanceof PeakFormulaeModel model)) {
      return false;
    }

    if (getNode() != null ? !getNode().equals(model.getNode()) : model.getNode() != null) {
      return false;
    }
    if (!getPeakWithFormulae().equals(model.getPeakWithFormulae())) {
      return false;
    }
    return getSelectedFormulaWithMz().equals(model.getSelectedFormulaWithMz());
  }

  @Override
  public int hashCode() {
    int result = getNode() != null ? getNode().hashCode() : 0;
    result = 31 * result + getPeakWithFormulae().hashCode();
    result = 31 * result + getSelectedFormulaWithMz().hashCode();
    return result;
  }
}
