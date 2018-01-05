/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redist
te it and/or modify it under the terms of the GNU
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

import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class FontBoxRenderer extends BasicComboBoxRenderer {

  private static final long serialVersionUID = 1L;
  private JComboBox comboBox;
  final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
  private int row;

  public FontBoxRenderer(JComboBox fontsBox) {
    comboBox = fontsBox;
  }

  private void manItemInCombo() {
    if (comboBox.getItemCount() > 0) {
      final Object comp = comboBox.getUI().getAccessibleChild(comboBox, 0);
      if ((comp instanceof JPopupMenu)) {
        final JList list = new JList(comboBox.getModel());
        final JPopupMenu popup = (JPopupMenu) comp;
        final JScrollPane scrollPane = (JScrollPane) popup.getComponent(0);
        final JViewport viewport = scrollPane.getViewport();
        final Rectangle rect = popup.getVisibleRect();
        final Point pt = viewport.getViewPosition();
        row = list.locationToIndex(pt);
      }
    }
  }

  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index,
      boolean isSelected, boolean cellHasFocus) {
    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    if (list.getModel().getSize() > 0) {
      manItemInCombo();
    }
    final JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, row,
        isSelected, cellHasFocus);
    final Object fntObj = value;
    final String fontFamilyName = (String) fntObj;
    setFont(new Font(fontFamilyName, Font.PLAIN, 16));
    return this;
  }
}
