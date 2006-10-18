/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.userinterface.dialogs;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.Frame;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.methods.MethodParameters;



/**
 * This class represents the parameter setup dialog shown to the user before processing
 */
public class ParameterSetupDialog extends JDialog implements ActionListener {

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
	public ParameterSetupDialog(Frame owner, String title, MethodParameters methodParameters) {

		// Make dialog modal
		super(owner, true);
		
		System.err.println("Initializing parameter setup dialog");
		
		exitCode = -1;

		Parameter[] parameters = methodParameters.getParameters();
		if ( (parameters==null) || (parameters.length==0) ) {
			System.err.println("Parameters is null or length 0.");
			System.err.println("parameters.length = " + parameters.length);
			exitCode = 1;
			dispose();		
		}
		
		// Allocate arrays
		textFields = new JFormattedTextField[methodParameters.getParameters().length];
		labels = new JLabel[methodParameters.getParameters().length];

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
		for (int i=0; i<methodParameters.getParameters().length; i++) {
			Parameter p = methodParameters.getParameters()[i];
			
			//textFields[i] = new JFormattedTextField(decimalNumberFormat);
			textFields[i] = new JFormattedTextField(p.getFormat());
			textFields[i].setValue(p.getDefaultValue());
			textFields[i].setColumns(8);
			textFields[i].setToolTipText(p.getDescription());

			labels[i] = new JLabel(p.getName());
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

		pack();

		setLocationRelativeTo(owner);
	}

	/**
	 * Implementation for ActionListener interface
	 */
	public void actionPerformed(java.awt.event.ActionEvent ae) {
		Object src = ae.getSource();
		if (src==btnOK) {
			exitCode = 1;
			dispose();
			//setVisible(false);
		}
		if (src==btnCancel) {
			exitCode = -1;
			dispose();
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
