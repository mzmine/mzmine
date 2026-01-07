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

package io.github.mzmine.modules.io.import_feature_networks;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleExternalNetworkEdgeTest {

  @Test
  public void testParseEdge() {
    String line = "12,38,MS1 annotation,0.9521,[M+H]+ [M+Na]+ dm/z=21.9830";
    var edge = SimpleExternalNetworkEdge.parse(List.of(line.split(",")));
    Assertions.assertEquals(0.9521, edge.score());
    Assertions.assertEquals(12, edge.id1());
    Assertions.assertEquals(38, edge.id2());
    Assertions.assertEquals("[M+H]+ [M+Na]+ dm/z=21.9830", edge.annotation());
    Assertions.assertEquals("MS1 annotation", edge.edgeType());

    line = "12,38,MS1 annotation,0.9521, ";
    edge = SimpleExternalNetworkEdge.parse(List.of(line.split(",")));
    Assertions.assertEquals(0.9521, edge.score());
    Assertions.assertEquals(12, edge.id1());
    Assertions.assertEquals(38, edge.id2());
    Assertions.assertEquals(" ", edge.annotation());
    Assertions.assertEquals("MS1 annotation", edge.edgeType());
  }

  @Test
  public void testParseEdgeException() {
    Assertions.assertThrows(RuntimeException.class, () -> SimpleExternalNetworkEdge.parse(
        List.of("12,,MS1 annotation,0.9521,[M+H]+ [M+Na]+ dm/z=21.9830".split(","))));

    Assertions.assertThrows(RuntimeException.class, () -> SimpleExternalNetworkEdge.parse(
        List.of("12,11,MS1 annotation,0.9521test,[M+H]+ [M+Na]+ dm/z=21.9830".split(","))));

    Assertions.assertThrows(IllegalArgumentException.class, () -> SimpleExternalNetworkEdge.parse(
        List.of(
            "12,11,MS1 annotation,0.9521test,[M+H]+ [M+Na]+ dm/z=21.9830, too much".split(","))));
  }

}