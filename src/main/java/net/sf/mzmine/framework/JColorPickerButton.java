/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.framework;

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
import net.sf.mzmine.framework.listener.ColorChangedListener;

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
        dialog = JColorChooser.createDialog(parent, // parent comp
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
