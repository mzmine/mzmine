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

package net.sf.mzmine.util.interpolatinglookuppaintscale;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.util.ExitCode;

public class InterpolatingLookupPaintScaleSetupDialog extends JDialog implements
	ActionListener, ListSelectionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final int VALUEFIELD_COLUMNS = 4;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private JFormattedTextField fieldValue;

    private JScrollPane scrollpaneLookupValues;
    private JTable tableLookupValues;
    private InterpolatingLookupPaintScaleSetupDialogTableModel tableModel;

    private TreeMap<Double, Color> lookupTable = new TreeMap<Double, Color>();

    private JButton buttonAddModify;
    private JButton buttonDelete;
    private JButton buttonColor;
    private JButton buttonOK;
    private JButton buttonCancel;

    private ExitCode exitCode = ExitCode.CANCEL;

    public InterpolatingLookupPaintScaleSetupDialog(JFrame parent,
	    InterpolatingLookupPaintScale paintScale) {
	super(parent, "Select colors for paint scale", true);

	Double[] lookupValues = paintScale.getLookupValues();
	for (Double lookupValue : lookupValues) {
	    Color color = (Color) paintScale.getPaint(lookupValue);
	    lookupTable.put(lookupValue, color);
	}

	// Panel for controls and list
	JPanel panelControlsAndList = new JPanel(new BorderLayout());
	add(panelControlsAndList, BorderLayout.CENTER);

	// Sub-panel for controls
	JPanel panelControls = new JPanel(new BorderLayout());
	panelControlsAndList.add(panelControls, BorderLayout.NORTH);

	// Sub-sub panel for value and color chooser controls
	JPanel panelValueAndColor = new JPanel(new FlowLayout(FlowLayout.LEFT));
	panelControls.add(panelValueAndColor, BorderLayout.NORTH);

	JLabel labelValue = new JLabel("Value: ");
	panelValueAndColor.add(labelValue);
	fieldValue = new JFormattedTextField(NumberFormat.getNumberInstance());
	fieldValue.setColumns(VALUEFIELD_COLUMNS);
	panelValueAndColor.add(fieldValue);

	buttonColor = new JButton("Select color");
	buttonColor.setBackground(new Color(255, 255, 255));
	buttonColor.addActionListener(this);
	panelValueAndColor.add(buttonColor);

	// JPanel panelAddRemoveButtons = new JPanel(new
	// FlowLayout(FlowLayout.LEFT));
	buttonAddModify = new JButton("Add/Modify");
	buttonAddModify.addActionListener(this);
	buttonDelete = new JButton("Delete");
	buttonDelete.addActionListener(this);
	panelValueAndColor.add(new JPanel());
	panelValueAndColor.add(buttonAddModify);
	panelValueAndColor.add(buttonDelete);

	// Sub-panel for scrollpane & list
	JPanel panelList = new JPanel();

	tableModel = new InterpolatingLookupPaintScaleSetupDialogTableModel(
		lookupTable);
	tableLookupValues = new JTable(tableModel);
	tableLookupValues
		.getColumnModel()
		.getColumn(1)
		.setCellRenderer(
			new InterpolatingLookupPaintScaleSetupDialogTableCellRenderer(
				lookupTable));
	tableLookupValues.getSelectionModel().addListSelectionListener(this);

	panelControlsAndList.add(panelList, BorderLayout.CENTER);
	scrollpaneLookupValues = new JScrollPane(tableLookupValues);
	panelList.add(scrollpaneLookupValues);

	JPanel panelOKCancelButtons = new JPanel(new FlowLayout(
		FlowLayout.RIGHT));
	buttonOK = new JButton("OK");
	buttonOK.addActionListener(this);
	panelOKCancelButtons.add(buttonOK);
	buttonCancel = new JButton("Cancel");
	buttonCancel.addActionListener(this);
	panelOKCancelButtons.add(buttonCancel);

	panelControlsAndList.add(panelOKCancelButtons, BorderLayout.SOUTH);

	pack();
	setLocationRelativeTo(parent);
	setResizable(false);

    }

    public void valueChanged(ListSelectionEvent event) {

	if (event.getValueIsAdjusting())
	    return;

	ListSelectionModel lsm = (ListSelectionModel) event.getSource();

	int index = lsm.getLeadSelectionIndex();
	if (index < 0) {
	    return;
	}
	Double value = lookupTable.keySet().toArray(new Double[0])[index];
	Color color = lookupTable.get(value);
	fieldValue.setValue(value);
	buttonColor.setBackground(color);

    }

    public void actionPerformed(ActionEvent event) {
	Object src = event.getSource();
	if (src == buttonColor) {
	    Color newColor = JColorChooser.showDialog(this,
		    "Please select color", buttonColor.getBackground());
	    buttonColor.setBackground(newColor);
	}

	if (src == buttonAddModify) {
	    if (fieldValue.getValue() == null) {
		displayMessage("Please enter value first.");
		return;
	    }

	    Double d = ((Number) fieldValue.getValue()).doubleValue();
	    lookupTable.put(d, buttonColor.getBackground());
	    tableModel.fireTableDataChanged();
	    scrollpaneLookupValues.repaint();
	}

	if (src == buttonDelete) {
	    int[] selectedRows = tableLookupValues.getSelectedRows();
	    for (int rowIndex : selectedRows) {
		Double value = lookupTable.keySet().toArray(new Double[0])[rowIndex];
		lookupTable.remove(value);
	    }
	    tableModel.fireTableDataChanged();
	    scrollpaneLookupValues.repaint();
	}

	if (src == buttonOK) {

	    exitCode = ExitCode.OK;
	    dispose();
	}

	if (src == buttonCancel) {
	    exitCode = ExitCode.CANCEL;
	    dispose();
	}
    }

    public ExitCode getExitCode() {
	return exitCode;
    }

    public InterpolatingLookupPaintScale getPaintScale() {
	InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();
	for (Double value : lookupTable.keySet()) {
	    paintScale.add(value, lookupTable.get(value));
	}
	return paintScale;
    }

    private void displayMessage(String msg) {
	try {
	    logger.info(msg);
	    JOptionPane.showMessageDialog(this, msg, "Error",
		    JOptionPane.ERROR_MESSAGE);
	} catch (Exception exce) {
	}
    }

}
