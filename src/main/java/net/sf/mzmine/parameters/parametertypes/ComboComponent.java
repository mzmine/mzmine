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
package net.sf.mzmine.parameters.parametertypes;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class ComboComponent<ValueType> extends JPanel {

  private static final long serialVersionUID = 1L;

  private final JComboBox<ValueType> comboBox;

  public ComboComponent(ValueType choices[]) {
    setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
    comboBox = new JComboBox<ValueType>(choices);
    add(comboBox);
  }

  @Override
  public void setToolTipText(String toolTip) {
    comboBox.setToolTipText(toolTip);
  }

  public Object getSelectedItem() {
    return comboBox.getSelectedItem();
  }

  public int getSelectedIndex() {
    return comboBox.getSelectedIndex();
  }

  public void setSelectedItem(ValueType newValue) {
    comboBox.setSelectedItem(newValue);
  }

  public void setModel(ComboBoxModel<ValueType> model) {
    comboBox.setModel(model);
  }
}
