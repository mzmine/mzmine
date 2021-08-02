/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
