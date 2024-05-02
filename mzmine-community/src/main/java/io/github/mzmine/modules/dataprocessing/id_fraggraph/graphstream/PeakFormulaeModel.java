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

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_fraggraph.PeakWithFormulae;
import io.github.mzmine.util.FormulaWithExactMz;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.graphstream.graph.Node;
import org.jetbrains.annotations.Nullable;

/**
 * Maps the results of the formula prediction to a node in a fragment graph. Contains all available
 * formulae and the selected formulae and its mz delta. Also contains a reference to the node to
 * update the node in the gui if the formula changes.
 * <p>
 * TODO: add a reference to the filtered node.
 */
public class PeakFormulaeModel {

  private static final Logger logger = Logger.getLogger(PeakFormulaeModel.class.getName());

  private final Node unfilteredNode;
  private Node filteredNode = null;
  private final PeakWithFormulae peakWithFormulae;
  private final ObjectProperty<FormulaWithExactMz> selectedFormulaWithMz = new SimpleObjectProperty<>();
  private final DoubleProperty deltaMz = new SimpleDoubleProperty(0);

  public PeakFormulaeModel(Node unfilteredNode, PeakWithFormulae formulae) {
    this.unfilteredNode = unfilteredNode;
    this.peakWithFormulae = formulae;
    final NumberFormats formats = ConfigService.getGuiFormats();

    deltaMz.bind(Bindings.createDoubleBinding(
        () -> selectedFormulaWithMz.get() != null ? formulae.peak().getMZ()
            - selectedFormulaWithMz.get().mz() : 0d, selectedFormulaWithMz));

    selectedFormulaWithMz.addListener((_, _, n) -> {
      for (FragNodeAttr value : FragNodeAttr.values()) {
        value.setToNode(unfilteredNode, formulae);
        value.setToNode(unfilteredNode, n);
      }

      unfilteredNode.setAttribute("ui.label", STR."""
          \{unfilteredNode.getAttribute(FragNodeAttr.MZ.name())}
          \{unfilteredNode.getAttribute(FragNodeAttr.FORMULA.name())}
          \{formats.ppm(deltaMz.get())} ppm
          """);

      // todo: can we even trigger edge updates from here?
    });
    selectedFormulaWithMz.set(formulae.formulae().getFirst());
  }

  public Node getUnfilteredNode() {
    return unfilteredNode;
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
    return STR."PeakFormulaeModel{node=\{unfilteredNode}, selectedFormulaWithMz=\{selectedFormulaWithMz}, deltaMz=\{deltaMz}\{'}'}";
  }

  public void setFilteredNode(@Nullable Node node) {
    if(this.filteredNode != null) {
      logger.warning(() -> STR."Warning, node for \{toString()} has already been set. Resetting.");
    }
    filteredNode = node;
  }

  @Nullable
  public Node getFilteredNode() {
    return filteredNode;
  }

  public String getId() {
    return getUnfilteredNode().getId();
  }
}
