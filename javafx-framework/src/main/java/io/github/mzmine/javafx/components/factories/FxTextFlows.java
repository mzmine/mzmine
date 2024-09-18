/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.javafx.components.factories;

import io.github.mzmine.javafx.components.util.FxLayout;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class FxTextFlows {

  /**
   * text flow
   *
   * @param nodes typically {@link Text} or {@link Hyperlink}
   */
  public static TextFlow newTextFlow(Node... nodes) {
    return newTextFlow(TextAlignment.LEFT, nodes);
  }

  /**
   * text flow
   *
   * @param nodes typically {@link Text} or {@link Hyperlink}
   */
  public static TextFlow newTextFlow(final TextAlignment textAlignment, Node... nodes) {
    var textFlow = new TextFlow(nodes);
    textFlow.setTextAlignment(textAlignment);
    textFlow.setPrefHeight(Region.USE_COMPUTED_SIZE);
    textFlow.setMaxWidth(Double.MAX_VALUE);
    textFlow.setMaxHeight(Double.MAX_VALUE);
    return textFlow;
  }

  public static Region newTextFlowInAccordion(final String title, Node... nodes) {
    return newTextFlowInAccordion(title, false, TextAlignment.LEFT, nodes);
  }

  public static Region newTextFlowInAccordion(final String title, boolean expanded, Node... nodes) {
    return newTextFlowInAccordion(title, expanded, TextAlignment.LEFT, nodes);
  }

  public static Region newTextFlowInAccordion(final String title, boolean expanded,
      final TextAlignment textAlignment, Node... nodes) {
    TextFlow textFlow = newTextFlow(nodes);
    textFlow.setPadding(FxLayout.DEFAULT_PADDING_INSETS);
    textFlow.setTextAlignment(textAlignment);
    final TitledPane pane = new TitledPane(title, textFlow);
    final Accordion accordion = new Accordion(pane);
    if (expanded) {
      accordion.setExpandedPane(accordion.getPanes().getFirst());
    }
    return accordion;
  }
}
