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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.ComponentCellRenderer;
import net.sf.mzmine.util.dialogs.PeriodicTableDialog;

import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;

public class ElementsTableComponent extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

    private JTable elementsTable;
    private JButton addElementButton, removeElementButton;
    private ElementsTableModel elementsTableModel;

    public ElementsTableComponent() {

	super(new BorderLayout());

	elementsTableModel = new ElementsTableModel();

	elementsTable = new JTable(elementsTableModel);
	elementsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	elementsTable.setRowSelectionAllowed(true);
	elementsTable.setColumnSelectionAllowed(false);
	elementsTable.setDefaultRenderer(Object.class,
		new ComponentCellRenderer(smallFont));
	elementsTable.getTableHeader().setReorderingAllowed(false);

	elementsTable.getTableHeader().setResizingAllowed(false);
	elementsTable
		.setPreferredScrollableViewportSize(new Dimension(200, 80));

	JScrollPane elementsScroll = new JScrollPane(elementsTable);
	add(elementsScroll, BorderLayout.CENTER);

	// Add buttons
	JPanel buttonsPanel = new JPanel();
	BoxLayout buttonsPanelLayout = new BoxLayout(buttonsPanel,
		BoxLayout.Y_AXIS);
	buttonsPanel.setLayout(buttonsPanelLayout);
	addElementButton = GUIUtils.addButton(buttonsPanel, "Add", null, this);
	removeElementButton = GUIUtils.addButton(buttonsPanel, "Remove", null,
		this);
	add(buttonsPanel, BorderLayout.EAST);

	this.setPreferredSize(new Dimension(300, 100));

    }

    public void actionPerformed(ActionEvent event) {

	Object src = event.getSource();

	if (src == addElementButton) {
	    JFrame parent = (JFrame) SwingUtilities.getAncestorOfClass(
		    JFrame.class, this);
	    PeriodicTableDialog dialog = new PeriodicTableDialog(parent);
	    dialog.setVisible(true);
	    IIsotope chosenIsotope = dialog.getSelectedIsotope();
	    if (chosenIsotope == null)
		return;
	    elementsTableModel.addRow(chosenIsotope, 0, 100);
	}

	if (src == removeElementButton) {
	    int selectedRow = elementsTable.getSelectedRow();
	    if (selectedRow < 0)
		return;
	    elementsTableModel.removeRow(selectedRow);
	}
    }

    public MolecularFormulaRange getElements() {

	MolecularFormulaRange newValue = new MolecularFormulaRange();

	for (int i = 0; i < elementsTableModel.getRowCount(); i++) {

	    IIsotope isotope = (IIsotope) elementsTableModel.getValueAt(i, 0);
	    int minCount = (Integer) elementsTableModel.getValueAt(i, 1);
	    int maxCount = (Integer) elementsTableModel.getValueAt(i, 2);

	    newValue.addIsotope(isotope, minCount, maxCount);
	}
	return newValue;
    }

    public void setElements(MolecularFormulaRange elements) {

	if (elements == null)
	    return;

	for (IIsotope isotope : elements.isotopes()) {
	    int minCount = elements.getIsotopeCountMin(isotope);
	    int maxCount = elements.getIsotopeCountMax(isotope);
	    elementsTableModel.addRow(isotope, minCount, maxCount);

	}
    }

}