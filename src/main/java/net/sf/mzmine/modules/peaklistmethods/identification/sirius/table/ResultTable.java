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

import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.table.ResultTableModel.PREVIEW_INDEX;

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

/**
 * Class ResultTable for SingleIdentificationTask results
 * Implements ComponentToolTipProvider in order to provide specific tooltip on a cells.
 *  Images of chemical structures
 */
public class ResultTable extends JTable implements ComponentToolTipProvider {
  private final Hashtable<Long, Image> images;
  private final ComponentToolTipManager ttm;
  private final ResultTableModel listElementModel;

  /**
   * Constructor for ResultTable class.
   * Configures internal parameters
   * 1) Registers in ComponentToolTipManager.
   * 2) Initializes hashtable
   *
   * @param model
   */
  public ResultTable(ResultTableModel model) {
    super(model);
    this.listElementModel = model;
    images = new Hashtable<>(10);

    // Register in ComponentToolTipManager to receive updates
    ttm = new ComponentToolTipManager();
    ttm.registerComponent(this);

    // Configure table
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    getTableHeader().setReorderingAllowed(false);
    // Special configuration of multiline cell
    setDefaultRenderer(String[].class, new MultiLineTableCellRenderer());

     /* Sorter orders by FingerID score by default */
    ResultTableSorter sorter = new ResultTableSorter(listElementModel);
    setRowSorter(sorter);

    /* Configure rows & columns sizes */
    getColumnModel().getColumn(PREVIEW_INDEX).setWidth(SiriusCompound.PREVIEW_WIDTH);
    setRowHeight(SiriusCompound.PREVIEW_HEIGHT);
  }

  @Override
  public String getToolTipText(MouseEvent e) {
    Point p = e.getPoint();
    int column = columnAtPoint(p);

    // Show tooltip iff mouse points on an image preview cell
    if (column == PREVIEW_INDEX) {
      return "PRINT_IMAGE";
    }

    return null;
  }

  /**
   * Provided by interface `ComponentToolTipProvider`
   * Allows to set more complex tooltips for components
   * In this case full size image is shown (chemical structure) of a compound
   * @param event
   * @return constructed complex tooltip
   */
  public JComponent getCustomToolTipComponent(MouseEvent event) {
    JComponent component = null;
    String text = this.getToolTipText(event);

    if (text == null) {
      return null;
    }

    // Flag of tooltip form preview_image cell
    if (text.contains("PRINT_IMAGE")) {
      Point p = event.getPoint();
      int row = this.rowAtPoint(p);
      int realRow = this.convertRowIndexToModel(row);

      ResultTableModel model = (ResultTableModel)(this.getModel());
      SiriusCompound compound = model.getCompoundAt(realRow);

      // Obtain a preview from the compound, if it is null -> it is not processed by fingerId
      Object shortcut = compound.getPreview();
      if (shortcut != null) {
        // Using hash of the image, retrieve a large image
        long hash = shortcut.hashCode();
        Image img = getIconImage(hash);

        // Construct new tooltip
        JLabel label = new JLabel();
        if (img != null) {
          label.setIcon(new ImageIcon(img)); // set image
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

  /**
   * Identify Image by its' preview hash code
   * @param hash code of the preview
   * @return null, if there is no such image and large version otherwise.
   */
  private @Nullable Image getIconImage(Long hash) {
    return images.get(hash);
  }

  /**
   * Generate large image and store <hash of the preview, Image> pairs in the dictionary.
   * @param compound
   */
  public void generateIconImage(SiriusCompound compound) {
    Object preview = compound.getPreview();

    // If there is no preview - no large image should be generated
    if (preview == null)
      return;

    long hash = compound.getPreview().hashCode();
    Image image = compound.generateStructureImage(SiriusCompound.STRUCTURE_WIDTH, SiriusCompound.STRUCTURE_HEIGHT);
    if (image != null)
      images.put(hash, image);
  }
}
