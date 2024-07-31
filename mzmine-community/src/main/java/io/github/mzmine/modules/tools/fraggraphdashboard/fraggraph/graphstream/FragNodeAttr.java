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
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.SignalWithFormulae;
import io.github.mzmine.util.FormulaWithExactMz;
import org.graphstream.graph.Node;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Node attributes for a fragment graph. This class describes the available values and sets them
 * from a {@link SignalWithFormulae} and a {@link FormulaWithExactMz} to a {@link Node}.
 */
public enum FragNodeAttr {
  MZ, INTENSITY, FORMULA, DELTA_MZ_ABS, DELTA_MZ_PPM;

  public void setToNode(Node node, SignalFormulaeModel sfm) {
    final NumberFormats formats = ConfigService.getGuiFormats();
    switch (this) {
      case MZ -> node.setAttribute(name(), formats.mz(sfm.getCalculatedMz()));
      case INTENSITY -> node.setAttribute(name(),
          formats.intensity(sfm.getPeakWithFormulae().peak().getIntensity()));
      case FORMULA -> node.setAttribute(name(),
          MolecularFormulaManipulator.getString(sfm.getSelectedFormulaWithMz().formula()));
      case DELTA_MZ_ABS -> node.setAttribute(name(), formats.mz(sfm.getDeltaMzAbs()));
      case DELTA_MZ_PPM -> node.setAttribute(name(), formats.mz(sfm.getDeltaMzPpm()));
    }
  }
}
