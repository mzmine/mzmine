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

import io.github.mzmine.util.FormulaUtils;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Generates all possible sub formula edges for a fragmentation graph.
 * <p>
 * TODO: refine edges to clean up the graph and reflect deleted edges.
 * Todo: maybe it's better to create an edge from every node to every node and only set the edges
 *  that are sub formulae to visible?
 */
public class SubFormulaEdgeGenerator {

  private static final Logger logger = Logger.getLogger(SubFormulaEdgeGenerator.class.getName());

  private final List<PeakFormulaeModel> peaks;
  private final NumberFormat idFormatter;
  private List<SubFormulaEdge> edges = new ArrayList<>();

  public SubFormulaEdgeGenerator(List<PeakFormulaeModel> peaks, NumberFormat idFormatter) {
    // ensure the list is sorted by decreasing mz
    this.peaks = peaks.stream()
        .sorted(Comparator.comparingDouble(p -> p.getPeakWithFormulae().peak().getMZ() * -1))
        .toList();
    this.idFormatter = idFormatter;
    generateEdges();
  }

  private void generateEdges() {
    logger.finest(() -> STR."Generating edges for \{peaks.size()} signals.");
    for (int i = 0; i < peaks.size() - 1; i++) {
      PeakFormulaeModel larger = peaks.get(i);
      // check all smaller formulae only
      for (int j = i + 1; j < peaks.size(); j++) {
        PeakFormulaeModel smaller = peaks.get(j);
        if (!FormulaUtils.isSubFormula(smaller.getSelectedFormulaWithMz(),
            larger.getSelectedFormulaWithMz())) {
          continue;
        }
        edges.add(new SubFormulaEdge(smaller, larger, idFormatter));
      }
    }
    logger.finest(
        () -> STR."Finished edge generation. Generated \{edges.size()} edges for \{peaks.size()} signals.");
  }

  public List<SubFormulaEdge> getEdges() {
    return edges;
  }
}
