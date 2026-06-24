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
 *  1. implement this
 *  2. make a list of all edges and a list of the possible edges in the graph
 *  3. update the list of all possible edges when the selected formula changes
 *  4. create table of all edges
 *  5. use only possible edges in the edge table
 *  6. option to show/hide edges from the edge table
 */
public class SubFormulaEdgeGenerator {

  private static final Logger logger = Logger.getLogger(SubFormulaEdgeGenerator.class.getName());

  private final List<SignalFormulaeModel> peaks;
  private final NumberFormat idFormatter;
  private List<SubFormulaEdge> edges = new ArrayList<>();

  public SubFormulaEdgeGenerator(List<SignalFormulaeModel> peaks, NumberFormat idFormatter) {
    // ensure the list is sorted by decreasing mz
    this.peaks = peaks.stream()
        .sorted(Comparator.comparingDouble(p -> p.getPeakWithFormulae().peak().getMZ() * -1))
        .toList();
    this.idFormatter = idFormatter;
    generateEdges();
  }

  private void generateEdges() {
    logger.finest(() -> "Generating edges for " + peaks.size() + " signals.");
    for (int i = 0; i < peaks.size() - 1; i++) {
      SignalFormulaeModel larger = peaks.get(i);
      // check all smaller formulae only
      for (int j = i + 1; j < peaks.size(); j++) {
        SignalFormulaeModel smaller = peaks.get(j);
//        if (!FormulaUtils.isSubFormula(smaller.getSelectedFormulaWithMz(), // generate edges only for sub formulae
//            larger.getSelectedFormulaWithMz())) {
//          continue;
//        }
        edges.add(new SubFormulaEdge(smaller, larger, idFormatter));
      }
    }
    logger.finest(
        () -> "Finished edge generation. Generated " + edges.size() + " edges for " + peaks.size()
            + " signals.");
  }

  public List<SubFormulaEdge> getEdges() {
    return edges;
  }
}
