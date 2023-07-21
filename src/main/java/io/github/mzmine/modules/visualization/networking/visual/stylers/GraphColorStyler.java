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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphElementAttr;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphObject;
import io.github.mzmine.util.GraphStreamUtils;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Logger;
import javafx.scene.paint.Color;
import org.graphstream.graph.Graph;
import org.jetbrains.annotations.NotNull;

public final class GraphColorStyler extends AbstractGraphStyler {

  private static final Logger logger = Logger.getLogger(GraphColorStyler.class.getName());

  public GraphColorStyler(final GraphObject go) {
    super(go);
  }

  @Override
  public void applyStyle(Graph graph, GraphElementAttr attribute,
      Function<GraphElementAttr, Range<Float>> valueRangeFunction,
      @NotNull Function<GraphElementAttr, Map<String, Integer>> valuesMapFunction) {
    logger.info(go + " colors");
    var valueRange = valueRangeFunction.apply(attribute);
    var valuesMap = valuesMapFunction.apply(attribute);
    int distinctValues = valuesMap.size();

    SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette();

    if (valueRange != null || (distinctValues == 0 || distinctValues > colors.size())) {
      applyByRange(graph, attribute, valueRange);
    } else {
      applyByMap(graph, attribute, valuesMap, colors);
    }
  }

  private void applyByMap(final Graph graph, final GraphElementAttr attribute,
      final Map<String, Integer> valuesMap, final SimpleColorPalette colors) {
    AtomicInteger failed = new AtomicInteger(0);
    go.stream(graph).forEach(element -> {
//      element.removeAttribute("ui.class");
      String value = GraphStreamUtils.getStringOrElse(element, attribute, "").strip().toLowerCase();
      Integer index = valuesMap.get(value);
      if (index != null) {
        element.setAttribute("ui.color", ColorsFX.toHexString(colors.get(index)));
      } else {
        failed.incrementAndGet();
        element.setAttribute("ui.color", ColorsFX.toHexString(Color.RED));
      }
    });
    if (failed.get() > 0) {
      logger.fine(failed.get()
                  + " nodes failed to calculate color. This might be normal if those colors did not hold this attribute: "
                  + attribute);
    }
  }

  private void applyByRange(final Graph graph, final GraphElementAttr attribute,
      final Range<Float> valueRange) {
    AtomicInteger failed = new AtomicInteger(0);
    go.stream(graph).forEach(element -> {

//      var type = GraphStreamUtils.getElementType(element();

      Optional<Float> value = GraphStreamUtils.getFloatValue(element, attribute)
          .map(v -> interpolate(valueRange, v)).map(v -> attribute.isReversed() ? 1 - v : v);
      if (value.isPresent()) {
//        element.setAttribute("ui.class", "GRADIENT");
        element.setAttribute("ui.color", value.get());
      } else {
//        element.removeAttribute("ui.class");
        failed.incrementAndGet();
      }
    });
    if (failed.get() > 0) {
      logger.fine(failed.get()
                  + " nodes failed to calculate color. This might be normal if those colors did not hold this attribute: "
                  + attribute);
    }
  }


}
