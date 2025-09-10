/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.javafx.validation;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import org.controlsfx.control.decoration.GraphicDecoration;

/**
 * Tooltip of control hides tooltip of error message this class fixes this
 */
public class TooltipFixGraphicDecoration extends GraphicDecoration {

  private Tooltip savedTooltip;

  /**
   * @param decorationNode the node used as decoration on a target
   */
  public TooltipFixGraphicDecoration(Node decorationNode) {
    super(decorationNode);
  }


  /**
   * @param decorationNode the node used as decoration on a target
   */
  public TooltipFixGraphicDecoration(Node decorationNode, Pos position) {
    super(decorationNode, position);
  }

  /**
   * @param decorationNode the node used as decoration on a target
   */

  public TooltipFixGraphicDecoration(Node decorationNode, Pos position, double xOffset,
      double yOffset) {
    super(decorationNode, position, xOffset, yOffset);
  }

  @Override
  public Node applyDecoration(Node targetNode) {
    if (targetNode instanceof Control control) {
      savedTooltip = control.getTooltip();
      control.setTooltip(null);
    }
    return super.applyDecoration(targetNode);
  }

  @Override
  public void removeDecoration(Node targetNode) {
    super.removeDecoration(targetNode);
    if (targetNode instanceof Control control && savedTooltip != null) {
      control.setTooltip(savedTooltip);
      savedTooltip = null;
    }
  }
}
