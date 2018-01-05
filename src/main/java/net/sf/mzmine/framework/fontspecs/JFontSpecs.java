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

package net.sf.mzmine.framework.fontspecs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.event.ItemListener;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.sf.mzmine.framework.JColorPickerButton;
import net.sf.mzmine.framework.listener.ColorChangedListener;
import net.sf.mzmine.framework.listener.DelayedDocumentListener;

public class JFontSpecs extends JPanel {

  private JFontBox fontBox;
  private JFontStyleBox styleBox;
  private JTextField txtSize;
  private JColorPickerButton color;
  private DelayedDocumentListener dl;

  public JFontSpecs() {
    super();
    fontBox = new JFontBox();
    add(fontBox);

    styleBox = new JFontStyleBox();
    add(styleBox);


    txtSize = new JTextField();
    txtSize.setHorizontalAlignment(SwingConstants.RIGHT);
    txtSize.setText("14");
    add(txtSize);
    txtSize.setColumns(3);

    color = new JColorPickerButton(this);
    add(color);
    color.setColor(Color.WHITE);

    // setPreferredSize(new Dimension(280, 30));
    this.validate();
    setMaximumSize(getPreferredSize());
  }


  public void setSelectedFont(Font font) {
    fontBox.setSelectedItem(font.getName());
    styleBox.setSelectedIndex(font.getStyle());
    txtSize.setText(String.valueOf(font.getSize()));
  }

  /**
   * FontSpecs (Font and Color)
   * 
   * @return
   */
  public FontSpecs getFontSpecs() {
    return new FontSpecs(getColor(), getFont());
  }

  public void setFontSpecs(FontSpecs f) {
    setColor(f.getColor());
    setFont(f.getFont());
  }

  @Override
  public void setFont(Font font) {
    setSelectedFont(font);
  }

  public Font getSelectedFont() {
    return new Font(getFontFamily(), getFontStyle(), getFontSize());
  }

  @Override
  public Font getFont() {
    return getSelectedFont();
  }

  public Color getColor() {
    return color.getColor();
  }

  public void setColor(Color c) {
    color.setColor(c);
  }

  public void setColor(Paint c) {
    color.setColor((Color) c);
  }

  public void setFontSize(int size) {
    txtSize.setText(String.valueOf(size));
  }

  /**
   * The font size or 1 if there is an error.
   * 
   * @return
   */
  public int getFontSize() {
    if (txtSize == null || txtSize.getText().length() == 0)
      return 1;
    try {
      return Integer.parseInt(txtSize.getText());
    } catch (Exception e) {
      e.printStackTrace();
      return 1;
    }
  }

  /**
   * Style: plain, italic, bold, ...
   * 
   * @return
   */
  public int getFontStyle() {
    if (styleBox == null)
      return Font.PLAIN;
    return styleBox.getSelectedStyle();
  }

  /**
   * Family such as arial ...
   * 
   * @return
   */
  public String getFontFamily() {
    if (fontBox == null)
      return "Arial";
    return String.valueOf(fontBox.getSelectedItem());
  }

  public void addListener(ColorChangedListener ccl, ItemListener il, DocumentListener dl) {
    fontBox.addItemListener(il);
    styleBox.addItemListener(il);
    txtSize.getDocument().addDocumentListener(dl);
    color.addColorChangedListener(ccl);
    if (dl instanceof DelayedDocumentListener)
      this.dl = (DelayedDocumentListener) dl;
  }


  public void addListener(final Consumer<FontSpecs> f) {
    ColorChangedListener ccl = e -> f.accept(getFontSpecs());
    ItemListener il = e -> f.accept(getFontSpecs());
    dl = new DelayedDocumentListener() {
      @Override
      public void documentChanged(DocumentEvent e) {
        f.accept(getFontSpecs());
      }
    };

    fontBox.addItemListener(il);
    styleBox.addItemListener(il);
    txtSize.getDocument().addDocumentListener(dl);
    color.addColorChangedListener(ccl);
  }


  public JTextField getTxtSize() {
    return txtSize;
  }


  public void stopListener() {
    if (dl != null)
      dl.stop();
  }

}
