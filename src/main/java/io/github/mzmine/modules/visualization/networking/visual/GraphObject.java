/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.visualization.networking.visual;

import java.util.stream.Stream;
import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;

/**
 * Defines access to different objects in the graph
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum GraphObject {
  NODE, EDGE;

  /**
   * Stream all elements in graph of this type
   *
   * @param graph the target graph
   * @return a stream of nodes or edges
   */
  public Stream<? extends Element> stream(Graph graph) {
    return switch (this) {
      case NODE -> graph.nodes();
      case EDGE -> graph.edges();
    };
  }
}
