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

package io.github.mzmine.modules.visualization.networking.visual;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class ConsensusEdge {

  // nodes might be redirected - does not have to be the same from bestEdge
  private final Node a;
  private final Node b;
  private final EdgeType type;
  private Double score;
  private String annotation;
  private Edge bestEdge;
  private int numberOfEdges;

  public ConsensusEdge(Node a, Node b, Edge edge) {
    this.bestEdge = edge;
    this.a = a;
    this.b = b;
    numberOfEdges = 1;
    type = edge.getAttribute(EdgeAtt.TYPE.toString(), EdgeType.class);
    annotation = edge.getAttribute(EdgeAtt.LABEL.toString(), String.class);
    score = getScore(edge);
  }

  public void add(Edge edge) {
    numberOfEdges++;
    double score = getScore(edge);
    if (score > this.score) {
      bestEdge = edge;
      annotation = bestEdge.getAttribute(EdgeAtt.LABEL.toString(), String.class);
      this.score = score;
    }
  }

  private double getScore(Edge edge) {
    try {
      return Double.parseDouble(edge.getAttribute(EdgeAtt.SCORE.toString()).toString());
    } catch (Exception ex) {
      return 0;
    }
  }

  public Node getA() {
    return a;
  }

  public Node getB() {
    return b;
  }

  public EdgeType getType() {
    return type;
  }

  public boolean matchingType(EdgeType type) {
    return type == this.type;
  }

  public Edge getBestEdge() {
    return bestEdge;
  }

  public String getAnnotation() {
    return annotation;
  }

  public Double getScore() {
    return score;
  }

  public int getNumberOfEdges() {
    return numberOfEdges;
  }

  public boolean matches(Node mnode, Node secondNode, Edge edge) {
    return a.equals(mnode) && b.equals(secondNode) && type.equals(edge.getAttribute(EdgeAtt.TYPE
        .toString()));
  }
}
