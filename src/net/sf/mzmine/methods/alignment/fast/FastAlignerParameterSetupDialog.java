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

package net.sf.mzmine.methods.alignment.fast;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;

import java.text.NumberFormat;

import java.util.Vector;

/**
 * Customized parameter setup dialog for join aligner
 */
public class FastAlignerParameterSetupDialog extends JDialog implements ActionListener {

	// Array for Text fields
	private JFormattedTextField txtMZvsRTBalance;
	private JFormattedTextField txtMZTolerance;
	private JComboBox cmbRTToleranceType;
	private JFormattedTextField txtRTToleranceAbsValue;
	private JFormattedTextField txtRTTolerancePercent;

	// Number formatting used in text fields
	private NumberFormat decimalNumberFormatOther;
	private NumberFormat decimalNumberFormatMZ;
	private NumberFormat percentFormat;

	// Options available in cmbRTToleranceType
	private Vector<String> optionsIncmbRTToleranceType;

	// Labels
	private JLabel lblMZvsRTBalance;
	private JLabel lblMZTolerance;
	private JLabel lblRTToleranceType;
	private JLabel lblRTToleranceAbsValue;
	private JLabel lblRTTolerancePercent;

	// Buttons
	private JButton btnOK;
	private JButton btnCancel;

	// Panels for all above
	private JPanel pnlAll;
	private JPanel pnlLabels;
	private JPanel pnlFields;
	private JPanel pnlButtons;

	// Parameter values
	FastAlignerParameters params;

	// Exit code for controlling ok/cancel response
	private int exitCode = -1;


	/**
	 * Constructor
	 */
	public FastAlignerParameterSetupDialog(JFrame owner, String title, FastAlignerParameters _params) {
		super(owner, title, true);

		params = _params;
		exitCode = -1;

		// Panel where everything is collected
		pnlAll = new JPanel(new BorderLayout());
		pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(pnlAll);

		// Two more panels: one for labels and another for text fields
		pnlLabels = new JPanel(new GridLayout(0,1));
		pnlFields = new JPanel(new GridLayout(0,1));

		// Setup number formats for text fields
		decimalNumberFormatMZ = NumberFormat.getNumberInstance();
		decimalNumberFormatMZ.setMinimumFractionDigits(3);
		decimalNumberFormatOther = NumberFormat.getNumberInstance();
		decimalNumberFormatOther.setMinimumFractionDigits(1);
		percentFormat = NumberFormat.getPercentInstance();

		// Create fields
		txtMZvsRTBalance = new JFormattedTextField(decimalNumberFormatOther);
		txtMZvsRTBalance.setColumns(8);
		txtMZvsRTBalance.setValue(params.paramMZvsRTBalance);
		pnlFields.add(txtMZvsRTBalance);

		txtMZTolerance = new JFormattedTextField(decimalNumberFormatMZ);
		txtMZTolerance.setColumns(8);
		txtMZTolerance.setValue(params.paramMZTolerance);
		pnlFields.add(txtMZTolerance);

		optionsIncmbRTToleranceType = new Vector<String>();
		optionsIncmbRTToleranceType.add(new String("Absolute (seconds)"));
		optionsIncmbRTToleranceType.add(new String("Percent of RT"));
		cmbRTToleranceType = new JComboBox(optionsIncmbRTToleranceType);
		cmbRTToleranceType.addActionListener(this);
		pnlFields.add(cmbRTToleranceType);

		txtRTToleranceAbsValue = new JFormattedTextField(decimalNumberFormatOther);
		txtRTToleranceAbsValue.setColumns(8);
		txtRTToleranceAbsValue.setValue(params.paramRTToleranceAbs);
		pnlFields.add(txtRTToleranceAbsValue);

		txtRTTolerancePercent = new JFormattedTextField(percentFormat);
		txtRTTolerancePercent.setColumns(8);
		txtRTTolerancePercent.setValue(params.paramRTTolerancePercent);
		pnlFields.add(txtRTTolerancePercent);



		// Create labels
		lblMZvsRTBalance = new JLabel("Balance between M/Z and RT");
		lblMZvsRTBalance.setLabelFor(txtMZvsRTBalance);
		pnlLabels.add(lblMZvsRTBalance);

		lblMZTolerance = new JLabel("M/Z tolerance size");
		lblMZTolerance.setLabelFor(txtMZTolerance);
		pnlLabels.add(lblMZTolerance);

		lblRTToleranceType = new JLabel("RT tolerance type");
		lblRTToleranceType.setLabelFor(cmbRTToleranceType);
		pnlLabels.add(lblRTToleranceType);

		lblRTToleranceAbsValue = new JLabel("RT tolerance size (absolute)");
		lblRTToleranceAbsValue.setLabelFor(txtRTToleranceAbsValue);
		pnlLabels.add(lblRTToleranceAbsValue);

		lblRTTolerancePercent = new JLabel("RT tolerance size (percent)");
		lblRTTolerancePercent.setLabelFor(txtRTTolerancePercent);
		pnlLabels.add(lblRTTolerancePercent);


		if (params.paramRTToleranceUseAbs) {
			cmbRTToleranceType.setSelectedIndex(0);
		} else {
			cmbRTToleranceType.setSelectedIndex(1);
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

		pack();

		setLocationRelativeTo(owner);

	}

	/**
	 * Implementation for ActionListener interface
	 */
	public void actionPerformed(java.awt.event.ActionEvent ae) {
		Object src = ae.getSource();
		if (src==btnOK) {

			// Copy values back to parameters object
			params.paramMZvsRTBalance = ((Number)(txtMZvsRTBalance.getValue())).doubleValue();
			params.paramMZTolerance = ((Number)(txtMZTolerance.getValue())).doubleValue();
			params.paramRTToleranceAbs = ((Number)(txtRTToleranceAbsValue.getValue())).doubleValue();
			params.paramRTTolerancePercent = ((Number)(txtRTTolerancePercent.getValue())).doubleValue();
			int ind = cmbRTToleranceType.getSelectedIndex();
			if (ind==0) {
				params.paramRTToleranceUseAbs = true;
			} else {
				params.paramRTToleranceUseAbs = false;
			}

			// Set exit code and fade away
			exitCode = 1;
			setVisible(false);
		}

		if (src==btnCancel) {
			exitCode = -1;
			setVisible(false);
		}

		if (src==cmbRTToleranceType) {
			int ind = cmbRTToleranceType.getSelectedIndex();
			if (ind==0) {
				// "Absolute" selected
				txtRTToleranceAbsValue.setEnabled(true);
				lblRTToleranceAbsValue.setEnabled(true);

				txtRTTolerancePercent.setEnabled(false);
				lblRTTolerancePercent.setEnabled(false);
			}

			if (ind==1) {
				// "Percent" selected
				txtRTToleranceAbsValue.setEnabled(false);
				lblRTToleranceAbsValue.setEnabled(false);

				txtRTTolerancePercent.setEnabled(true);
				lblRTTolerancePercent.setEnabled(true);
			}

		}

	}

	/**
	 * Method for reading contents of a field
	 * @param	fieldNum	Number of field
	 * @return	Value of the field
	 */
	public FastAlignerParameters getParameters() {
		return params;
	}

	/**
	 * Method for reading exit code
	 * @return	1=OK clicked, -1=cancel clicked
	 */
	public int getExitCode() {
		return exitCode;
	}

}
