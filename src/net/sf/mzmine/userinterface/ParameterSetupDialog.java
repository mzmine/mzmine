/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package net.sf.mzmine.userinterface;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This class represents the parameter setup dialog shown to the user before processing
 */
public class ParameterSetupDialog extends ModalJInternalFrame implements ActionListener {

	// Array for Text fields
	private JFormattedTextField[] textFields;

	// Number formatting used in text fields
	private NumberFormat decimalNumberFormat;

	// Labels
	private JLabel[] labels;

	// Buttons
	private JButton btnOK;
	private JButton btnCancel;

	// Panels for all above
	private JPanel pnlAll;
	private JPanel pnlLabels;
	private JPanel pnlFields;
	private JPanel pnlButtons;

	// Exit code for controlling ok/cancel response
	private int exitCode = -1;


	/**
	 * Constructor
	 */
	public ParameterSetupDialog(MainWindow _mainWin, String title, String[] labelNames, double[] defaultValues, NumberFormat[] numberFormats) {
		//super(_mainWin, title, true);
		exitCode = -1;

		// Allocate arrays
		textFields = new JFormattedTextField[labelNames.length];
		labels = new JLabel[labelNames.length];

		// Panel where everything is collected
		pnlAll = new JPanel(new BorderLayout());
		pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(pnlAll);

		// Two more panels: one for labels and another for text fields
		pnlLabels = new JPanel(new GridLayout(0,1));
		pnlFields = new JPanel(new GridLayout(0,1));

		// Setup number format for text fields
		decimalNumberFormat = NumberFormat.getNumberInstance();
		decimalNumberFormat.setMinimumFractionDigits(1);

		// Create fields and labels
		for (int i=0; i<labelNames.length; i++) {
			//textFields[i] = new JFormattedTextField(decimalNumberFormat);
			textFields[i] = new JFormattedTextField(numberFormats[i]);
			textFields[i].setValue(new Double(defaultValues[i]));
			textFields[i].setColumns(8);

			labels[i] = new JLabel(labelNames[i]);
			labels[i].setLabelFor(textFields[i]);

			pnlLabels.add(labels[i]);
			pnlFields.add(textFields[i]);
		}

		// Buttons
		pnlButtons = new JPanel();
		btnOK = new JButton("OK");
		btnOK.addActionListener(this);
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);
		pnlButtons.add(btnOK);
		pnlButtons.add(btnCancel);

		pnlAll.add(pnlLabels,BorderLayout.CENTER);
		pnlAll.add(pnlFields,BorderLayout.LINE_END);
		pnlAll.add(pnlButtons,BorderLayout.SOUTH);

		getContentPane().add(pnlAll);

		setTitle(title);

		//setLocationRelativeTo(_mainWin);

		pack();
	}

	/**
	 * Implementation for ActionListener interface
	 */
	public void actionPerformed(java.awt.event.ActionEvent ae) {
		Object src = ae.getSource();
		if (src==btnOK) {
			exitCode = 1;
			disposeModal();
			//setVisible(false);
		}
		if (src==btnCancel) {
			exitCode = -1;
			disposeModal();
			//setVisible(false);
		}
	}

	/**
	 * Method for reading contents of a field
	 * @param	fieldNum	Number of field
	 * @return	Value of the field
	 */
	public double getFieldValue(int fieldNum) {
		return ((Number)textFields[fieldNum].getValue()).doubleValue();
	}

	/**
	 * Method for reading exit code
	 * @return	1=OK clicked, -1=cancel clicked
	 */
	public int getExitCode() {
		return exitCode;
	}

}
