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

// ColorPicker.java
// A quick test of the JColorChooser dialog.
//
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import io.github.mzmine.gui.framework.listener.ColorChangedListener;

public class JColorPickerButton extends JButton {
  private static final long serialVersionUID = 1L;
  //
  private JDialog dialog;
  private JColorChooser chooser = new JColorChooser();
  private Component parent;
  protected ArrayList<ColorChangedListener> colorChangedListener;

  public JColorPickerButton(Component parent) {
    this(parent, null);
  }

  public JColorPickerButton(Component parent, Color color) {
    super();
    this.parent = parent;
    setPreferredSize(new Dimension(25, 25));
    this.addActionListener(al -> showDialog());
    this.setColor(color);
  }

  @Override
  public void paint(Graphics g) {
    // super.paint(g);
    g.setColor(getBackground());
    g.fillRect(0, 0, getWidth(), getHeight());

    g.setColor(Color.BLACK);
    g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
  }

  /**
   * Changes the color and calls all listeners
   *
   * @param color
   */
  public void colorChanged(Color color) {
    this.setBackground(color);
    if (colorChangedListener != null)
      for (ColorChangedListener listener : colorChangedListener) {
        listener.colorChanged(color);
      }
  }

  /**
   * Show color chooser dialog
   */
  public void showDialog() {
    chooser.setColor(this.getBackground());
    //
    // New Dialog
    try {
      ActionListener okListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          Color color = chooser.getColor();
          colorChanged(color);
        }
      };
      ActionListener cancelListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {}
      };

      if (dialog == null)
        dialog = JColorChooser.createDialog(null, // parent comp
            "Pick A Color", // dialog title
            false, // modality
            chooser, okListener, cancelListener);

      dialog.setVisible(true);
      //
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    if (dialog != null) {
      dialog.setVisible(true);
    }
  }

  public void addColorChangedListener(ColorChangedListener listener) {
    if (colorChangedListener == null)
      colorChangedListener = new ArrayList<ColorChangedListener>();
    this.colorChangedListener.add(listener);
  }

  public List<ColorChangedListener> getColorChangedListeners() {
    return colorChangedListener;
  }

  /**
   * The selected color (which is also the background color)
   *
   * @return
   */
  public Color getColor() {
    return getBackground();
  }

  /**
   * Same as color changed
   *
   * @param c
   */
  public void setColor(Color c) {
    colorChanged(c);
  }

  /**
   * same as color changed
   *
   * @param c
   */
  public void setColor(Paint c) {
    setColor((Color) c);
  }
}
