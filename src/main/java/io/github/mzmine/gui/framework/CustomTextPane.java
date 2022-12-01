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

package io.github.mzmine.gui.framework;

import java.awt.Container;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

public class CustomTextPane extends JTextPane {
  private boolean lineWrap;

  public CustomTextPane(final boolean lineWrap) {
    this.lineWrap = lineWrap;

    if (lineWrap)
      setEditorKit(new WrapEditorKit());
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    if (lineWrap)
      return super.getScrollableTracksViewportWidth();
    else {
      Container parent = SwingUtilities.getUnwrappedParent(this);
      return parent == null || getUI().getPreferredSize(this).width <= parent.getSize().width;
    }
  }

  private class WrapEditorKit extends StyledEditorKit {
    private final ViewFactory defaultFactory = new WrapColumnFactory();

    @Override
    public ViewFactory getViewFactory() {
      return defaultFactory;
    }
  }

  private class WrapColumnFactory implements ViewFactory {
    @Override
    public View create(final Element element) {
      final String kind = element.getName();
      if (kind != null) {
        switch (kind) {
          case AbstractDocument.ContentElementName:
            return new WrapLabelView(element);
          case AbstractDocument.ParagraphElementName:
            return new ParagraphView(element);
          case AbstractDocument.SectionElementName:
            return new BoxView(element, View.Y_AXIS);
          case StyleConstants.ComponentElementName:
            return new ComponentView(element);
          case StyleConstants.IconElementName:
            return new IconView(element);
        }
      }

      // Default to text display.
      return new LabelView(element);
    }
  }

  private class WrapLabelView extends LabelView {
    public WrapLabelView(final Element element) {
      super(element);
    }

    @Override
    public float getMinimumSpan(final int axis) {
      switch (axis) {
        case View.X_AXIS:
          return 0;
        case View.Y_AXIS:
          return super.getMinimumSpan(axis);
        default:
          throw new IllegalArgumentException("Invalid axis: " + axis);
      }
    }
  }
}
