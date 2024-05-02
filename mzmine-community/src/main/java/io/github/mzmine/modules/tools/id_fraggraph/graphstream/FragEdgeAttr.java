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

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.graphstream.graph.Edge;

/**
 * Edge attributes for a fragment graph. This class describes the available values and sets them
 * from a {@link SubFormulaEdge} to an {@link Edge}.
 */
public enum FragEdgeAttr {
  DELTA_MZ, DELTA_FORMULA;

  public void setToEdgeAttributes(SubFormulaEdge edge, Edge graphEdge) {
    final NumberFormats formats = ConfigService.getGuiFormats();
    graphEdge.setAttribute(name(), valueAsString(edge, formats));
  }

  public String valueAsString(SubFormulaEdge edge, NumberFormats formats) {
    return switch (this) {
      case DELTA_MZ -> formats.mz(edge.getDeltaMz());
      case DELTA_FORMULA -> edge.getLossFormulaAsString();
    };
  }

  public String valueAsString(SubFormulaEdge edge) {
    final NumberFormats formats = ConfigService.getGuiFormats();
    return valueAsString(edge, formats);
  }

  public static void applyAllAsLabel(SubFormulaEdge edge, Edge graphEdge) {
    final NumberFormats formats = ConfigService.getGuiFormats();
    final String label = Arrays.stream(FragEdgeAttr.values())
        .map(attr -> attr.valueAsString(edge, formats)).filter(str -> !str.isBlank())
        .collect(Collectors.joining("\n"));
    graphEdge.setAttribute("ui.label", label);
  }
}
