/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.sirius.table;

import java.awt.Component;

import java.util.Hashtable;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Renderer for PreviewCell compounds
 */
public class PreviewCellRenderer implements TableCellRenderer {
  // Store created PreviewCells and reuse later
  private Hashtable<IAtomContainer, PreviewCell> components;

  public PreviewCellRenderer() {
    components = new Hashtable<>(10);
  }

  /**
   * Render PreviewCell component that contains image
   * @param table
   * @param value
   * @param isSelected
   * @param hasFocus
   * @param row
   * @param column
   * @return
   */
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {
    if (value == null)
      return null;

    if (!components.contains(value)) {
      IAtomContainer container = (IAtomContainer) value;
      components.put(container, new PreviewCell(container));
    }
    return components.get(value);
  }
}
