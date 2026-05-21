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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class NetworkStyleSheetTest {

  @Test
  void parsesNamedColor() {
    final List<Color> colors = NetworkStyleSheet.parseColorList("salmon");
    assertEquals(1, colors.size());
    assertEquals(Color.SALMON, colors.getFirst());
  }

  @Test
  void parsesHexColor() {
    final List<Color> colors = NetworkStyleSheet.parseColorList("#156CA2");
    assertEquals(1, colors.size());
    assertEquals(Color.web("#156CA2"), colors.getFirst());
  }

  @Test
  void parsesRgbFunctionWithoutTearingCommas() {
    // rgb(108, 108, 108) has internal commas; the comma-split must NOT tear them apart
    final List<Color> colors = NetworkStyleSheet.parseColorList("rgb(108, 108, 108)");
    assertEquals(1, colors.size());
    assertEquals(Color.rgb(108, 108, 108), colors.getFirst());
  }

  @Test
  void parsesMixedGradient() {
    // mirrors the node.FEATURE rule: hex + named colors interleaved
    final List<Color> colors = NetworkStyleSheet.parseColorList(
        "#ffffe6, yellow, orange, #c10000, #570025, black");
    assertEquals(6, colors.size());
    assertEquals(Color.web("#ffffe6"), colors.get(0));
    assertEquals(Color.YELLOW, colors.get(1));
    assertEquals(Color.ORANGE, colors.get(2));
    assertEquals(Color.web("#c10000"), colors.get(3));
    assertEquals(Color.web("#570025"), colors.get(4));
    assertEquals(Color.BLACK, colors.get(5));
  }

  @Test
  void readsRealStylesheet() {
    // loads themes/graph_network_style.css from resources and exercises the lookup API
    final NetworkStyleSheet style = NetworkStyleSheet.loadDefault();
    // node.ANALOG { fill-color: salmon; size: 15px; }
    final List<Color> analog = style.getNodeFillColors("ANALOG");
    assertEquals(1, analog.size());
    assertEquals(Color.SALMON, analog.getFirst());
    // edge.IIN { fill-color: rgb(227, 116, 30); stroke-mode: dashes; }
    final List<Color> iin = style.getEdgeFillColors("IIN");
    assertEquals(1, iin.size());
    assertEquals(Color.rgb(227, 116, 30), iin.getFirst());
    assertEquals("dashes", style.getEdgeStrokeMode("IIN"));
    // edge.COSINE has a 4-color dyn-plain gradient
    final List<Color> cosine = style.getEdgeFillColors("COSINE");
    assertEquals(4, cosine.size(), "COSINE should expose its full color gradient");
    // unknown class falls back to the bare node { ... } rule
    final List<Color> fallback = style.getNodeFillColors("DEFINITELY_NOT_A_CLASS");
    assertNotNull(fallback);
    assertTrue(!fallback.isEmpty(), "fallback to bare 'node' rule should yield the default color");
  }
}
