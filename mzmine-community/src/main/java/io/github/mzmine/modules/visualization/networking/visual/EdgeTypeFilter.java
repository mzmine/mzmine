/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.modules.visualization.networking.visual.enums.EdgeAtt;
import java.util.List;
import java.util.Set;
import org.graphstream.graph.Edge;

public class EdgeTypeFilter implements EdgeFilter {

  private final Set<String> edgeTypes;

  public EdgeTypeFilter(final Set<String> edgeTypes) {
    this.edgeTypes = edgeTypes;
  }

  public EdgeTypeFilter(final List<? extends String> list) {
    edgeTypes = Set.of(list.toArray(String[]::new));
  }

  @Override
  public boolean accept(final Edge edge) {
    return edgeTypes.contains(edge.getAttribute(EdgeAtt.TYPE_STRING.toString()).toString());
  }
}
