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

package io.github.mzmine.modules.visualization.networking.visual.stylers;

import com.google.common.collect.Range;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphElementAttr;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphObject;
import io.github.mzmine.util.GraphStreamUtils;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graphstream.graph.Graph;
import org.jetbrains.annotations.NotNull;

public final class GraphLabelStyler extends AbstractGraphStyler {

  private static final Logger logger = Logger.getLogger(GraphLabelStyler.class.getName());

  public GraphLabelStyler(final GraphObject go) {
    super(go);
  }

  @Override
  public void applyStyle(Graph graph, GraphElementAttr attribute,
      Function<GraphElementAttr, Range<Float>> valueRangeFunction,
      @NotNull Function<GraphElementAttr, Map<String, Integer>> valuesMapFunction) {
    logger.info(go + " labels");
    go.stream(graph).forEach(element -> {
      try {
        element.setAttribute("ui.label", GraphStreamUtils.getStringOrElse(element, attribute, ""));
      } catch (Exception ex) {
        logger.log(Level.SEVERE, "Error while setting label attribute. " + ex.getMessage(), ex);
      }
    });
  }

}
