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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JComboBox;

public class JFontBox extends JComboBox<String> {

  public JFontBox() {
    super(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());

    setSelectedItem("Arial");
    setPreferredSize(new Dimension(120, 22));
    setMaximumSize(getPreferredSize());
    setMaximumRowCount(20);
    setRenderer(new FontBoxRenderer(this));
    addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          final String fontName = getSelectedItem().toString();
          setFont(new Font(fontName, Font.PLAIN, 14));
        }
      }
    });
  }



}
