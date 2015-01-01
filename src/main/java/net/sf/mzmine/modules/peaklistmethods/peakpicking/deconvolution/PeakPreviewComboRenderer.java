/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.components.PeakXICComponent;

public class PeakPreviewComboRenderer extends JPanel implements
	ListCellRenderer<PeakListRow> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public Component getListCellRendererComponent(
	    JList<? extends PeakListRow> combo, PeakListRow value, int index,
	    boolean isSelected, boolean cellHasFocus) {

	PeakListRow row = (PeakListRow) value;
	if (row == null)
	    return new JPanel();
	Feature peak = row.getPeaks()[0];

	String labelText = "#"
		+ row.getID()
		+ " "
		+ MZmineCore.getConfiguration().getMZFormat()
			.format(row.getAverageMZ()) + " m/z ";
	JLabel textComponent = new JLabel(labelText);
	textComponent.setFont(combo.getFont());

	PeakXICComponent shapeComponent = new PeakXICComponent(peak);
	shapeComponent.setBorder(null);

	JPanel panel = new JPanel(new BorderLayout());

	panel.setOpaque(true);
	if (isSelected) {
	    panel.setBackground(combo.getSelectionBackground());
	    panel.setForeground(combo.getSelectionForeground());
	} else {
	    panel.setBackground(combo.getBackground());
	    panel.setForeground(combo.getForeground());
	}

	panel.setEnabled(combo.isEnabled());

	Border border = null;
	if (cellHasFocus) {
	    if (isSelected) {
		border = UIManager
			.getBorder("List.focusSelectedCellHighlightBorder");
	    }
	    if (border == null) {
		border = UIManager.getBorder("List.focusCellHighlightBorder");
	    }
	} else {
	    border = noFocusBorder;
	}
	panel.setBorder(border);

	panel.add(textComponent, BorderLayout.WEST);
	panel.add(shapeComponent, BorderLayout.CENTER);

	return panel;
    }

}
