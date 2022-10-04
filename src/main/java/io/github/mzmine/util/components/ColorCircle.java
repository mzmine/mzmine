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

package io.github.mzmine.util.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * 
 */
public class ColorCircle extends JComponent {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static final int DEFAULT_MARGIN = 5;

  private Color circleColor;
  private int margin;

  /**
   */
  public ColorCircle(Color circleColor) {
    this(circleColor, DEFAULT_MARGIN);
  }

  /**
   */
  public ColorCircle(Color circleColor, int margin) {
    this.circleColor = circleColor;
    this.margin = margin;
  }

  public void paint(Graphics g) {
    super.paint(g);
    Dimension size = getSize();
    g.setColor(circleColor);
    int diameter = Math.min(size.width, size.height) - (2 * margin);
    g.fillOval((size.width - diameter) / 2, (size.height - diameter) / 2, diameter, diameter);
  }

}
