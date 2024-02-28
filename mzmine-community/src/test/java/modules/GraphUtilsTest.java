/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package modules;

import io.github.mzmine.util.GraphStreamUtils;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphUtilsTest {

  Graph graph;

  @BeforeEach
  public void init() {
    graph = new MultiGraph("Test-Graph");
    graph.setStrict(false);
    graph.setAutoCreate(true);
    graph.addEdge("AB", "A", "B");
    graph.addEdge("AC", "A", "C");
    graph.addEdge("CD", "C", "D");
    graph.addEdge("BD", "B", "D");
    graph.addEdge("BE", "B", "E");
    graph.addEdge("BF", "B", "F");
    graph.addEdge("BC", "B", "C");
    graph.addEdge("GF", "G", "F");
    graph.addEdge("HI", "H", "I");
    graph.addEdge("FE", "F", "E");
    graph.addEdge("FK", "F", "K");
    graph.addEdge("FI", "F", "I");
    graph.addEdge("KL", "K", "L");
    graph.addEdge("JL", "J", "L");
    graph.addEdge("IJ", "I", "J");
    graph.addEdge("FJ", "F", "J");
    graph.addEdge("CJ", "C", "J");
    graph.addEdge("KJ", "K", "J");
    for (Node node : graph) {
      node.setAttribute("ui.label", node.getId());
    }
  }

  @Test
  public void testMethod() {
    Assertions.assertEquals("[A, B, C, D, E, F, J]",
        GraphStreamUtils.getNodeNeighbors(graph, graph.getNode("A"), 2).toString());
    Assertions.assertEquals("[A, B, C]",
        GraphStreamUtils.getNodeNeighbors(graph, graph.getNode("A"), 1).toString());
    Assertions.assertEquals("[A]",
        GraphStreamUtils.getNodeNeighbors(graph, graph.getNode("A"), 0).toString());
  }
}
