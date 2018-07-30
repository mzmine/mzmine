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

import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;

import java.util.Hashtable;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import net.sf.mzmine.util.components.ComponentToolTipManager;
import net.sf.mzmine.util.components.ComponentToolTipProvider;

public class ResultTable extends JTable implements ComponentToolTipProvider {
  private Hashtable<Long, Image> images;
  private ComponentToolTipManager ttm;
  private ResultTableModel listElementModel;


  public ResultTable(ResultTableModel model) {
    super(model);
    this.listElementModel = model;
    images = new Hashtable<>(10);

    ttm = new ComponentToolTipManager();
    ttm.registerComponent(this);

    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    getTableHeader().setReorderingAllowed(false);
    setDefaultRenderer(String[].class, new MultiLineTableCellRenderer());

     /* Sorter orders by FingerID score by default */
    ResultTableSorter sorter = new ResultTableSorter(listElementModel);
    setRowSorter(sorter);
  }

  @Override
  public String getToolTipText(MouseEvent e) {
    Point p = e.getPoint();
    int column = columnAtPoint(p);

    if (column == ResultTableModel.PREVIEW_INDEX) {
      return "PRINT_IMAGE";
    }

    return null;
  }

  public JComponent getCustomToolTipComponent(MouseEvent event) {
    JComponent component = null;
    String text = this.getToolTipText(event);

    if (text == null) {
      return null;
    }

    if (text.contains("PRINT_IMAGE")) {
      Point p = event.getPoint();
      int row = this.rowAtPoint(p);
      int realRow = this.convertRowIndexToModel(row);

      ResultTableModel model = (ResultTableModel)(this.getModel());
      SiriusCompound compound = model.getCompoundAt(realRow);

      Object shortcut = compound.getPreview();
      if (shortcut != null) {
        long hash = shortcut.hashCode();

        JLabel label = new JLabel();
        Image img = getIconImage(hash);
        if (img != null) {
          label.setIcon(new ImageIcon(img));
          JPanel panel = new JPanel();
          panel.setBackground(ComponentToolTipManager.bg);
          panel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
          panel.add(label);
          component = panel;
        } else {
          return null;
        }
      }
    }

    return component;
  }

  private @Nullable Image getIconImage(Long hash) {
    return images.get(hash);
  }

  public void generateIconImage(SiriusCompound compound) {
    Object preview = compound.getPreview();
    if (preview == null)
      return;

    long hash = compound.getPreview().hashCode();
    Image image = compound.generateStructureImage(600, 400);
    if (image != null)
      images.put(hash, image);
  }
}
